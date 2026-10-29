// 树洞世界（深色夜话）专用视觉常量，避免污染站点浅色主题的情绪配色
export const NIGHT_EMOTION_COLORS = {
  all: '#9fb3d9',
  happiness: '#fbbf24',
  sadness: '#60a5fa',
  anxiety: '#22d3ee',
  excitement: '#f472b6',
  loneliness: '#a5b4fc',
  anger: '#fb7185',
  gratitude: '#34d399',
  other: '#94a3b8'
}

export const NIGHT_EMOTION_ORDER = [
  'happiness',
  'excitement',
  'gratitude',
  'other',
  'loneliness',
  'sadness',
  'anxiety',
  'anger'
]

const stroke = (path, size = 16) =>
  `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${path}</svg>`

export const NIGHT_ICONS = {
  moon: stroke('<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>', 15),
  write: stroke('<path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>', 16),
  info: stroke('<circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>', 16),
  back: stroke('<line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/>', 18),
  close: stroke('<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>', 18),
  star: stroke('<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>', 15),
  timer: stroke('<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>', 15),
  heart: stroke('<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>', 15),
  hug: stroke('<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><path d="M20 8v10"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/>', 15),
  comment: stroke('<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>', 15),
  lock: stroke('<rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>', 14),
  chevronDown: stroke('<polyline points="6 9 12 15 18 9"/>', 16),
  send: stroke('<line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>', 15),
  flag: stroke('<path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/>', 14),
  trash: stroke('<polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>', 14),
  shield: stroke('<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>', 16),
  incognito: stroke('<path d="M12 2a10 10 0 1 0 10 10 4 4 0 0 1-5-5 4 4 0 0 1-5 5c0-3.417 2.167-6.417 5-8.417C12.5 6.083 13 5.083 12 2z"/>', 16),
  clock: stroke('<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>', 16),
  warmth: stroke('<path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>', 16),
  letter: stroke('<path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22 6 12 13 2 6"/>', 56),
  cloudMoon: stroke('<path d="M20.354 15.354A9 9 0 0 1 8.646 3.646 9.003 9.003 0 0 0 12 21a9.003 9.003 0 0 0 8.354-5.646z"/><path d="M16 16a5 5 0 0 1-4-8"/>', 56),
  retry: stroke('<polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>', 15)
}

export const NIGHT_EMOTION_ICONS = {
  all: stroke('<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="2.6" fill="currentColor"/><circle cx="12" cy="5.6" r="1.3" fill="currentColor"/><circle cx="17.2" cy="14.4" r="1.3" fill="currentColor"/><circle cx="6.8" cy="14.4" r="1.3" fill="currentColor"/>'),
  happiness: stroke('<circle cx="12" cy="12" r="9"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/>'),
  sadness: stroke('<circle cx="12" cy="12" r="9"/><path d="M8 15s1.5-1 4-1 4 1 4 1"/><path d="M9 10l-1 3"/><path d="M15 10l1 3"/>'),
  anxiety: stroke('<path d="M2 12h4l3-8 4 16 3-8h4"/>'),
  excitement: stroke('<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>'),
  loneliness: stroke('<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>'),
  anger: stroke('<path d="M12 2c0 0-7 4-7 11 0 3.86 3.14 7 7 7s7-3.14 7-7c0-7-7-11-7-11z"/><path d="M12 12c0 0-3 2-3 5 0 1.66 1.34 3 3 3s3-1.34 3-3c0-3-3-5-3-5z"/>'),
  gratitude: stroke('<path d="M20.42 4.58a5.4 5.4 0 0 0-7.65 0L12 5.35l-.77-.77a5.4 5.4 0 0 0-7.65 7.65l.77.77L12 21l7.65-7.65.77-.77a5.4 5.4 0 0 0 0-7.65z"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/>'),
  other: stroke('<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>')
}

export const nightColor = (key) => NIGHT_EMOTION_COLORS[key] || NIGHT_EMOTION_COLORS.other

export const emotionIcon = (key) => NIGHT_EMOTION_ICONS[key] || NIGHT_EMOTION_ICONS.other
