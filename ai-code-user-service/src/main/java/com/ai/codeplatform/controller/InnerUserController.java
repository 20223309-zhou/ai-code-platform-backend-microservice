package com.ai.codeplatform.controller;

import com.ai.codeplatform.ai.model.entity.User;
import com.ai.codeplatform.ai.model.vo.UserVO;
import com.ai.codeplatform.annotation.InnerApi;
import com.ai.codeplatform.mapper.UserMapper;
import com.ai.codeplatform.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@InnerApi
@RestController
@RequestMapping("/inner/user")
public class InnerUserController {

    @Resource
    private UserService userService;
    @Resource
    private UserMapper userMapper;

    @GetMapping("/listByIds")
    public List<User> listByIds(@RequestParam Collection<Long> ids) {
        return userService.listByIds(ids);
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping("/vo")
    public UserVO getUserVO(@RequestBody User user) {
        return userService.getUserVO(user);
    }

    @PostMapping("/quota/deduct")
    public boolean deductQuota(@RequestParam("userId") Long userId) {
        // 单条原子 SQL：quota = quota - 1 由数据库计算，条件用字面量 0。
        // 并发请求会在该行的排他锁上排队等待，锁释放后重新判断条件，不会失败，故无需应用层重试。
        return userService.updateChain()
                .setRaw(User::getQuota, "quota - 1")
                .set(User::getUpdateTime, java.time.LocalDateTime.now())
                .where(User::getId).eq(userId)
                .and(User::getQuota).gt(0)
                .update();
    }

    @GetMapping("/count")
    public Long countTotalUsers() {
        return userMapper.selectCountByQuery(QueryWrapper.create());
    }
}
