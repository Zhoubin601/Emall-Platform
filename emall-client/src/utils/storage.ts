export function readStoredJson<T>(key: string, fallback: T): T {
  const value = localStorage.getItem(key)
  if (!value) return fallback

  try {
    const parsed = JSON.parse(value)
    return parsed === null ? fallback : parsed as T
  } catch {
    localStorage.removeItem(key)
    return fallback
  }
}
