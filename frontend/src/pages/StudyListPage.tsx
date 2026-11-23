import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";

import { extractErrorMessage } from "../api/client";
import { getStudyPosts } from "../api/studyPosts";
import type { StudyPostListItem, StudyStatus } from "../api/studyPosts";
import type { PageResponse, Part } from "../api/types";
import { PART_OPTIONS } from "../constants/part";
import { useAuth } from "../context/AuthContext";

export function StudyListPage() {
  const { user } = useAuth();

  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<StudyStatus | "">("");
  const [part, setPart] = useState<Part | "">("");
  const [page, setPage] = useState(0);

  const [data, setData] = useState<PageResponse<StudyPostListItem> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setErrorMessage(null);

    getStudyPosts({
      keyword: keyword || undefined,
      status: status || undefined,
      part: part || undefined,
      page,
      size: 10,
    })
      .then((result) => {
        if (!cancelled) {
          setData(result);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setErrorMessage(extractErrorMessage(error));
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [keyword, status, part, page]);

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setKeyword(keywordInput);
  }

  return (
    <section className="mx-auto flex max-w-3xl flex-col gap-4 p-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">스터디 목록</h1>
        {user && <Link to="/studies/new">글쓰기</Link>}
      </div>

      <form className="flex flex-wrap items-end gap-3" onSubmit={handleSearchSubmit}>
        <div className="flex flex-col gap-1">
          <label htmlFor="keyword">키워드</label>
          <input
            id="keyword"
            type="text"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
          />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="status">모집 상태</label>
          <select
            id="status"
            value={status}
            onChange={(e) => {
              setPage(0);
              setStatus(e.target.value as StudyStatus | "");
            }}
          >
            <option value="">전체</option>
            <option value="RECRUITING">RECRUITING</option>
            <option value="CLOSED">CLOSED</option>
          </select>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="part">파트</label>
          <select
            id="part"
            value={part}
            onChange={(e) => {
              setPage(0);
              setPart(e.target.value as Part | "");
            }}
          >
            <option value="">전체</option>
            {PART_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
        <button type="submit">검색</button>
      </form>

      {isLoading && <p>목록을 불러오는 중이에요...</p>}
      {errorMessage && <p role="alert">{errorMessage}</p>}

      {data && (
        <>
          <ul className="flex flex-col gap-3">
            {data.content.map((item) => (
              <li key={item.postId} className="flex flex-col gap-1 border-b border-gray-200 pb-3">
                <Link to={`/studies/${item.postId}`} className="font-medium">
                  {item.title}
                </Link>
                <span>
                  {item.author.nickname} · {item.author.generation}기 · {item.author.part}
                </span>
                <span>
                  {item.studyStatus} · 댓글 {item.commentCount} · {item.createdAt}
                </span>
              </li>
            ))}
            {data.content.length === 0 && <li>조건에 맞는 모집글이 없어요.</li>}
          </ul>

          <div className="flex items-center gap-3">
            <button
              type="button"
              disabled={data.page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              이전
            </button>
            <span>
              페이지 {data.page + 1} / {Math.max(data.totalPages, 1)}
            </span>
            <button
              type="button"
              disabled={data.page + 1 >= data.totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              다음
            </button>
          </div>
        </>
      )}
    </section>
  );
}
