import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";

import { extractErrorMessage } from "../api/client";
import { login as loginApi } from "../api/auth";
import { useAuth } from "../context/AuthContext";

export function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setIsLoading(true);

    try {
      const { accessToken } = await loginApi({ email, password });
      await login(accessToken);
      navigate("/studies");
    } catch (error) {
      setErrorMessage(extractErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <section className="mx-auto flex max-w-md flex-col gap-4 p-4">
      <h1 className="text-xl font-semibold">로그인</h1>
      <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
        <div className="flex flex-col gap-1">
          <label htmlFor="email">이메일</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="password">비밀번호</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        {errorMessage && <p role="alert">{errorMessage}</p>}
        {isLoading && <p>로그인하는 중이에요...</p>}

        <button type="submit" disabled={isLoading}>
          로그인
        </button>
      </form>
      <Link to="/signup">계정이 없나요? 회원가입</Link>
    </section>
  );
}
