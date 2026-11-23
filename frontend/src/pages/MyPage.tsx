import { useAuth } from "../context/AuthContext";

export function MyPage() {
  const { user } = useAuth();

  if (!user) {
    return <p>로딩 중이에요...</p>;
  }

  return (
    <section className="mx-auto flex max-w-md flex-col gap-3 p-4">
      <h1 className="text-xl font-semibold">마이페이지</h1>
      <dl className="flex flex-col gap-2">
        <div>
          <dt className="font-medium">이메일</dt>
          <dd>{user.email}</dd>
        </div>
        <div>
          <dt className="font-medium">이름</dt>
          <dd>{user.name}</dd>
        </div>
        <div>
          <dt className="font-medium">닉네임</dt>
          <dd>{user.nickname}</dd>
        </div>
        <div>
          <dt className="font-medium">기수</dt>
          <dd>{user.generation}</dd>
        </div>
        <div>
          <dt className="font-medium">파트</dt>
          <dd>{user.part}</dd>
        </div>
      </dl>
    </section>
  );
}
