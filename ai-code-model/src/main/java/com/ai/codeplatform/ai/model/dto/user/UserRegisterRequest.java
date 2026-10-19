package com.ai.codeplatform.ai.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     * <p>
     * 注意：不允许包含 @。userAccount 与 email 是两个独立的唯一索引、跨列不互斥，
     * 若账号里出现 @ 会与邮箱登录产生歧义。
     */
    private String userAccount;

    /**
     * 邮箱（注册必填，需通过邮箱验证码校验）
     */
    private String email;

    /**
     * 邮箱验证码
     */
    private String emailCode;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
