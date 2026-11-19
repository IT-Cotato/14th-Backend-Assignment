import { STATUS_LABELS, type StudyStatus } from '../api/types'

export default function StatusBadge({ status }: { status: StudyStatus }) {
  const color = status === 'RECRUITING' ? 'bg-green-100 text-green-700' : 'bg-gray-200 text-gray-600'
  return <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${color}`}>{STATUS_LABELS[status]}</span>
}
