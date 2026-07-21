package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.Coupon;
import com.emall.backend.entity.UserCoupon;
import com.emall.backend.mapper.CouponMapper;
import com.emall.backend.mapper.UserCouponMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.emall.backend.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;
    @Autowired
    private AuthorizationService authorizationService;

    // 1. 获取所有可领取的优惠券 (用于首页/详情页领券中心展示)
    @GetMapping("/list")
    public List<Coupon> getCouponList() {
        return couponMapper.selectList(new QueryWrapper<Coupon>().orderByDesc("create_time"));
    }

    // 2. 用户领券接口 (支持复购领券逻辑)
    @PostMapping("/claim")
    public String claimCoupon(@RequestParam Long userId, @RequestParam Long couponId, Authentication authentication) {
        userId = authorizationService.requireSelfOrAdmin(authentication, userId);
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "优惠券不存在");
        if (coupon.getEndTime() == null || java.time.LocalDateTime.now().isAfter(coupon.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "优惠券已过期");
        }
        // ✨ 核心升级：只检查用户包里是否还有【未使用】的这张券
        UserCoupon existUnused = userCouponMapper.selectOne(
                new QueryWrapper<UserCoupon>()
                        .eq("user_id", userId)
                        .eq("coupon_id", couponId)
                        .eq("status", 0) // 0 代表未使用
                        .last("LIMIT 1") // 加上 limit 1 防止多条未使用的报错
        );

        if (existUnused != null) {
            return "您已经有一张该神券还没使用哦，请先去消费吧！";
        }

        // 如果没有未使用的（即没领过，或者以前领的都用掉了），就发一张新的
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0); // 0代表未使用
        userCouponMapper.insert(userCoupon);
        return "领取成功！";
    }

    // 3. ✨ 获取用户当前【未使用】的优惠券及详情 (用于结算页面满减计算)
    @GetMapping("/myUsable")
    public List<Map<String, Object>> getMyUsableCoupons(@RequestParam Long userId, Authentication authentication) {
        userId = authorizationService.requireSelfOrAdmin(authentication, userId);
        List<Map<String, Object>> result = new ArrayList<>();

        // 查出该用户所有未使用的领券记录
        List<UserCoupon> myCoupons = userCouponMapper.selectList(
                new QueryWrapper<UserCoupon>().eq("user_id", userId).eq("status", 0)
        );

        // 拼接优惠券的具体金额和门槛信息返回给前端
        for (UserCoupon uc : myCoupons) {
            Coupon couponInfo = couponMapper.selectById(uc.getCouponId());
            if (couponInfo != null && couponInfo.getEndTime() != null
                    && !java.time.LocalDateTime.now().isAfter(couponInfo.getEndTime())) {
                Map<String, Object> map = new HashMap<>();
                map.put("userCouponId", uc.getId());
                map.put("couponId", couponInfo.getId());
                map.put("name", couponInfo.getName());
                map.put("minAmount", couponInfo.getMinAmount());
                map.put("discountAmount", couponInfo.getDiscountAmount());
                map.put("endTime", couponInfo.getEndTime());
                result.add(map);
            }
        }
        return result;
    }
}
