import { STUDY_STATUS_LABELS, type StudyStatus } from '../api/types'

export function StatusBadge({ status }: { status: StudyStatus }) {
  const colorClassName = status === 'RECRUITING' ? 'bg-green-100 text-green-800' : 'bg-gray-200 text-gray-700'
  return (
    <span className={`inline-block shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${colorClassName}`}>
      {STUDY_STATUS_LABELS[status]}
    </span>
  )
}
