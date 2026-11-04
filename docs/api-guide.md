# API 가이드

## 공통 응답 포맷

모든 API는 `ApiResponse<T>`로 감싸서 응답한다.

```json
{
  "success": true,
  "status": 200,
  "data": { },
  "timestamp": "2026-11-04 12:00:00"
}
```

- `success`: 요청 처리 성공 여부
- `status`: HTTP 상태 코드
- `data`: 실제 응답 데이터 (없으면 필드 자체가 생략됨)
- `timestamp`: 응답 생성 시각 (Asia/Seoul)

에러 응답은 `ErrorResponse`로 통일하며, `GlobalExceptionHandler`가 `Backend14thException`을 포착해 `ErrorCode`에 정의된 상태 코드/메시지로 변환한다.

## Book API

| Method | URL | 설명 |
| --- | --- | --- |
| GET | `/books/{id}` | ID로 책 단건 조회 |
| POST | `/books` | 책 등록 |

### GET /books/{id}

- Path Variable: `id` (Long)
- 성공 시 `200 OK` + `BookResponse` (`id`, `title`, `author`)
- 존재하지 않는 id면 `BOOK_NOT_FOUND` 에러 응답

### POST /books

- Request Body: `BookRequest` (`title`, `author`)
- 성공 시 `201 Created` + `BookResponse`

## Swagger

로컬 실행 후 아래 주소에서 전체 API 스펙과 테스트 UI를 확인할 수 있다.

```
http://localhost:8080/swagger-ui/index.html
```
