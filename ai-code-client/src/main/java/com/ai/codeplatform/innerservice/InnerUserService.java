package com.ai.codeplatform.innerservice;

import com.ai.codeplatform.exception.BusinessException;
import com.ai.codeplatform.exception.ErrorCode;
import com.ai.codeplatform.ai.model.entity.User;
import com.ai.codeplatform.ai.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import static com.ai.codeplatform.constant.UserConstant.USER_LOGIN_STATE;
@FeignClient(value = "ai-code-user" ,path = "/api/inner/user")
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

    @GetMapping("/listByIds")
    List<User> listByIds(@RequestParam("ids") Collection<Long> ids);

    @GetMapping("/{id}")
    User getById(@PathVariable("id") Long id);

    @PostMapping("/vo")
    UserVO getUserVO(@RequestBody User user);

    @PostMapping("/quota/deduct")
    boolean deductQuota(@RequestParam("userId") Long userId);

    @GetMapping("/count")
    Long countTotalUsers();
}
