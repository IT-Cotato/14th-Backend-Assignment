import { Link, useNavigate } from "react-router-dom";

import { useAuth } from "../context/AuthContext";

export function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login");
  }

  return (
    <header className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
      <Link to="/mypage" className="font-semibold">
        코테이토 스터디 매칭
      </Link>
      <nav className="flex items-center gap-4">
        {user ? (
          <>
            <span>{user.nickname}님</span>
            <Link to="/mypage">마이페이지</Link>
            <button type="button" onClick={handleLogout}>
              로그아웃
            </button>
          </>
        ) : (
          <>
            <Link to="/login">로그인</Link>
            <Link to="/signup">회원가입</Link>
          </>
        )}
      </nav>
    </header>
  );
}
