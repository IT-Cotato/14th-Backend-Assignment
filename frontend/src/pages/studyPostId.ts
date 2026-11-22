// 서버 STUDY-001 메시지와 같은 문구예요. 주소의 id가 숫자가 아니면 API를 부르지 않고 이 문구를 보여줘요.
export const NOT_FOUND_MESSAGE = '존재하지 않는 스터디 모집글이에요.'

export function parsePostId(id: string | undefined): number | null {
  if (!id || !/^\d+$/.test(id)) return null
  const postId = Number(id)
  return postId >= 1 && Number.isSafeInteger(postId) ? postId : null
}
