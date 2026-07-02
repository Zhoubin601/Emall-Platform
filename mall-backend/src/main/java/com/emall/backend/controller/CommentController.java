package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.Comment;
import com.emall.backend.entity.Order;
import com.emall.backend.mapper.CommentMapper;
import com.emall.backend.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    public String add(@RequestBody Comment comment) {
        // 1. 保存评价（评价表里记录了 order_id）
        commentMapper.insert(comment);

        // 2. ✨ 精准更新：只把“这一个”订单改为已评价
        Order order = new Order();
        order.setId(comment.getOrderId()); // 这里的 ID 是唯一的订单主键
        order.setCommentStatus(1);
        orderMapper.updateById(order);

        return "success";
    }

    // 2. ✨ 修复 404：获取当前用户的评价列表
    @GetMapping("/my")
    public List<Comment> getMyComments(@RequestParam Long userId) {
        return commentMapper.selectMyComments(userId); // ✨ 调用联表查询
    }

    // 3. ✨ 获取评价详情（用于修改评价时的回显）
    @GetMapping("/detail/{id}")
    public Comment getDetail(@PathVariable Long id) {
        return commentMapper.selectById(id);
    }
    // 在 CommentController.java 中追加此方法
    @DeleteMapping("/{id}")
    public String deleteComment(@PathVariable Long id) {
        // 调用 MyBatis-Plus 提供的 deleteById 直接删除数据库中的评价记录
        commentMapper.deleteById(id);
        return "评价删除成功";
    }
    @PostMapping("/update")
    public String update(@RequestBody Comment comment) {
        // ✨ 核心修复：手动将时间设置为“现在”
        // 这样每次点击“保存修改”，时间都会跳到当前最新的系统时间
        comment.setCreateTime(java.time.LocalDateTime.now());

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

}