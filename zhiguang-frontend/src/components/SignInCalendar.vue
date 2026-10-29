<template>
  <div class="sign-in-calendar">
    <div class="calendar-header">
      <button class="nav-btn" @click="changeMonth(-1)">
        <span class="nav-icon">‹</span>
      </button>
      <div class="current-month">
        <span class="month-year">{{ currentYear }}年</span>
        <span class="month-value">{{ currentMonth }}月</span>
      </div>
      <button class="nav-btn" @click="changeMonth(1)">
        <span class="nav-icon">›</span>
      </button>
    </div>

    <div class="calendar-stats">
      <div class="stat-item">
        <span class="stat-value">{{ calendar.continuousDays || 0 }}</span>
        <span class="stat-label">连续签到</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ calendar.signedDays || 0 }}</span>
        <span class="stat-label">本月签到</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ calendar.supplementRemaining || 0 }}</span>
        <span class="stat-label">剩余补签</span>
      </div>
    </div>

    <div class="calendar-weekdays">
      <div v-for="day in weekDays" :key="day" class="weekday-cell">{{ day }}</div>
    </div>

    <div class="calendar-days">
      <div
        v-for="(day, index) in calendarDays"
        :key="index"
        class="day-cell"
        :class="{
          'empty': !day.date,
          'signed': day.signed,
          'supplement': day.supplement,
          'today': isToday(day.date),
          'can-sign': day.canSign,
          'can-supplement': day.canSupplement,
          'future': day.date && isFuture(day.date)
        }"
        @click="handleDayClick(day)"
      >
        <template v-if="day.date">
          <span class="day-number">{{ day.dayOfMonth }}</span>
          <span v-if="day.signed" class="day-status">
            <i class="status-icon">✓</i>
          </span>
          <span v-else-if="day.canSupplement && !day.canSign" class="day-status supplement-text">补</span>
          <span v-else-if="day.canSign" class="day-status sign-text">签</span>
        </template>
      </div>
    </div>

    <div class="calendar-legend">
      <div class="legend-item">
        <span class="legend-dot signed"></span>
        <span>已签到</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot supplement"></span>
        <span>补签</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot today"></span>
        <span>今天</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot can-supplement"></span>
        <span>可补签</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'

const props = defineProps({
  initialYear: { type: Number, default: () => new Date().getFullYear() },
  initialMonth: { type: Number, default: () => new Date().getMonth() + 1 }
})

const emit = defineEmits(['month-change', 'day-click'])

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const currentYear = ref(props.initialYear)
const currentMonth = ref(props.initialMonth)

const calendar = reactive({
  year: currentYear.value,
  month: currentMonth.value,
  days: [],
  continuousDays: 0,
  signedDays: 0,
  supplementRemaining: 0,
  signedToday: false
})

const today = computed(() => new Date())

const calendarDays = computed(() => {
  if (!calendar.days || calendar.days.length === 0) {
    return generateEmptyMonth(currentYear.value, currentMonth.value)
  }
  return calendar.days
})

function generateEmptyMonth(year, month) {
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const startPadding = firstDay.getDay()
  const daysInMonth = lastDay.getDate()
  const result = []

  for (let i = 0; i < startPadding; i++) {
    result.push({})
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const date = new Date(year, month - 1, d)
    result.push({
      date: date,
      dayOfMonth: d,
      signed: false,
      supplement: false,
      canSign: false,
      canSupplement: false
    })
  }

  return result
}

function isToday(dateStr) {
  if (!dateStr) return false
  const d = typeof dateStr === 'string' ? new Date(dateStr) : dateStr
  const now = today.value
  return d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
}

function isFuture(dateStr) {
  if (!dateStr) return false
  const d = typeof dateStr === 'string' ? new Date(dateStr) : dateStr
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  return d > now
}

function changeMonth(delta) {
  let newMonth = currentMonth.value + delta
  let newYear = currentYear.value

  if (newMonth > 12) {
    newMonth = 1
    newYear++
  } else if (newMonth < 1) {
    newMonth = 12
    newYear--
  }

  currentYear.value = newYear
  currentMonth.value = newMonth
  emit('month-change', { year: newYear, month: newMonth })
}

function handleDayClick(day) {
  if (!day.date) return
  emit('day-click', day)
}

function updateCalendar(data) {
  Object.assign(calendar, data)
  currentYear.value = data.year || currentYear.value
  currentMonth.value = data.month || currentMonth.value
}

watch(() => props.initialYear, (val) => {
  currentYear.value = val
})

watch(() => props.initialMonth, (val) => {
  currentMonth.value = val
})

defineExpose({
  updateCalendar,
  currentYear,
  currentMonth
})
</script>

<style lang="scss" scoped>
.sign-in-calendar {
  background: var(--color-card, #fff);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0,0,0,0.08));
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.nav-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--color-bg-soft, #f5f5f5);
  color: var(--color-text, #333);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover {
    background: var(--color-primary-light, rgba(26,115,232,0.1));
    color: var(--color-primary, #1a73e8);
  }
}

.nav-icon {
  font-size: 20px;
  font-weight: 500;
}

.current-month {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.month-year {
  font-size: 13px;
  color: var(--color-text-secondary, #666);
}

.month-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text, #202124);
}

.calendar-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px 0;
  margin-bottom: 16px;
  border-top: 1px solid var(--color-border, #e8eaed);
  border-bottom: 1px solid var(--color-border, #e8eaed);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary, #1a73e8);
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-secondary, #5f6368);
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin-bottom: 8px;
}

.weekday-cell {
  text-align: center;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary, #5f6368);
  padding: 8px 0;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.day-cell {
  aspect-ratio: 1;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  cursor: default;
  background: var(--color-bg-soft, #f8f9fa);
  transition: all 0.2s ease;

  &.empty {
    background: transparent;
    cursor: default;
  }

  .day-number {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text, #202124);
    z-index: 1;
  }

  .day-status {
    position: absolute;
    bottom: 4px;
    font-size: 10px;
    font-weight: 600;
    z-index: 1;
  }

  .status-icon {
    color: #fff;
    font-style: normal;
  }

  .supplement-text {
    color: #f59e0b;
  }

  .sign-text {
    color: var(--color-primary, #1a73e8);
  }

  &.today {
    box-shadow: inset 0 0 0 2px var(--color-primary, #1a73e8);
  }

  &.signed {
    background: linear-gradient(135deg, #34a853, #2e7d32);
    cursor: default;

    .day-number, .status-icon {
      color: #fff;
    }
  }

  &.supplement {
    background: linear-gradient(135deg, #f59e0b, #d97706);
    cursor: default;

    .day-number, .status-icon, .supplement-text {
      color: #fff;
    }
  }

  &.can-sign {
    cursor: pointer;
    background: var(--color-primary-light, rgba(26,115,232,0.12));

    &:hover {
      background: var(--color-primary, #1a73e8);

      .day-number, .sign-text {
        color: #fff;
      }
    }
  }

  &.can-supplement {
    cursor: pointer;
    background: rgba(245, 158, 11, 0.12);

    &:hover {
      background: #f59e0b;

      .day-number, .supplement-text {
        color: #fff;
      }
    }
  }

  &.future {
    opacity: 0.5;
    cursor: default;

    &:hover {
      background: var(--color-bg-soft, #f8f9fa);
    }
  }
}

.calendar-legend {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border, #e8eaed);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-secondary, #5f6368);
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;

  &.signed {
    background: #34a853;
  }

  &.supplement {
    background: #f59e0b;
  }

  &.today {
    border: 2px solid var(--color-primary, #1a73e8);
    background: transparent;
  }

  &.can-supplement {
    background: rgba(245, 158, 11, 0.3);
  }
}

@media (max-width: 768px) {
  .sign-in-calendar {
    padding: 16px;
  }

  .calendar-stats {
    gap: 8px;
  }

  .stat-value {
    font-size: 20px;
  }

  .month-value {
    font-size: 18px;
  }

  .calendar-weekdays,
  .calendar-days {
    gap: 4px;
  }

  .day-cell {
    border-radius: 8px;

    .day-number {
      font-size: 12px;
    }

    .day-status {
      font-size: 9px;
      bottom: 2px;
    }
  }

  .calendar-legend {
    gap: 10px;
  }
}
</style>
