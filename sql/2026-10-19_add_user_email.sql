-- ============================================================
-- 2026-10-19  user 表新增 email / emailVerified（微服务版）
-- 用途：邮箱注册（必填 + 验证码校验）与「账号或邮箱」登录
-- ============================================================
--
-- 【执行位置】直接在业务库执行。
--   微服务架构不使用主从读写分离，各服务（app / user / log）连的是同一个单库：
--     开发环境：192.168.100.100:3307/ai_code_platform
--     生产环境：${SPRING_MYSQL_URL}:${SPRING_MYSQL_PORT}/ai_code_platform
--   只需执行一次，所有服务立即生效（它们共用同一张表）。
--
-- 【幂等性】MySQL 8 不支持 ADD COLUMN IF NOT EXISTS。
--   重复执行会报：
--     1060 Duplicate column name      -> 列已存在
--     1061 Duplicate key name         -> 索引已存在
--   看到这两个错误说明之前已经执行成功，忽略即可。
--
-- 【锁与耗时】MySQL 8.0.29+ 的 ADD COLUMN 支持 INSTANT 算法（含指定 AFTER 位置），
--   加二级唯一索引默认 INPLACE、不阻塞读写。
--
-- 【回滚】
--   ALTER TABLE `user`
--     DROP INDEX `uk_user_email`,
--     DROP COLUMN `emailVerified`,
--     DROP COLUMN `email`;
--   注意：DROP COLUMN email 会丢掉已录入的邮箱，不可恢复。
-- ============================================================


-- ------------------------------------------------------------
-- 步骤 1：备份（生产环境务必执行）
-- ------------------------------------------------------------
--   mysqldump -h<host> -P<port> -uroot -p ai_code_platform user > user_bak_20261019.sql


-- ------------------------------------------------------------
-- 步骤 2：上线前自检
-- ------------------------------------------------------------

-- 2.1 确认列尚未存在（返回 0 行 = 还没执行过）
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user'
  AND COLUMN_NAME IN ('email', 'emailVerified');

-- 2.2 【重要】检查是否已有 userAccount 含 @ 的历史账号
--   userAccount 与 email 是两个独立的唯一索引，跨列不互斥。
--   新的登录逻辑按「输入含 @ 走 email 查询，否则走 userAccount」分流，
--   因此 userAccount 里带 @ 的历史账号在改动后将无法登录，必须先处理。
--   预期返回 0 行。
SELECT id, userAccount, githubId
FROM `user`
WHERE userAccount LIKE '%@%';

-- 说明：不需要做「检查 email 是否已有重复值」的前置校验 ——
-- 该列此刻尚不存在，不可能有重复值。


-- ------------------------------------------------------------
-- 步骤 3：正式执行
-- ------------------------------------------------------------
-- 说明：
--   email 统一存「小写归一化后」的值（Gmail 会去掉 +后缀 与 点号），
--   否则 A@qq.com / a@qq.com 会被当成两个账号，唯一索引形同虚设。
--   emailVerified=0 是默认值，存量行自动填 0，无需回填。
--   唯一索引允许多个 NULL，所以存量用户（email 为 NULL）不会互相冲突。
ALTER TABLE `user`
  ADD COLUMN `email` VARCHAR(256) NULL COMMENT '邮箱（小写归一化后存储）' AFTER `userAccount`,
  ADD COLUMN `emailVerified` TINYINT NOT NULL DEFAULT 0 COMMENT '邮箱是否经本站验证：0否 1是' AFTER `email`,
  ADD UNIQUE KEY `uk_user_email` (`email`);


-- ------------------------------------------------------------
-- 步骤 4：校验结果
-- ------------------------------------------------------------
SHOW COLUMNS FROM `user` LIKE 'email%';

SHOW INDEX FROM `user` WHERE Key_name = 'uk_user_email';

-- 期望：
--   email           varchar(256)  YES  UNI  NULL
--   emailVerified   tinyint       NO        0
--   uk_user_email   Non_unique=0，Column_name=email
