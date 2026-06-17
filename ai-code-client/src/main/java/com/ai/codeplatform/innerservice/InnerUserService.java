package com.ai.codeplatform.innerservice;

import com.ai.codeplatform.exception.BusinessException;
import com.ai.codeplatform.exception.ErrorCode;
import com.ai.codeplatform.ai.model.entity.User;
import com.ai.codeplatform.ai.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import static com.ai.codeplatform.constant.UserConstant.USER_LOGIN_STATE;

public interface InnerUserService {

    // 获取当前登录用户
    // 静态方法，避免跨服务调用
    static User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    // 根据id列表查询
    List<User> listByIds(Collection<? extends Serializable> ids);

    // 根据id查询
    User getById(Serializable id);

    // 获取当前登录用户VO
    UserVO getUserVO(User user);
    // 扣减用户额度
    boolean deductQuota(Long userId, int currentQuota);

    // 统计用户总数
    Long countTotalUsers();
}
