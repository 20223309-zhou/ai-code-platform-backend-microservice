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
    public boolean deductQuota(Long userId, int currentQuota) {
        return userService.updateChain()
                .set(User::getQuota, currentQuota - 1)
                .set(User::getUpdateTime, java.time.LocalDateTime.now())
                .where(User::getId).eq(userId)
                .and(User::getQuota).eq(currentQuota)
                .update();
    }

    @Override
    public Long countTotalUsers() {
        return userMapper.selectCountByQuery(QueryWrapper.create());
    }
}
