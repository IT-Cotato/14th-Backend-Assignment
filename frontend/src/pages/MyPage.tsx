import { useEffect, useState } from 'react'
import { useLocation } from 'react-router'
import { getMe } from '../api/auth'
import { toApiError, type ApiError } from '../api/errors'
import { LOGOUT_UNCONFIRMED_MESSAGE } from '../auth/AuthContext'
import { useLogout } from '../auth/useLogout'
import { dangerButtonClass, secondaryButtonClass } from '../components/FormField'
import LoadingState from '../components/LoadingState'
import MessageState from '../components/MessageState'
import { PART_LABELS } from '../types/api'
import type { User } from '../types/auth'

type MeState =
    | { kind: 'loading' }
    | { kind: 'error'; error: ApiError }
    | { kind: 'success'; me: User }

interface MyPageLocationState {
    // 헤더에서 로그아웃을 확인하지 못하고 이동해 온 경우예요.
    logoutUnconfirmed?: boolean
}

function InfoRow({ label, value }: { label: string; value: string }) {
    return (
        <div className="flex flex-col gap-1 py-3 sm:flex-row sm:items-center sm:gap-4">
            <dt className="w-20 shrink-0 text-sm text-slate-500">{label}</dt>
            <dd className="break-all font-medium text-slate-900">{value}</dd>
        </div>
    )
}

// RequireAuth로 감싸서 로그인한 사용자만 들어와요.
export default function MyPage() {
    const location = useLocation()
    const fromHeader = ((location.state ?? {}) as MyPageLocationState).logoutUnconfirmed === true

    const [result, setResult] = useState<({ key: number } & MeState) | null>(null)
    const [reloadKey, setReloadKey] = useState(0)
    const [failedHere, setFailedHere] = useState(false)
    // 마이페이지에 있는 상태에서 헤더 로그아웃이 실패해도 location.key가 바뀌어서 다시 보여줘요.
    const [dismissedKey, setDismissedKey] = useState<string | null>(null)
    const unconfirmed = failedHere || (fromHeader && dismissedKey !== location.key)
    const { loggingOut, run, runLocally } = useLogout(() => setFailedHere(true))

    const state: MeState = result && result.key === reloadKey ? result : { kind: 'loading' }

    // 화면에 들어올 때마다 GET /api/users/me로 최신 정보를 불러와요.
    useEffect(() => {
        let ignore = false
        getMe()
            .then((me) => {
                if (!ignore) setResult({ key: reloadKey, kind: 'success', me })
            })
            .catch((error) => {
                // 401은 인터셉터가 로그인 화면으로 보내요.
                if (!ignore) setResult({ key: reloadKey, kind: 'error', error: toApiError(error) })
            })
        return () => {
            ignore = true
        }
    }, [reloadKey])

    const handleLogout = () => {
        setFailedHere(false)
        setDismissedKey(location.key)
        void run()
    }

    const logoutPanel = (
        <section className="mt-4 rounded-xl bg-white p-5 shadow-sm sm:p-8" aria-labelledby="logout-title">
            <h3 id="logout-title" className="font-semibold text-slate-900">
                로그아웃
            </h3>
            <p className="mt-1 text-sm text-slate-500">
                로그아웃하면 서버에서 현재 토큰을 무효화하고 로그인 화면으로 이동해요.
            </p>

            {unconfirmed && (
                <div className="mt-4 flex flex-col gap-3 rounded-lg bg-amber-50 px-4 py-3" role="alert">
                    <p className="text-sm text-amber-800">{LOGOUT_UNCONFIRMED_MESSAGE}</p>
                    <div className="flex flex-col gap-2 sm:flex-row">
                        <button
                            type="button"
                            className={secondaryButtonClass}
                            onClick={handleLogout}
                            disabled={loggingOut}
                        >
                            {loggingOut ? '다시 시도 중...' : '로그아웃 다시 시도'}
                        </button>
                        <button
                            type="button"
                            className={dangerButtonClass}
                            onClick={runLocally}
                            disabled={loggingOut}
                        >
                            이 기기에서만 로그아웃
                        </button>
                    </div>
                </div>
            )}

            {!unconfirmed && (
                <div className="mt-4 flex sm:justify-end">
                    <button
                        type="button"
                        className={`${dangerButtonClass} w-full sm:w-auto`}
                        onClick={handleLogout}
                        disabled={loggingOut}
                    >
                        {loggingOut ? '로그아웃 중...' : '로그아웃'}
                    </button>
                </div>
            )}
        </section>
    )

    return (
        <div className="mx-auto w-full max-w-2xl">
            <h2 className="text-2xl font-bold text-slate-900">마이페이지</h2>

            <div className="mt-4">
                {state.kind === 'loading' && <LoadingState message="내 정보를 불러오고 있어요..." />}

                {state.kind === 'error' && (
                    <MessageState
                        tone="error"
                        title="내 정보를 불러오지 못했어요."
                        description={state.error.message}
                        action={
                            <button
                                type="button"
                                className={secondaryButtonClass}
                                onClick={() => setReloadKey((k) => k + 1)}
                            >
                                다시 시도
                            </button>
                        }
                    />
                )}

                {state.kind === 'success' && (
                    <section className="rounded-xl bg-white p-5 shadow-sm sm:p-8" aria-labelledby="profile-title">
                        <div className="flex items-center gap-4 border-b border-slate-100 pb-4">
                            <span
                                className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-blue-100 text-xl font-bold text-blue-700"
                                aria-hidden="true"
                            >
                                {state.me.nickname.charAt(0)}
                            </span>
                            <div className="min-w-0">
                                <h3 id="profile-title" className="truncate text-lg font-semibold text-slate-900">
                                    {state.me.nickname}
                                </h3>
                                <p className="text-sm text-slate-500">
                                    코테이토 {state.me.generation}기 · {PART_LABELS[state.me.part]}
                                </p>
                            </div>
                        </div>
                        <dl className="divide-y divide-slate-100" data-testid="my-info">
                            <InfoRow label="이름" value={state.me.name} />
                            <InfoRow label="이메일" value={state.me.email} />
                            <InfoRow label="닉네임" value={state.me.nickname} />
                            <InfoRow label="기수" value={`${state.me.generation}기`} />
                            <InfoRow label="파트" value={PART_LABELS[state.me.part]} />
                        </dl>
                    </section>
                )}
            </div>

            {logoutPanel}
        </div>
    )
}
