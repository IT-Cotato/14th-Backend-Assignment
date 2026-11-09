// URL의 :id를 양의 정수로 바꿔요. 잘못된 값이면 null이에요.
export function parsePostId(value: string | undefined): number | null {
    if (!value || !/^\d+$/.test(value)) return null
    const id = Number(value)
    return Number.isSafeInteger(id) && id > 0 ? id : null
}
