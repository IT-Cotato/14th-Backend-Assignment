import axios, { AxiosHeaders, type AxiosError } from "axios";
import { clearToken, emitAuthExpired, getToken } from "./tokenStorage";

declare module "axios" {
  interface AxiosRequestConfig {
    // true면 Authorization 헤더를 붙이지 않아요. (로그인, 회원가입)
    skipAuth?: boolean;
    // 401 뒤에 토큰 없이 한 번 다시 보낸 요청인지 표시해요.
    retriedWithoutAuth?: boolean;
    // true면 401이 와도 인터셉터가 토큰 삭제·로그인 이동을 하지 않아요. (호출한 쪽이 직접 처리해요)
    skipAuthExpiredHandling?: boolean;
  }
}

const api = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// 저장된 JWT를 Authorization 헤더에 자동으로 붙여요.
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token && !config.skipAuth) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

// 401이 오면 토큰을 지우고 로그인을 유도해요.
// 공개 GET 요청은 만료된 토큰 때문에 막히지 않도록 토큰 없이 한 번 다시 보내요.
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config;
    if (
      error.response?.status !== 401 ||
      !config ||
      config.skipAuth ||
      config.skipAuthExpiredHandling
    ) {
      return Promise.reject(error);
    }

    const hadToken = AxiosHeaders.from(config.headers).has("Authorization");
    clearToken();

    if (hadToken && config.method === "get" && !config.retriedWithoutAuth) {
      emitAuthExpired(false);
      const headers = AxiosHeaders.from(config.headers);
      headers.delete("Authorization");
      return api.request({
        ...config,
        headers,
        skipAuth: true,
        retriedWithoutAuth: true,
      });
    }

    emitAuthExpired(true);
    return Promise.reject(error);
  },
);

export default api;
