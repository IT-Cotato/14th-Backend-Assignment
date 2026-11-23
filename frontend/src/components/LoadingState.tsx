export default function LoadingState({ message = '불러오는 중이에요...' }: { message?: string }) {
    return (
        <div className="flex flex-col items-center justify-center gap-3 py-16 text-slate-500" role="status">
            <span className="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-blue-600" />
            <p className="text-sm">{message}</p>
        </div>
    )
}
