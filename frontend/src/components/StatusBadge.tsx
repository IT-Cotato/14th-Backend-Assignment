import { STATUS_LABELS, type StudyStatus } from '../api/types.ts'

export default function StatusBadge({ status }: { status: StudyStatus }) {
  const style = status === 'RECRUITING' ? 'bg-green-100 text-green-700' : 'bg-gray-200 text-gray-600'
  return <span className={`rounded px-2 py-0.5 text-xs font-medium ${style}`}>{STATUS_LABELS[status]}</span>
}
