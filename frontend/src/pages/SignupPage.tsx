import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";

import { extractErrorMessage } from "../api/client";
import { signup } from "../api/auth";
import type { Part } from "../api/types";
import { PART_OPTIONS } from "../constants/part";

export function SignupPage() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [nickname, setNickname] = useState("");
  const [generation, setGeneration] = useState("");
  const [part, setPart] = useState<Part>("BE");

  const [validationMessage, setValidationMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setValidationMessage(null);

    if (password.length < 8) {
      setValidationMessage("비밀번호는 8자 이상이어야 해요.");
      return;
    }
    if (nickname.length < 2 || nickname.length > 10) {
      setValidationMessage("닉네임은 2~10자여야 해요.");
      return;
    }
    const generationNumber = Number(generation);
    if (!Number.isInteger(generationNumber) || generationNumber < 1) {
      setValidationMessage("기수는 1 이상의 정수여야 해요.");
      return;
    }

    setIsLoading(true);
    try {
      await signup({ email, password, name, nickname, generation: generationNumber, part });
      navigate("/login");
    } catch (error) {
      setErrorMessage(extractErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <section className="mx-auto flex max-w-md flex-col gap-4 p-4">
      <h1 className="text-xl font-semibold">회원가입</h1>
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
        <div className="flex flex-col gap-1">
          <label htmlFor="name">이름</label>
          <input id="name" type="text" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="nickname">닉네임</label>
          <input
            id="nickname"
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            required
          />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="generation">기수</label>
          <input
            id="generation"
            type="number"
            value={generation}
            onChange={(e) => setGeneration(e.target.value)}
            required
          />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="part">파트</label>
          <select id="part" value={part} onChange={(e) => setPart(e.target.value as Part)}>
            {PART_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        {validationMessage && <p role="alert">{validationMessage}</p>}
        {errorMessage && <p role="alert">{errorMessage}</p>}
        {isLoading && <p>가입하는 중이에요...</p>}

        <button type="submit" disabled={isLoading}>
          회원가입
        </button>
      </form>
      <Link to="/login">이미 계정이 있나요? 로그인</Link>
    </section>
  );
}
