import { useNavigate } from 'react-router-dom'
import { createStudyPost } from '../api/studyPosts'
import { StudyFormLayout, StudyPostForm, type StudyPostFormValues } from '../components/StudyPostForm'

const EMPTY_VALUES: StudyPostFormValues = { title: '', content: '', studyStatus: 'RECRUITING' }

export function StudyCreatePage() {
  const navigate = useNavigate()

  const handleSubmit = async ({ title, content }: StudyPostFormValues) => {
    const { postId } = await createStudyPost({ title, content })
    navigate(`/studies/${postId}`, { replace: true })
  }

  return (
    <StudyFormLayout title="모집글 작성">
      <StudyPostForm
        initialValues={EMPTY_VALUES}
        showStatus={false}
        submitLabel="등록"
        loadingText="등록 중..."
        cancelTo="/studies"
        onSubmit={handleSubmit}
      />
    </StudyFormLayout>
  )
}
