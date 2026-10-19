package com.ai.codeplatform.service;

import com.ai.codeplatform.ai.model.dto.user.UserQueryRequest;
import com.ai.codeplatform.ai.model.vo.LoginUserVO;
import com.ai.codeplatform.ai.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.ai.codeplatform.ai.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author Administrator
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册（邮箱必填，需通过邮箱验证码校验）
     *
     * @param userAccount   用户账户（不允许包含 @，避免与邮箱登录撞号）
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param email         邮箱（原始输入，内部做归一化与白名单校验）
     * @param emailCode     邮箱验证码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword,
                      String email, String emailCode);

    /**
     * 获取加密密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 逻辑删除用户。删除前改写 email，释放唯一索引，
     * 否则被删用户的邮箱将永久无法再次注册。
     *
     * @param userId 用户 id
     * @return 是否删除成功
     */
    boolean deleteUserLogically(Long userId);

    /**
     * 获取脱敏的用户信息
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
}
