package com.emall.backend.controller;

// ✨ 核心 Spring Boot 注解
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

// ✨ Java 基础库
import java.util.List;

// ✨ 你的业务实体和 Mapper (请根据你的实际包名确认，通常如下)
import com.emall.backend.entity.Address;
import com.emall.backend.mapper.AddressMapper;
import com.emall.backend.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// ✨ MyBatis-Plus 的查询构造器
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

@RestController
@RequestMapping("/api/address")
public class AddressController {
    @Autowired private AddressMapper addressMapper;
    @Autowired private AuthorizationService authorizationService;

    @GetMapping("/list")
    public List<Address> list(@RequestParam Long userId, Authentication authentication) {
        userId = authorizationService.requireSelfOrAdmin(authentication, userId);
        return addressMapper.selectList(new QueryWrapper<Address>()
                .eq("user_id", userId).orderByDesc("is_default"));
    }

    @PostMapping("/add")
    public String add(@RequestBody Address address, Authentication authentication) {
        address.setUserId(authorizationService.requireSelfOrAdmin(authentication, address.getUserId()));
        addressMapper.insert(address);
        return "添加成功";
    }

    @PutMapping("/update")
    public String update(@RequestBody Address address, Authentication authentication) {
        Address existing = addressMapper.selectById(address.getId());
        if (existing == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "地址不存在");
        authorizationService.requireOwnerOrAdmin(authentication, existing.getUserId());
        address.setUserId(existing.getUserId());
        addressMapper.updateById(address);
        return "修改成功";
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Authentication authentication) {
        Address existing = addressMapper.selectById(id);
        if (existing == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "地址不存在");
        authorizationService.requireOwnerOrAdmin(authentication, existing.getUserId());
        addressMapper.deleteById(id);
        return "删除成功";
    }

    @PutMapping("/setDefault/{userId}/{id}")
    @Transactional
    public String setDefault(@PathVariable Long userId, @PathVariable Long id, Authentication authentication) {
        userId = authorizationService.requireSelfOrAdmin(authentication, userId);
        Address existing = addressMapper.selectById(id);
        if (existing == null || !java.util.Objects.equals(existing.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "地址不存在");
        }
        // 先将该用户所有地址设为非默认
        Address updateAll = new Address();
        updateAll.setIsDefault(0);
        addressMapper.update(updateAll, new QueryWrapper<Address>().eq("user_id", userId));
        // 再设置当前地址为默认
        Address address = new Address();
        address.setId(id);
        address.setIsDefault(1);
        addressMapper.updateById(address);
        return "设置成功";
    }
}
