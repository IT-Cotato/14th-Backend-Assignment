import { useState } from 'react'
import { useNavigate } from 'react-router'
import { createStudyPost } from '../api/studyPosts'
import { toApiError } from '../api/errors'
import StudyPostForm, { type StudyPostFormValues } from '../components/StudyPostForm'

const INITIAL_VALUES: StudyPostFormValues = { title: '', content: '', studyStatus: 'RECRUITING' }

export default function StudyCreatePage() {
    const navigate = useNavigate()
    const [submitting, setSubmitting] = useState(false)
    const [errorMessage, setErrorMessage] = useState<string | null>(null)

    const handleSubmit = async ({ title, content }: StudyPostFormValues) => {
        setSubmitting(true)
        setErrorMessage(null)
        try {
            const { postId } = await createStudyPost({ title, content })
            navigate(`/studies/${postId}`, { replace: true })
        } catch (error) {
            setErrorMessage(toApiError(error).message)
            setSubmitting(false)
        }
    }

    return (
        <div className="mx-auto w-full max-w-3xl rounded-xl bg-white p-5 shadow-sm sm:p-8">
            <h2 className="text-2xl font-bold text-slate-900">스터디 모집글 작성</h2>
            <p className="mt-1 mb-6 text-sm text-slate-500">작성한 글은 모집 중 상태로 등록돼요.</p>
            <StudyPostForm
                initialValues={INITIAL_VALUES}
                submitLabel="등록"
                submitting={submitting}
                errorMessage={errorMessage}
                onSubmit={handleSubmit}
                onCancel={() => navigate('/studies')}
            />
        </div>
    )
}
