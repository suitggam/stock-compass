CREATE DATABASE IF NOT EXISTS stockapp
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE stockapp;

-- users (변경 반영)
CREATE TABLE IF NOT EXISTS users (
  user_no        INT AUTO_INCREMENT PRIMARY KEY,
  social_email   VARCHAR(254) NOT NULL,
  nickname       VARCHAR(30)  NOT NULL,
  cancel         TINYINT(1)   NOT NULL DEFAULT 0,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  total_reward   INT          NOT NULL DEFAULT 10000000 COMMENT '기본지급금+상금+추급',
  cash           INT          NOT NULL DEFAULT 10000000 COMMENT '매수 가능 금액',
  CONSTRAINT uq_users_email    UNIQUE (social_email),
  CONSTRAINT uq_users_nickname UNIQUE (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 소셜 계정 연결 (크기만 조정)
CREATE TABLE IF NOT EXISTS oauth_identities (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_no           INT           NOT NULL,
  provider          ENUM('GOOGLE','KAKAO') NOT NULL,
  provider_user_id  VARCHAR(191)  NOT NULL,     -- 길이 조정
  provider_email    VARCHAR(254)  NULL,         -- 길이 조정
  profile_image_url VARCHAR(255)  NULL,
  email_verified    TINYINT(1)    NOT NULL DEFAULT 0,
  connected_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_provider_subject UNIQUE (provider, provider_user_id),
  INDEX idx_userno (user_no),
  CONSTRAINT fk_oauth_user FOREIGN KEY (user_no) REFERENCES users(user_no)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- (선택) 관심종목
CREATE TABLE IF NOT EXISTS favorite_stocks (
  interest_no  BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_no      INT        NOT NULL,
  stock_pk     BIGINT     NOT NULL,
  ISCANCEL     TINYINT(1) NOT NULL DEFAULT 0,
  created_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME   NULL ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_user_stock UNIQUE (user_no, stock_pk),
  INDEX idx_fav_user (user_no),
  CONSTRAINT fk_fav_user FOREIGN KEY (user_no) REFERENCES users(user_no)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- (선택) 리프레시 토큰
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_no    INT      NOT NULL,
  token_hash VARCHAR(255) NOT NULL,
  issued_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME NOT NULL,
  revoked    TINYINT(1) NOT NULL DEFAULT 0,
  user_agent VARCHAR(255) NULL,
  ip         VARCHAR(45)  NULL,
  INDEX idx_rt_user (user_no),
  CONSTRAINT fk_rt_user FOREIGN KEY (user_no) REFERENCES users(user_no)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;