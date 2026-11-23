import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";

import { createComment, deleteComment, getComments, updateComment } from "../api/comments";
import type { CommentListItem } from "../api/comments";
import { extractErrorMessage } from "../api/client";
import type { PageResponse } from "../api/types";
import { useAuth } from "../context/AuthContext";

interface CommentSectionProps {
  postId: number;
  isPostClosed: boolean;
}

const PAGE_SIZE = 20;

export function CommentSection({ postId, isPostClosed }: CommentSectionProps) {
  const { user } = useAuth();

  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<CommentListItem> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [newContent, setNewContent] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingContent, setEditingContent] = useState("");

  const fetchComments = useCallback(() => {
    setIsLoading(true);
    setErrorMessage(null);
    return getComments(postId, page, PAGE_SIZE)
      .then(setData)
      .catch((error) => setErrorMessage(extractErrorMessage(error)))
      .finally(() => setIsLoading(false));
  }, [postId, page]);

  useEffect(() => {
    fetchComments();
  }, [fetchComments]);

  async function handleCreateSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitError(null);

    if (newContent.length < 1 || newContent.length > 500) {
      setSubmitError("내용은 1~500자여야 해요.");
      return;
    }

    setIsSubmitting(true);
    try {
      await createComment(postId, { content: newContent });
      setNewContent("");
      setPage(0);
      await fetchComments();
    } catch (error) {
      setSubmitError(extractErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  function startEdit(comment: CommentListItem) {
    setEditingId(comment.commentId);
    setEditingContent(comment.content);
  }

  async function handleEditSave(commentId: number) {
    try {
      await updateComment(commentId, { content: editingContent });
      setEditingId(null);
      await fetchComments();
    } catch (error) {
      setErrorMessage(extractErrorMessage(error));
    }
  }

  async function handleDelete(commentId: number) {
    if (!window.confirm("댓글을 삭제할까요?")) {
      return;
    }
    try {
      await deleteComment(commentId);
      await fetchComments();
    } catch (error) {
      setErrorMessage(extractErrorMessage(error));
    }
  }

  return (
    <div className="flex flex-col gap-4 border-t border-gray-200 pt-4">
      <h2 className="text-lg font-semibold">댓글(스터디 신청) {data?.totalElements ?? 0}개</h2>

      {isLoading && <p>댓글을 불러오는 중이에요...</p>}
      {errorMessage && <p role="alert">{errorMessage}</p>}

      {data && (
        <>
          <ul className="flex flex-col gap-3">
            {data.content.map((comment) => (
              <li key={comment.commentId} className="flex flex-col gap-1 border-b border-gray-200 pb-2">
                {editingId === comment.commentId ? (
                  <div className="flex flex-col gap-2">
                    <label htmlFor={`edit-comment-${comment.commentId}`}>댓글 수정</label>
                    <textarea
                      id={`edit-comment-${comment.commentId}`}
                      value={editingContent}
                      onChange={(e) => setEditingContent(e.target.value)}
                      rows={3}
                    />
                    <div className="flex gap-2">
                      <button type="button" onClick={() => handleEditSave(comment.commentId)}>
                        저장
                      </button>
                      <button type="button" onClick={() => setEditingId(null)}>
                        취소
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <p className="whitespace-pre-wrap">{comment.content}</p>
                    <span className="text-sm">
                      {comment.author.nickname} · {comment.author.generation}기 · {comment.author.part} ·{" "}
                      {comment.createdAt}
                    </span>
                    {comment.isAuthor && (
                      <div className="flex gap-2">
                        <button type="button" onClick={() => startEdit(comment)}>
                          수정
                        </button>
                        <button type="button" onClick={() => handleDelete(comment.commentId)}>
                          삭제
                        </button>
                      </div>
                    )}
                  </>
                )}
              </li>
            ))}
            {data.content.length === 0 && <li>아직 댓글이 없어요.</li>}
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

      {user ? (
        isPostClosed ? (
          <p role="alert">마감된 모집글에는 댓글을 작성할 수 없어요.</p>
        ) : (
          <form className="flex flex-col gap-2" onSubmit={handleCreateSubmit}>
            <label htmlFor="newComment">댓글 작성(스터디 신청)</label>
            <textarea
              id="newComment"
              value={newContent}
              onChange={(e) => setNewContent(e.target.value)}
              rows={3}
            />
            {submitError && <p role="alert">{submitError}</p>}
            {isSubmitting && <p>등록하는 중이에요...</p>}
            <button type="submit" disabled={isSubmitting}>
              댓글 등록
            </button>
          </form>
        )
      ) : (
        <p>
          <Link to="/login">로그인</Link> 후 댓글(스터디 신청)을 작성할 수 있어요.
        </p>
      )}
    </div>
  );
}
