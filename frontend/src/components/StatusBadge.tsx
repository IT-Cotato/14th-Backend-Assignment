import { STUDY_STATUS_LABELS, type StudyStatus } from '../types/studyPost'

export default function StatusBadge({ status }: { status: StudyStatus }) {
    const style =
        status === 'RECRUITING'
            ? 'bg-blue-50 text-blue-700 ring-blue-200'
            : 'bg-slate-100 text-slate-500 ring-slate-200'
    return (
        <span
            className={`inline-flex shrink-0 items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ${style}`}
        >
            {STUDY_STATUS_LABELS[status]}
        </span>
    )
}
