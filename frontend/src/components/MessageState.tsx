import type { ReactNode } from 'react'

interface MessageStateProps {
    title: string
    description?: string
    tone?: 'neutral' | 'error'
    action?: ReactNode
}

// 빈 목록, 오류, 403/404 안내 화면에 함께 써요.
export default function MessageState({
    title,
    description,
    tone = 'neutral',
    action,
}: MessageStateProps) {
    return (
        <div
            className="flex flex-col items-center gap-3 rounded-xl bg-white px-6 py-14 text-center shadow-sm"
            role={tone === 'error' ? 'alert' : undefined}
        >
            <p className={`text-lg font-semibold ${tone === 'error' ? 'text-red-600' : 'text-slate-800'}`}>
                {title}
            </p>
            {description && <p className="text-sm text-slate-500">{description}</p>}
            {action && <div className="mt-2">{action}</div>}
        </div>
    )
}
