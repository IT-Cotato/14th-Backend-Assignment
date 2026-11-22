import type { ReactNode } from 'react'

// 로그인·회원가입 폼을 감싸는 카드. 모바일에서는 화면 폭에 맞고, 넓은 화면에서는 가운데 정렬돼요.
export function AuthCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mx-auto w-full max-w-md rounded-xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <h1 className="mb-6 text-2xl font-bold text-slate-900">{title}</h1>
      {children}
    </section>
  )
}
