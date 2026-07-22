package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.Comment;
import com.emall.backend.entity.Order;
import com.emall.backend.entity.OrderItem;
import com.emall.backend.entity.User;
import com.emall.backend.mapper.CommentMapper;
import com.emall.backend.mapper.OrderItemMapper;
import com.emall.backend.mapper.OrderMapper;
import com.emall.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.emall.backend.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private OrderMapper orderMapper; // 用于更新订单评价状态
    // ✨ 1. 在类顶部注入商品 Mapper
    @Autowired
    private com.emall.backend.mapper.ProductMapper productMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AuthorizationService authorizationService;

    // ✨ 2. 修改获取全站评价列表的方法
    @GetMapping("/list")
    public List<Comment> listAll() {
        // 先查出所有的评价基础记录
        List<Comment> list = commentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Comment>()
                        .orderByDesc("create_time")
        );

        // 核心同步逻辑：遍历每一条评价，根据 productId 去查商品表拿名字
        for (Comment comment : list) {
            if (comment.getProductId() != null) {
                com.emall.backend.entity.Product product = productMapper.selectById(comment.getProductId());
                if (product != null) {
                    // 填入到那个 @TableField(exist = false) 的非数据库字段中
                    comment.setProductName(product.getName());
                }
            }
        }

        return list;
    }
    // 1. 发布评价并锁定订单状态
    @PostMapping("/add")
    @Transactional
    public String add(@RequestBody Comment comment, Authentication authentication) {
        validateCommentBody(comment);
        if (comment.getOrderId() == null || comment.getProductId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order and product are required");
        }
        Order ownedOrder = orderMapper.selectById(comment.getOrderId());
        if (ownedOrder == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        authorizationService.requireOwner(authentication, ownedOrder.getUserId());

        OrderItem orderItem = orderItemMapper.selectOne(new QueryWrapper<OrderItem>()
                .eq("order_id", ownedOrder.getId())
                .eq("product_id", comment.getProductId())
                .last("LIMIT 1"));
        if (orderItem == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The product does not belong to this order");
        }
        if (orderMapper.markCommentedIfEligible(ownedOrder.getId()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only completed, unreviewed orders can be reviewed");
        }

        comment.setUserId(ownedOrder.getUserId());
        User author = userMapper.selectById(ownedOrder.getUserId());
        if (author == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review author does not exist");
        }
        comment.setNickname(author.getNickname() == null || author.getNickname().isBlank()
                ? author.getUsername() : author.getNickname());
        comment.setAvatar(author.getAvatar());
        comment.setContent(comment.getContent().trim());
        // 1. 保存评价（评价表里记录了 order_id）
        commentMapper.insert(comment);

        return "success";
    }

    // 2. ✨ 修复 404：获取当前用户的评价列表
    @GetMapping("/my")
    public List<Comment> getMyComments(@RequestParam Long userId, Authentication authentication) {
        userId = authorizationService.requireSelfOrAdmin(authentication, userId);
        return commentMapper.selectMyComments(userId); // ✨ 调用联表查询
    }

    // 3. ✨ 获取评价详情（用于修改评价时的回显）
    @GetMapping("/detail/{id}")
    public Comment getDetail(@PathVariable Long id, Authentication authentication) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评价不存在");
        authorizationService.requireOwnerOrAdmin(authentication, comment.getUserId());
        return comment;
    }
    // 在 CommentController.java 中追加此方法
    @DeleteMapping("/{id}")
    @Transactional
    public String deleteComment(@PathVariable Long id, Authentication authentication) {
        Comment existing = commentMapper.selectById(id);
        if (existing == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评价不存在");
        authorizationService.requireOwnerOrAdmin(authentication, existing.getUserId());
        // 调用 MyBatis-Plus 提供的 deleteById 直接删除数据库中的评价记录
        commentMapper.deleteById(id);
        Long remaining = commentMapper.selectCount(
                new QueryWrapper<Comment>().eq("order_id", existing.getOrderId()));
        if (remaining == 0) {
            orderMapper.resetCommentStatus(existing.getOrderId());
        }
        return "评价删除成功";
    }
    @PostMapping("/update")
    public String update(@RequestBody Comment comment, Authentication authentication) {
        validateCommentBody(comment);
        Comment existing = commentMapper.selectById(comment.getId());
        if (existing == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评价不存在");
        authorizationService.requireOwner(authentication, existing.getUserId());
        comment.setUserId(existing.getUserId());
        comment.setOrderId(existing.getOrderId());
        comment.setProductId(existing.getProductId());
        comment.setNickname(existing.getNickname());
        comment.setAvatar(existing.getAvatar());
        comment.setCreateTime(existing.getCreateTime());
        comment.setContent(comment.getContent().trim());

        // 执行更新
        commentMapper.updateById(comment);
        return "success";
    }
    // 5. 获取商品评价列表 (已有接口)
    @GetMapping("/list/{productId}")
    public List<Comment> list(@PathVariable Long productId) {
        return commentMapper.selectList(new QueryWrapper<Comment>()
                .eq("product_id", productId)
                .orderByDesc("create_time"));
    }

    private void validateCommentBody(Comment comment) {
        if (comment == null || comment.getStar() == null || comment.getStar() < 1 || comment.getStar() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }
        if (comment.getContent() == null || comment.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review content is required");
        }
    }

}
