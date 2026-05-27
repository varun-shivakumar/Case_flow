import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'

/**
 * Language is no longer a hardcoded translation map. We now:
 *   1. Default to English (no translation API call).
 *   2. For any other language, lazily call a free translation API
 *      (MyMemory — https://mymemory.translated.net) per phrase.
 *   3. Cache every translation in localStorage so we never re-fetch the
 *      same phrase, and the UI updates the moment a fetch resolves.
 *   4. If the API ever fails, t(text) falls back to the original English.
 *
 * Add a new language to SUPPORTED_LANGUAGES below — no other code changes
 * are required, the API handles the actual translation.
 */

export const SUPPORTED_LANGUAGES = [
  { id: 'english',   label: 'English',     code: 'en' },
  { id: 'hindi',     label: 'हिन्दी',         code: 'hi' },
  { id: 'bengali',   label: 'বাংলা',         code: 'bn' },
  { id: 'tamil',     label: 'தமிழ்',         code: 'ta' },
  { id: 'telugu',    label: 'తెలుగు',        code: 'te' },
  { id: 'marathi',   label: 'मराठी',          code: 'mr' },
  { id: 'kannada',   label: 'ಕನ್ನಡ',           code: 'kn' },
  { id: 'gujarati',  label: 'ગુજરાતી',         code: 'gu' },
  { id: 'malayalam', label: 'മലയാളം',        code: 'ml' },
  { id: 'punjabi',   label: 'ਪੰਜਾਬੀ',          code: 'pa' },
]

const LS_KEY = 'caseflow-i18n-cache-v1'

// Load cache from localStorage on module load.
function loadCache() {
  try { return JSON.parse(localStorage.getItem(LS_KEY)) || {} } catch { return {} }
}
function saveCache(cache) {
  try { localStorage.setItem(LS_KEY, JSON.stringify(cache)) } catch { /* quota — ignore */ }
}

const LanguageContext = createContext(null)

export function LanguageProvider({ children }) {
  const [language, setLanguage] = useState(() => localStorage.getItem('caseflow-language') || 'english')
  // cache: { [langCode]: { [phrase]: translation } }
  const cacheRef = useRef(loadCache())
  // pendingRef: { [langCode]: Set<phrase> } — phrases currently being fetched
  const pendingRef = useRef({})
  // Bump this counter to force a re-render whenever new translations land.
  const [bump, setBump] = useState(0)

  useEffect(() => { localStorage.setItem('caseflow-language', language) }, [language])

  const langCode = (SUPPORTED_LANGUAGES.find(l => l.id === language)?.code) || 'en'

  const fetchTranslation = useCallback(async (text, code) => {
    if (!text || !code || code === 'en') return text
    pendingRef.current[code] = pendingRef.current[code] || new Set()
    if (pendingRef.current[code].has(text)) return null
    pendingRef.current[code].add(text)

    try {
      // MyMemory free translation endpoint — no API key required, GET request.
      const url = `https://api.mymemory.translated.net/get?q=${encodeURIComponent(text)}&langpair=en|${code}`
      const res = await fetch(url)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const data = await res.json()
      const translated = data?.responseData?.translatedText
      if (typeof translated === 'string' && translated.length > 0) {
        cacheRef.current[code] = cacheRef.current[code] || {}
        cacheRef.current[code][text] = translated
        saveCache(cacheRef.current)
        setBump(b => b + 1)
        return translated
      }
    } catch (e) {
      // Silent — fall through to returning English. Cache the failure so we don't
      // keep retrying every render.
      cacheRef.current[code] = cacheRef.current[code] || {}
      cacheRef.current[code][text] = text   // poison-pill: fall back to English
      saveCache(cacheRef.current)
    } finally {
      pendingRef.current[code].delete(text)
    }
    return null
  }, [])

  /** t(text) — synchronous. Returns the cached translation or the original
   *  English text. If a translation isn't cached yet we kick off an async
   *  fetch and the component will re-render once it lands. */
  const t = useCallback((text) => {
    if (!text) return text
    if (langCode === 'en') return text
    const langCache = cacheRef.current[langCode]
    if (langCache && Object.prototype.hasOwnProperty.call(langCache, text)) {
      return langCache[text]
    }
    // Fire-and-forget; the bump counter triggers a re-render when it resolves.
    fetchTranslation(text, langCode)
    return text
  }, [langCode, fetchTranslation, bump]) // eslint-disable-line react-hooks/exhaustive-deps

  /** Pre-warm a list of phrases — useful at app startup to translate a screen
   *  in one batch instead of one HTTP call per <Trans> element. */
  const preload = useCallback((phrases) => {
    if (langCode === 'en' || !phrases) return
    const langCache = cacheRef.current[langCode] || {}
    phrases.forEach(p => {
      if (p && !Object.prototype.hasOwnProperty.call(langCache, p)) {
        fetchTranslation(p, langCode)
      }
    })
  }, [langCode, fetchTranslation])

  /** Manually clear the translation cache — exposed in case the user wants
   *  to force a refresh after fixing a bad translation upstream. */
  const clearCache = useCallback(() => {
    cacheRef.current = {}
    try { localStorage.removeItem(LS_KEY) } catch { /* ignore */ }
    setBump(b => b + 1)
  }, [])

  return (
    <LanguageContext.Provider
      value={{ language, setLanguage, t, preload, clearCache, supportedLanguages: SUPPORTED_LANGUAGES }}
    >
      {children}
    </LanguageContext.Provider>
  )
}

export function useLanguage() {
  const ctx = useContext(LanguageContext)
  if (!ctx) throw new Error('useLanguage must be used within LanguageProvider')
  return ctx
}
