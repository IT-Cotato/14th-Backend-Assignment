import { BrowserRouter, Route, Routes } from 'react-router-dom'

function Home() {
  return (
    <main className="mx-auto max-w-3xl p-8">
      <h1 className="text-2xl font-bold">Cotato 스터디 매칭</h1>
      <p className="mt-2 text-gray-600">프론트엔드 초기 세팅 완료</p>
    </main>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
      </Routes>
    </BrowserRouter>
  )
}
