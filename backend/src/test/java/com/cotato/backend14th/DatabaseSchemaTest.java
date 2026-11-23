package com.cotato.backend14th;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 MySQL 스키마가 ERD(docs/erd/erdCloud.png)와 맞는지 확인해요. 데이터는 읽기만 해요.
// ddl-auto: update는 기존 컬럼 타입을 바꾸지 않아서, 엔티티와 DB가 어긋나도 앱은 그대로 떠요.
@SpringBootTest
class DatabaseSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Enum 컬럼은 MySQL ENUM이 아니라 ERD와 같은 VARCHAR예요")
    void enumColumnsAreVarchar() {
        assertThat(columnType("user", "part")).isEqualTo("varchar(2)");
        assertThat(columnType("study_post", "study_status")).isEqualTo("varchar(20)");
    }

    @Test
    @DisplayName("ERD의 문자열 길이 제한이 DB 컬럼에 반영돼 있어요")
    void varcharLengthsMatchErd() {
        assertThat(columnType("user", "nickname")).isEqualTo("varchar(10)");
        assertThat(columnType("study_post", "title")).isEqualTo("varchar(50)");
        assertThat(columnType("study_post", "content")).isEqualTo("varchar(2000)");
        assertThat(columnType("study_post_comment", "content")).isEqualTo("varchar(500)");
    }

    @Test
    @DisplayName("댓글의 모집글 FK는 ON DELETE CASCADE예요")
    void commentPostForeignKeyCascades() {
        String deleteRule = jdbcTemplate.queryForObject("""
                SELECT delete_rule FROM information_schema.referential_constraints
                WHERE constraint_schema = DATABASE()
                  AND LOWER(table_name) = 'study_post_comment'
                  AND LOWER(referenced_table_name) = 'study_post'
                """, String.class);

        assertThat(deleteRule).isEqualTo("CASCADE");
    }

    private String columnType(String table, String column) {
        return jdbcTemplate.queryForObject("""
                SELECT LOWER(column_type) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND LOWER(table_name) = ?
                  AND LOWER(column_name) = ?
                """, String.class, table, column);
    }
}
