-- 코테이토 스터디 매칭 플랫폼 스키마 (MySQL)
-- JPA 엔티티에서 Hibernate로 생성한 DDL을 정리한 것. ERDCloud "SQL 가져오기"에 그대로 붙여넣는다.
-- 엔티티를 바꾸면 이 파일과 ERD 이미지도 함께 고친다.

CREATE TABLE users (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
    email       VARCHAR(255)  NOT NULL COMMENT '이메일',
    password    VARCHAR(255)  NOT NULL COMMENT '비밀번호 (BCrypt)',
    name        VARCHAR(50)   NOT NULL COMMENT '이름',
    nickname    VARCHAR(10)   NOT NULL COMMENT '닉네임',
    generation  INT           NOT NULL COMMENT '기수 (1 이상)',
    part        ENUM('PM','DE','BE','FE') NOT NULL COMMENT '파트',
    created_at  DATETIME(6)   NULL COMMENT '생성일시',
    updated_at  DATETIME(6)   NULL COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB COMMENT='회원';

CREATE TABLE study_post (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '모집글 ID',
    user_id       BIGINT        NOT NULL COMMENT '작성자 ID',
    title         VARCHAR(50)   NOT NULL COMMENT '제목',
    content       VARCHAR(2000) NOT NULL COMMENT '내용',
    study_status  ENUM('RECRUITING','CLOSED') NOT NULL COMMENT '모집 상태',
    created_at    DATETIME(6)   NULL COMMENT '생성일시',
    updated_at    DATETIME(6)   NULL COMMENT '수정일시',
    PRIMARY KEY (id),
    CONSTRAINT fk_study_post_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='스터디 모집글';

CREATE TABLE study_post_comment (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '댓글 ID',
    study_post_id  BIGINT        NOT NULL COMMENT '모집글 ID',
    user_id        BIGINT        NOT NULL COMMENT '작성자(신청자) ID',
    content        VARCHAR(500)  NOT NULL COMMENT '내용',
    created_at     DATETIME(6)   NULL COMMENT '생성일시',
    updated_at     DATETIME(6)   NULL COMMENT '수정일시',
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_study_post FOREIGN KEY (study_post_id) REFERENCES study_post (id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='모집글 댓글 (= 스터디 신청)';

CREATE TABLE blacklisted_token (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '토큰 ID',
    token       VARCHAR(512)  NOT NULL COMMENT '로그아웃한 accessToken',
    expires_at  DATETIME(6)   NOT NULL COMMENT '토큰 만료 시각',
    created_at  DATETIME(6)   NULL COMMENT '생성일시',
    updated_at  DATETIME(6)   NULL COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blacklisted_token_token (token)
) ENGINE=InnoDB COMMENT='로그아웃 토큰 (추가 테이블)';
