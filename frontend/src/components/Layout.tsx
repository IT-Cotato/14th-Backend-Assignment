import { Outlet } from 'react-router'
import { Header } from './Header'

export function Layout() {
  return (
    <>
      <Header />
      <main className="mx-auto max-w-3xl px-4 py-8">
        <Outlet />
      </main>
    </>
  )
}
