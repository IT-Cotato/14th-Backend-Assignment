import { expect, test } from "@playwright/test";

import {
  API_BASE_URL,
  createUserViaApi,
  generateTestUser,
  loginViaUi,
  signupViaUi,
} from "./helpers";

test("회원가입 성공 후 로그인하면 목록으로 이동하고, 마이페이지에 정보가 보이고, 로그아웃하면 비로그인 상태로 돌아간다", async ({
  page,
}) => {
  const user = generateTestUser();

  await signupViaUi(page, user);
  await page.waitForURL("**/login");

  await loginViaUi(page, user);
  await page.waitForURL("**/studies");

  await page.getByRole("link", { name: "마이페이지" }).click();
  await expect(page.getByText(user.email, { exact: true })).toBeVisible();
  await expect(page.getByText(user.name, { exact: true })).toBeVisible();
  await expect(page.getByText(user.nickname, { exact: true })).toBeVisible();
  await expect(page.getByText(String(user.generation), { exact: true })).toBeVisible();
  await expect(page.getByText(user.part, { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "로그아웃" }).click();
  await page.waitForURL("**/login");
  await expect(page.getByRole("link", { name: "로그인", exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "회원가입", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "로그아웃" })).toHaveCount(0);
});

test("로그아웃한 토큰으로 API를 직접 호출하면 401이 난다", async ({ page, request }) => {
  const user = generateTestUser();

  await signupViaUi(page, user);
  await page.waitForURL("**/login");
  await loginViaUi(page, user);
  await page.waitForURL("**/studies");

  const token = await page.evaluate(() => localStorage.getItem("accessToken"));
  expect(token).toBeTruthy();

  await page.getByRole("button", { name: "로그아웃" }).click();
  await page.waitForURL("**/login");

  const response = await request.get(`${API_BASE_URL}/api/users/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.status()).toBe(401);
});

test("중복된 이메일로 가입하면 에러 메시지가 보인다", async ({ page, request }) => {
  const user = generateTestUser();
  await createUserViaApi(request, user);

  await signupViaUi(page, user);

  await expect(page.getByRole("alert")).toBeVisible();
});

test("잘못된 비밀번호로 로그인하면 에러 메시지가 보인다", async ({ page, request }) => {
  const user = generateTestUser();
  await createUserViaApi(request, user);

  await page.goto("/login");
  await page.getByLabel("이메일").fill(user.email);
  await page.getByLabel("비밀번호").fill("wrong-password-123");
  await page.getByRole("button", { name: "로그인" }).click();

  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page).toHaveURL(/\/login$/);
});

test("비밀번호가 8자 미만이면 회원가입 검증 메시지가 보인다", async ({ page }) => {
  const user = generateTestUser();

  await page.goto("/signup");
  await page.getByLabel("이메일").fill(user.email);
  await page.getByLabel("비밀번호").fill("short1");
  await page.getByLabel("이름").fill(user.name);
  await page.getByLabel("닉네임").fill(user.nickname);
  await page.getByLabel("기수").fill(String(user.generation));
  await page.getByLabel("파트").selectOption({ label: user.partLabel });
  await page.getByRole("button", { name: "회원가입" }).click();

  await expect(page.getByText("비밀번호는 8자 이상이어야 해요.")).toBeVisible();
  await expect(page).toHaveURL(/\/signup$/);
});

test("비로그인으로 /studies/new, /mypage에 접근하면 로그인 화면으로 이동한다", async ({ page }) => {
  await page.goto("/studies/new");
  await page.waitForURL("**/login");

  await page.goto("/mypage");
  await page.waitForURL("**/login");
});
