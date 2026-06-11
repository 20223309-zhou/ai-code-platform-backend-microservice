package com.ai.codeplatform.constant;

public interface AppConstant {

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用生成目录（可通过 -Dapp.code.output.dir 或环境变量 APP_CODE_OUTPUT_DIR 覆盖）
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("app.code.output.dir",
            System.getenv().getOrDefault("APP_CODE_OUTPUT_DIR",
                    System.getProperty("user.dir") + "/tmp/code_output"));

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("app.code.deploy.dir",
            System.getenv().getOrDefault("APP_CODE_DEPLOY_DIR",
                    System.getProperty("user.dir") + "/tmp/code_deploy"));

    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";

}
