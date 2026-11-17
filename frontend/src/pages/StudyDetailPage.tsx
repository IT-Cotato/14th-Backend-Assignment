import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { extractErrorMessage } from "../api/client";
import { deleteStudyPost, getStudyPost } from "../api/studyPosts";
import type { StudyPostDetail } from "../api/studyPosts";

export function StudyDetailPage() {
  const { postId } = useParams();
  const navigate = useNavigate();

  const [detail, setDetail] = useState<StudyPostDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    setIsLoading(true);
    setErrorMessage(null);
    setNotFound(false);

    getStudyPost(Number(postId))
      .then(setDetail)
      .catch((error) => {
        if (error?.response?.status === 404) {
          setNotFound(true);
        } else {
          setErrorMessage(extractErrorMessage(error));
        }
      })
      .finally(() => setIsLoading(false));
  }, [postId]);

  async function handleDelete() {
    if (!window.confirm("정말 삭제할까요?")) {
      return;
    }
    try {
      await deleteStudyPost(Number(postId));
      navigate("/studies");
    } catch (error) {
      setErrorMessage(extractErrorMessage(error));
    }
  }

  if (isLoading) {
    return <p>로딩 중이에요...</p>;
  }

  if (notFound) {
    return (
      <section className="mx-auto flex max-w-3xl flex-col gap-3 p-4">
        <p role="alert">존재하지 않는 스터디 모집글이에요.</p>
        <Link to="/studies">목록으로</Link>
      </section>
    );
  }

  if (!detail) {
    return (
      <section className="mx-auto flex max-w-3xl flex-col gap-3 p-4">
        {errorMessage && <p role="alert">{errorMessage}</p>}
      </section>
    );
  }

  return (
    <section className="mx-auto flex max-w-3xl flex-col gap-4 p-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">{detail.title}</h1>
        {detail.isAuthor && (
          <div className="flex gap-3">
            <Link to={`/studies/${detail.postId}/edit`}>수정</Link>
            <button type="button" onClick={handleDelete}>
              삭제
            </button>
          </div>
        )}
      </div>

      {errorMessage && <p role="alert">{errorMessage}</p>}

      <dl className="flex flex-col gap-2">
        <div>
          <dt className="font-medium">작성자</dt>
          <dd>
            {detail.author.nickname} · {detail.author.generation}기 · {detail.author.part}
          </dd>
        </div>
        <div>
          <dt className="font-medium">모집 상태</dt>
          <dd>{detail.studyStatus}</dd>
        </div>
        <div>
          <dt className="font-medium">내용</dt>
          <dd className="whitespace-pre-wrap">{detail.content}</dd>
        </div>
        <div>
          <dt className="font-medium">작성일</dt>
          <dd>{detail.createdAt}</dd>
        </div>
        <div>
          <dt className="font-medium">수정일</dt>
          <dd>{detail.updatedAt}</dd>
        </div>
      </dl>

      <Link to="/studies">목록으로</Link>
    </section>
  );
}
