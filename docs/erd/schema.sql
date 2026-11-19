-- 코테이토 스터디 매칭 플랫폼 (MySQL 8)
-- JPA 엔티티와 같은 구조다. ERDCloud의 "Import > SQL" 로 불러올 수 있다.

CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
    email       VARCHAR(100) NOT NULL COMMENT '이메일(로그인 ID)',
    password    VARCHAR(60)  NOT NULL COMMENT 'BCrypt 해시',
    name        VARCHAR(20)  NOT NULL COMMENT '이름',
    nickname    VARCHAR(10)  NOT NULL COMMENT '닉네임',
    generation  INT          NOT NULL COMMENT '기수(1 이상)',
    part        VARCHAR(2)   NOT NULL COMMENT 'PM, DE, BE, FE',
    created_at  DATETIME(6)  NULL COMMENT '생성 시각',
    updated_at  DATETIME(6)  NULL COMMENT '수정 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) COMMENT '회원';

CREATE TABLE study_post (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '모집글 ID',
    user_id       BIGINT        NOT NULL COMMENT '작성자',
    title         VARCHAR(50)   NOT NULL COMMENT '제목',
    content       VARCHAR(2000) NOT NULL COMMENT '내용',
    study_status  VARCHAR(20)   NOT NULL COMMENT 'RECRUITING, CLOSED',
    created_at    DATETIME(6)   NULL COMMENT '생성 시각',
    updated_at    DATETIME(6)   NULL COMMENT '수정 시각',
    PRIMARY KEY (id),
    CONSTRAINT fk_study_post_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '스터디 모집글';

CREATE TABLE study_post_comment (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '댓글 ID',
    study_post_id  BIGINT       NOT NULL COMMENT '모집글',
    user_id        BIGINT       NOT NULL COMMENT '작성자(신청자)',
    content        VARCHAR(500) NOT NULL COMMENT '신청 내용',
    created_at     DATETIME(6)  NULL COMMENT '생성 시각',
    updated_at     DATETIME(6)  NULL COMMENT '수정 시각',
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_study_post FOREIGN KEY (study_post_id) REFERENCES study_post (id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '모집글 댓글 = 스터디 신청';

CREATE TABLE blacklisted_token (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    token_hash  VARCHAR(64) NOT NULL COMMENT '로그아웃한 토큰의 SHA-256',
    expires_at  DATETIME(6) NOT NULL COMMENT '토큰 만료 시각(이후 삭제)',
    PRIMARY KEY (id),
    CONSTRAINT uk_blacklisted_token_hash UNIQUE (token_hash)
) COMMENT '로그아웃한 토큰';
