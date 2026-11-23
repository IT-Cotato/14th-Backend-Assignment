import { expect, test } from "@playwright/test";

import {
  API_BASE_URL,
  createCommentViaApi,
  createStudyPostViaApi,
  createUserViaApi,
  generateContent,
  generateTestUser,
  generateTitle,
  signupAndLoginViaUi,
} from "./helpers";

test("댓글 작성 → 목록에 표시, 작성자만 수정/삭제 버튼이 보인다", async ({ page, request }) => {
  const author = generateTestUser();
  const { accessToken: authorToken } = await createUserViaApi(request, author);
  const postId = await createStudyPostViaApi(request, authorToken, {
    title: generateTitle(),
    content: generateContent(),
  });

  const commenter = generateTestUser();
  await signupAndLoginViaUi(page, commenter);

  const commentContent = generateContent("신청합니다");
  await page.goto(`/studies/${postId}`);
  await page.getByLabel("댓글 작성(스터디 신청)").fill(commentContent);
  await page.getByRole("button", { name: "댓글 등록" }).click();

  await expect(page.getByText(commentContent)).toBeVisible();
  await expect(page.getByRole("button", { name: "수정" })).toBeVisible();
  await expect(page.getByRole("button", { name: "삭제" })).toBeVisible();

  // 글 작성자(=댓글 작성자 아님) 입장에서는 이 댓글에 수정/삭제 버튼이 보이면 안 됨
  await page.getByRole("button", { name: "로그아웃" }).click();
  await page.waitForURL("**/login");
  await signupAndLoginViaUi(page, generateTestUser());
  await page.goto(`/studies/${postId}`);
  const commentRow = page.getByText(commentContent).locator("..");
  await expect(commentRow.getByRole("button", { name: "수정" })).toHaveCount(0);
  await expect(commentRow.getByRole("button", { name: "삭제" })).toHaveCount(0);
});

test("마감된 모집글에는 댓글 작성 폼 대신 안내가 보이고, API 직접 호출 시 409가 난다", async ({
  page,
  request,
}) => {
  const author = generateTestUser();
  const { accessToken: authorToken } = await createUserViaApi(request, author);
  const postId = await createStudyPostViaApi(request, authorToken, {
    title: generateTitle(),
    content: generateContent(),
  });
  await request.patch(`${API_BASE_URL}/api/study-posts/${postId}`, {
    headers: { Authorization: `Bearer ${authorToken}` },
    data: { studyStatus: "CLOSED" },
  });

  const other = generateTestUser();
  await signupAndLoginViaUi(page, other);
  await page.goto(`/studies/${postId}`);

  await expect(page.getByText("마감된 모집글에는 댓글을 작성할 수 없어요.")).toBeVisible();
  await expect(page.getByLabel("댓글 작성(스터디 신청)")).toHaveCount(0);

  const otherToken = await page.evaluate(() => localStorage.getItem("accessToken"));
  const response = await request.post(`${API_BASE_URL}/api/study-posts/${postId}/comments`, {
    headers: { Authorization: `Bearer ${otherToken}` },
    data: { content: generateContent() },
  });
  expect(response.status()).toBe(409);
});

test("남의 댓글을 API로 직접 수정/삭제하면 403이 난다", async ({ request }) => {
  const author = generateTestUser();
  const { accessToken: authorToken } = await createUserViaApi(request, author);
  const postId = await createStudyPostViaApi(request, authorToken, {
    title: generateTitle(),
    content: generateContent(),
  });
  const commentId = await createCommentViaApi(
    request,
    authorToken,
    postId,
    generateContent("신청합니다"),
  );

  const other = generateTestUser();
  const { accessToken: otherToken } = await createUserViaApi(request, other);

  const patchRes = await request.patch(`${API_BASE_URL}/api/comments/${commentId}`, {
    headers: { Authorization: `Bearer ${otherToken}` },
    data: { content: "해킹 시도" },
  });
  expect(patchRes.status()).toBe(403);

  const deleteRes = await request.delete(`${API_BASE_URL}/api/comments/${commentId}`, {
    headers: { Authorization: `Bearer ${otherToken}` },
  });
  expect(deleteRes.status()).toBe(403);
});

test("모집글을 삭제하면 달려 있던 댓글도 함께 삭제된다", async ({ request }) => {
  const author = generateTestUser();
  const { accessToken: authorToken } = await createUserViaApi(request, author);
  const postId = await createStudyPostViaApi(request, authorToken, {
    title: generateTitle(),
    content: generateContent(),
  });
  await createCommentViaApi(request, authorToken, postId, generateContent("신청합니다"));

  const beforeRes = await request.get(`${API_BASE_URL}/api/study-posts/${postId}/comments`);
  const beforeBody = await beforeRes.json();
  expect(beforeBody.data.totalElements).toBe(1);

  const deleteRes = await request.delete(`${API_BASE_URL}/api/study-posts/${postId}`, {
    headers: { Authorization: `Bearer ${authorToken}` },
  });
  expect(deleteRes.status()).toBe(204);

  const afterRes = await request.get(`${API_BASE_URL}/api/study-posts/${postId}/comments`);
  expect(afterRes.status()).toBe(404);
});
