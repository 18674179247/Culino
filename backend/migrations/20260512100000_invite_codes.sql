-- 邀请码系统（方案 B：一次性/限次邀请码）
--
-- 设计要点：
--   * code 列为明文随机字符串，使用 UNIQUE 索引精确查询
--   * max_uses=NULL 表示不限次数；used_count < max_uses 时可继续使用
--   * expires_at=NULL 表示永不过期
--   * created_by 引用 users(id) ON DELETE SET NULL：创建者被删除时保留邀请码审计记录
--   * note 字段用于管理员记录邀请对象（如 "给老王的"）

CREATE TABLE invite_codes (
    code        VARCHAR(64) PRIMARY KEY,
    created_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    max_uses    INT         NOT NULL DEFAULT 1 CHECK (max_uses > 0),
    used_count  INT         NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    expires_at  TIMESTAMPTZ,
    note        VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invite_codes_created_by ON invite_codes(created_by);

-- users 表增加 invited_by 字段，记录该用户注册时使用了哪个邀请码，便于追溯
ALTER TABLE users
    ADD COLUMN invited_by VARCHAR(64) REFERENCES invite_codes(code) ON DELETE SET NULL;

CREATE INDEX idx_users_invited_by ON users(invited_by);
