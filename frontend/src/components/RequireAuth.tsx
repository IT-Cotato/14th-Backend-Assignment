import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, initializing } = useAuth()

  if (initializing) {
    return <p className="py-10 text-center text-gray-500">불러오는 중...</p>
  }
  if (!user) {
    return <Navigate to="/login" replace />
  }
  return children
}
