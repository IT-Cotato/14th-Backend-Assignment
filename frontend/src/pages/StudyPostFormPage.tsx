import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { extractErrorMessage } from "../api/client";
import { createStudyPost, getStudyPost, updateStudyPost } from "../api/studyPosts";
import type { StudyStatus } from "../api/studyPosts";

export function StudyPostFormPage() {
  const { postId } = useParams();
  const isEditMode = postId !== undefined;
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [studyStatus, setStudyStatus] = useState<StudyStatus>("RECRUITING");

  const [isAuthor, setIsAuthor] = useState(!isEditMode);
  const [isInitializing, setIsInitializing] = useState(isEditMode);
  const [validationMessage, setValidationMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (!isEditMode) {
      return;
    }

    getStudyPost(Number(postId))
      .then((detail) => {
        setTitle(detail.title);
        setContent(detail.content);
        setStudyStatus(detail.studyStatus);
        setIsAuthor(detail.isAuthor);
      })
      .catch((error) => setErrorMessage(extractErrorMessage(error)))
      .finally(() => setIsInitializing(false));
  }, [isEditMode, postId]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setValidationMessage(null);

    if (title.length < 1 || title.length > 50) {
      setValidationMessage("제목은 1~50자여야 해요.");
      return;
    }
    if (content.length < 1 || content.length > 2000) {
      setValidationMessage("내용은 1~2000자여야 해요.");
      return;
    }

    setIsLoading(true);
    try {
      if (isEditMode) {
        await updateStudyPost(Number(postId), { title, content, studyStatus });
        navigate(`/studies/${postId}`);
      } else {
        const created = await createStudyPost({ title, content });
        navigate(`/studies/${created.postId}`);
      }
    } catch (error) {
      setErrorMessage(extractErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }

  if (isInitializing) {
    return <p>로딩 중이에요...</p>;
  }

  if (isEditMode && !isAuthor) {
    return (
      <section className="mx-auto flex max-w-3xl flex-col gap-3 p-4">
        <p role="alert">수정 권한이 없어요.</p>
        <Link to="/studies">목록으로</Link>
      </section>
    );
  }

  return (
    <section className="mx-auto flex max-w-3xl flex-col gap-4 p-4">
      <h1 className="text-xl font-semibold">{isEditMode ? "모집글 수정" : "모집글 작성"}</h1>
      <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
        <div className="flex flex-col gap-1">
          <label htmlFor="title">제목</label>
          <input id="title" type="text" value={title} onChange={(e) => setTitle(e.target.value)} required />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="content">내용</label>
          <textarea
            id="content"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={8}
            required
          />
        </div>
        {isEditMode && (
          <div className="flex flex-col gap-1">
            <label htmlFor="studyStatus">모집 상태</label>
            <select
              id="studyStatus"
              value={studyStatus}
              onChange={(e) => setStudyStatus(e.target.value as StudyStatus)}
            >
              <option value="RECRUITING">RECRUITING</option>
              <option value="CLOSED">CLOSED</option>
            </select>
          </div>
        )}

        {validationMessage && <p role="alert">{validationMessage}</p>}
        {errorMessage && <p role="alert">{errorMessage}</p>}
        {isLoading && <p>저장하는 중이에요...</p>}

        <button type="submit" disabled={isLoading}>
          {isEditMode ? "수정 완료" : "작성 완료"}
        </button>
      </form>
    </section>
  );
}
