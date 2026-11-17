import { randomUUID } from "node:crypto";
import type { APIRequestContext, Page } from "@playwright/test";

export const API_BASE_URL = "http://localhost:8080";

export interface TestUser {
  email: string;
  password: string;
  name: string;
  nickname: string;
  generation: number;
  part: "PM" | "DE" | "BE" | "FE";
  partLabel: string;
}

const PART_LABELS: Record<TestUser["part"], string> = {
  PM: "PM (기획)",
  DE: "DE (디자인)",
  BE: "BE (백엔드)",
  FE: "FE (프론트엔드)",
};

export function generateTestUser(overrides: Partial<TestUser> = {}): TestUser {
  const unique = randomUUID();
  const part = overrides.part ?? "BE";
  return {
    email: `e2e-${unique}@example.com`,
    // 비밀번호는 코드에 리터럴로 적지 않고 실행할 때마다 새로 생성해요.
    password: randomUUID().replace(/-/g, "").slice(0, 12),
    name: "홍길동",
    nickname: `닉${unique.slice(0, 6)}`,
    generation: 14,
    part,
    partLabel: PART_LABELS[part],
    ...overrides,
  };
}

export function generateTitle(prefix = "e2e 모집글"): string {
  return `${prefix} ${Date.now()}-${randomUUID().slice(0, 8)}`;
}

export function generateContent(prefix = "e2e 테스트 내용"): string {
  return `${prefix} ${randomUUID()}`;
}

export async function signupViaUi(page: Page, user: TestUser): Promise<void> {
  await page.goto("/signup");
  await page.getByLabel("이메일").fill(user.email);
  await page.getByLabel("비밀번호").fill(user.password);
  await page.getByLabel("이름").fill(user.name);
  await page.getByLabel("닉네임").fill(user.nickname);
  await page.getByLabel("기수").fill(String(user.generation));
  await page.getByLabel("파트").selectOption({ label: user.partLabel });
  await page.getByRole("button", { name: "회원가입" }).click();
}

export async function loginViaUi(page: Page, user: TestUser): Promise<void> {
  await page.getByLabel("이메일").fill(user.email);
  await page.getByLabel("비밀번호").fill(user.password);
  await page.getByRole("button", { name: "로그인" }).click();
}

export async function signupAndLoginViaUi(page: Page, user: TestUser): Promise<void> {
  await signupViaUi(page, user);
  await page.waitForURL("**/login");
  await loginViaUi(page, user);
  await page.waitForURL("**/studies");
}

export async function createUserViaApi(
  request: APIRequestContext,
  user: TestUser,
): Promise<{ userId: number; accessToken: string }> {
  const signupRes = await request.post(`${API_BASE_URL}/api/auth/signup`, {
    data: {
      email: user.email,
      password: user.password,
      name: user.name,
      nickname: user.nickname,
      generation: user.generation,
      part: user.part,
    },
  });
  const signupBody = await signupRes.json();
  const userId = signupBody.data.userId;

  const loginRes = await request.post(`${API_BASE_URL}/api/auth/login`, {
    data: { email: user.email, password: user.password },
  });
  const loginBody = await loginRes.json();
  return { userId, accessToken: loginBody.data.accessToken };
}

export async function createStudyPostViaApi(
  request: APIRequestContext,
  accessToken: string,
  input: { title: string; content: string },
): Promise<number> {
  const res = await request.post(`${API_BASE_URL}/api/study-posts`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: input,
  });
  const body = await res.json();
  return body.data.postId;
}

export async function createCommentViaApi(
  request: APIRequestContext,
  accessToken: string,
  postId: number,
  content: string,
): Promise<number> {
  const res = await request.post(`${API_BASE_URL}/api/study-posts/${postId}/comments`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: { content },
  });
  const body = await res.json();
  return body.data.commentId;
}
