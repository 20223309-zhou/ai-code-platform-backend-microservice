package com.ai.codeplatform.service.impl;

import com.ai.codeplatform.ai.model.entity.User;
import com.ai.codeplatform.ai.model.vo.UserVO;
import com.ai.codeplatform.innerservice.InnerUserService;
import com.ai.codeplatform.mapper.UserMapper;
import com.ai.codeplatform.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@DubboService
public class InnerUserService1Impl implements InnerUserService {

    @Resource
    private UserService userService;
    @Resource
    private UserMapper userMapper;
    @Override
    public List<User> listByIds(Collection<? extends Serializable> ids) {
        return userService.listByIds(ids);
    }

    @Override
    public User getById(Serializable id) {
        return userService.getById(id);
    }

    @Override
    public UserVO getUserVO(User user) {
        return userService.getUserVO(user);
    }

    @Override
    public boolean deductQuota(Long userId) {
        // 单条原子 SQL：quota = quota - 1 由数据库计算，条件用字面量 0。
        // 并发请求会在该行的排他锁上排队等待，锁释放后重新判断条件，不会失败，故无需应用层重试。
        return userService.updateChain()
                .setRaw(User::getQuota, "quota - 1")
                .set(User::getUpdateTime, java.time.LocalDateTime.now())
                .where(User::getId).eq(userId)
                .and(User::getQuota).gt(0)
                .update();
    }

    @Override
    public Long countTotalUsers() {
        return userMapper.selectCountByQuery(QueryWrapper.create());
    }
}
