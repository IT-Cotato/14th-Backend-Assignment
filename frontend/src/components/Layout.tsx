import { Outlet } from 'react-router'
import { Header } from './Header'

export function Layout() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <main className="mx-auto w-full max-w-3xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
