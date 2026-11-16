import { STUDY_STATUS_LABEL } from '../api/studyPosts'
import type { StudyStatus } from '../api/studyPosts'

export function StatusBadge({ status }: { status: StudyStatus }) {
  const color =
    status === 'RECRUITING'
      ? 'border-green-200 bg-green-50 text-green-700'
      : 'border-gray-200 bg-gray-100 text-gray-600'
  return (
    <span
      className={`inline-block shrink-0 rounded-full border px-2 py-0.5 text-xs font-medium ${color}`}
    >
      {STUDY_STATUS_LABEL[status]}
    </span>
  )
}
