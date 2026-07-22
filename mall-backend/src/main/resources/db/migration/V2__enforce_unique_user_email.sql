UPDATE sys_user
SET email = NULL
WHERE email IS NOT NULL AND LENGTH(TRIM(email)) = 0;

ALTER TABLE sys_user
    MODIFY COLUMN email VARCHAR(254) DEFAULT NULL COMMENT '邮箱';

CREATE UNIQUE INDEX uk_sys_user_email ON sys_user (email);
