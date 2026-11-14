import { PARTS, type Part, type SignupRequest } from '../api/types'

// 입력창 값은 모두 문자열로 다룹니다. 기수는 제출할 때 숫자로 바꿉니다.
export interface SignupForm {
  email: string
  password: string
  name: string
  nickname: string
  generation: string
  part: Part | ''
}

export type SignupErrors = Partial<Record<keyof SignupForm, string>>

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// 검증 규칙은 docs/api-spec.md 1번 회원가입과 같습니다. 이름은 명세에 규칙이 없어 필수만 확인합니다.
export function validateSignup(form: SignupForm): SignupErrors {
  const errors: SignupErrors = {}

  if (!form.email.trim()) {
    errors.email = '이메일을 입력해 주세요.'
  } else if (!EMAIL_PATTERN.test(form.email.trim())) {
    errors.email = '이메일 형식이 올바르지 않아요.'
  }

  if (!form.password) {
    errors.password = '비밀번호를 입력해 주세요.'
  } else if (form.password.length < 8) {
    errors.password = '비밀번호는 8자 이상이어야 해요.'
  }

  if (!form.name.trim()) {
    errors.name = '이름을 입력해 주세요.'
  }

  const nickname = form.nickname.trim()
  if (!nickname) {
    errors.nickname = '닉네임을 입력해 주세요.'
  } else if (nickname.length < 2 || nickname.length > 10) {
    errors.nickname = '닉네임은 2~10자여야 해요.'
  }

  const generation = Number(form.generation)
  if (!form.generation.trim()) {
    errors.generation = '기수를 입력해 주세요.'
  } else if (!Number.isInteger(generation) || generation < 1) {
    errors.generation = '기수는 1 이상의 정수여야 해요.'
  }

  if (!PARTS.includes(form.part as Part)) {
    errors.part = '파트를 선택해 주세요.'
  }

  return errors
}

// 검증을 통과한 폼을 API 요청 형식으로 바꿉니다.
export function toSignupRequest(form: SignupForm): SignupRequest {
  return {
    email: form.email.trim(),
    password: form.password,
    name: form.name.trim(),
    nickname: form.nickname.trim(),
    generation: Number(form.generation),
    part: form.part as Part,
  }
}
