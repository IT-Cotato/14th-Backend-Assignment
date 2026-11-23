import { expect, test } from "@playwright/test";

import {
  API_BASE_URL,
  createStudyPostViaApi,
  createUserViaApi,
  generateContent,
  generateTestUser,
  generateTitle,
  signupAndLoginViaUi,
} from "./helpers";

test("작성 후 상세에서 제목/내용/작성자 정보가 보인다", async ({ page }) => {
  const user = generateTestUser();
  await signupAndLoginViaUi(page, user);

  const title = generateTitle();
  const content = generateContent();

  await page.getByRole("link", { name: "글쓰기" }).click();
  await page.waitForURL("**/studies/new");
  await page.getByLabel("제목").fill(title);
  await page.getByLabel("내용").fill(content);
  await page.getByRole("button", { name: "작성 완료" }).click();
  await page.waitForURL(/\/studies\/\d+$/);

  await expect(page.getByRole("heading", { name: title })).toBeVisible();
  await expect(page.getByText(content)).toBeVisible();
  await expect(
    page.getByText(`${user.nickname} · ${user.generation}기 · ${user.part}`, { exact: true }),
  ).toBeVisible();
});

test("목록에서 키워드/상태/파트 필터와 페이지 이동이 동작한다", async ({ page, request }) => {
  const user = generateTestUser({ part: "FE" });
  const { accessToken } = await createUserViaApi(request, user);

  const keyword = generateTitle("e2e-page-filter");
  const postCount = 11;
  for (let i = 0; i < postCount; i += 1) {
    await createStudyPostViaApi(request, accessToken, {
      title: `${keyword} ${i}`,
      content: generateContent(),
    });
  }

  await page.goto("/studies");
  await page.getByLabel("키워드").fill(keyword);
  await page.getByRole("button", { name: "검색" }).click();

  await expect(page.getByText("페이지 1 /")).toBeVisible();
  const nextButton = page.getByRole("button", { name: "다음" });
  await expect(nextButton).toBeEnabled();

  await page.getByLabel("모집 상태").selectOption({ label: "RECRUITING" });
  await page.getByLabel("파트").selectOption({ label: user.partLabel });
  await page.getByRole("button", { name: "검색" }).click();
  await expect(page.getByText(keyword, { exact: false }).first()).toBeVisible();

  await nextButton.click();
  await expect(page.getByText("페이지 2 /")).toBeVisible();
});

test("작성자가 제목/내용/모집 상태를 수정하고, 삭제하면 목록에서 사라진다", async ({ page }) => {
  const user = generateTestUser();
  await signupAndLoginViaUi(page, user);

  const title = generateTitle();
  await page.getByRole("link", { name: "글쓰기" }).click();
  await page.waitForURL("**/studies/new");
  await page.getByLabel("제목").fill(title);
  await page.getByLabel("내용").fill(generateContent());
  await page.getByRole("button", { name: "작성 완료" }).click();
  await page.waitForURL(/\/studies\/\d+$/);

  const newTitle = generateTitle("수정됨");
  const newContent = generateContent("수정 내용");
  await page.getByRole("link", { name: "수정" }).click();
  await page.getByLabel("제목").fill(newTitle);
  await page.getByLabel("내용").fill(newContent);
  await page.getByLabel("모집 상태").selectOption({ label: "CLOSED" });
  await page.getByRole("button", { name: "수정 완료" }).click();
  await page.waitForURL(/\/studies\/\d+$/);

  await expect(page.getByRole("heading", { name: newTitle })).toBeVisible();
  await expect(page.getByText(newContent)).toBeVisible();
  await expect(page.getByText("CLOSED", { exact: true })).toBeVisible();

  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "삭제" }).click();
  await page.waitForURL("**/studies");

  await page.getByLabel("키워드").fill(newTitle);
  await page.getByRole("button", { name: "검색" }).click();
  await expect(page.getByText("조건에 맞는 모집글이 없어요.")).toBeVisible();
});

test("다른 사용자에게는 수정/삭제 버튼이 보이지 않고, API 직접 호출 시 403이 난다", async ({
  page,
  request,
}) => {
  const author = generateTestUser();
  const { accessToken: authorToken } = await createUserViaApi(request, author);
  const postId = await createStudyPostViaApi(request, authorToken, {
    title: generateTitle(),
    content: generateContent(),
  });

  const other = generateTestUser();
  await signupAndLoginViaUi(page, other);

  await page.goto(`/studies/${postId}`);
  await expect(page.getByRole("link", { name: "수정" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "삭제" })).toHaveCount(0);

  const otherToken = await page.evaluate(() => localStorage.getItem("accessToken"));

  const patchRes = await request.patch(`${API_BASE_URL}/api/study-posts/${postId}`, {
    headers: { Authorization: `Bearer ${otherToken}` },
    data: { title: "해킹 시도" },
  });
  expect(patchRes.status()).toBe(403);

  const deleteRes = await request.delete(`${API_BASE_URL}/api/study-posts/${postId}`, {
    headers: { Authorization: `Bearer ${otherToken}` },
  });
  expect(deleteRes.status()).toBe(403);
});

test("존재하지 않는 글 상세에 접근하면 404 처리 화면이 보인다", async ({ page }) => {
  await page.goto("/studies/999999999");
  await expect(page.getByText("존재하지 않는 스터디 모집글이에요.")).toBeVisible();
  await page.getByRole("link", { name: "목록으로" }).click();
  await page.waitForURL("**/studies");
});
