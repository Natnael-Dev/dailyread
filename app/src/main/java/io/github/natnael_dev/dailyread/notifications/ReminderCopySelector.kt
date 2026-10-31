package io.github.natnael_dev.dailyread.notifications

enum class BanditTone {
    LOSS_AVERSION,
    CONTENT_CURIOSITY,
    PASSIVE_AGGRESSIVE,
    URGENCY_ESCALATION,
    SURRENDER
}

data class ReminderCopy(
    val title: String,
    val message: String,
    val tone: BanditTone,
    val isUrgent: Boolean = false
)

/**
 * Psychological hook selection engine adhering to DEC-045 and behavioral reverse-engineering.
 * Selects notification copy framed around loss aversion, urgency escalation, curiosity,
 * passive-aggressive nudges, or surrender hooks after prolonged habit abandonment.
 */
class ReminderCopySelector {

    fun selectReminderCopy(
        streakDays: Int,
        pagesReadToday: Int,
        dailyGoalPages: Int,
        activeBookTitle: String? = null,
        consecutiveInactiveDays: Int = 0,
        hourOfDay: Int = 20,
        variant: Int = 0,
        forcedTone: BanditTone? = null
    ): ReminderCopy? {
        // Rule 1: Goal satisfaction suppresses notification completely
        if (pagesReadToday >= dailyGoalPages) {
            return null
        }

        val remainingPages = (dailyGoalPages - pagesReadToday).coerceAtLeast(1)

        // Rule 2: Tone determination
        val tone = when {
            forcedTone != null -> forcedTone
            consecutiveInactiveDays >= 3 -> BanditTone.SURRENDER
            isUrgentWindow(hourOfDay) && streakDays > 0 -> BanditTone.URGENCY_ESCALATION
            else -> {
                // Rotate among 3 standard bandit tones
                when (Math.floorMod(variant, 3)) {
                    0 -> BanditTone.LOSS_AVERSION
                    1 -> BanditTone.CONTENT_CURIOSITY
                    else -> BanditTone.PASSIVE_AGGRESSIVE
                }
            }
        }

        return when (tone) {
            BanditTone.SURRENDER -> ReminderCopy(
                title = "We're stepping back",
                message = "DailyRead will stop reminding you. Open the app whenever you want to rebuild your reading habit.",
                tone = BanditTone.SURRENDER,
                isUrgent = false
            )
            BanditTone.URGENCY_ESCALATION -> ReminderCopy(
                title = "⚠️ Streak expiring at 03:00!",
                message = "Your $streakDays-day streak expires at 03:00! Read $remainingPages pages now to save it.",
                tone = BanditTone.URGENCY_ESCALATION,
                isUrgent = true
            )
            BanditTone.LOSS_AVERSION -> {
                if (streakDays > 0) {
                    ReminderCopy(
                        title = "Keep your habit alive",
                        message = "Don't lose your $streakDays-day streak! Read $remainingPages pages before 03:00 rollover.",
                        tone = BanditTone.LOSS_AVERSION
                    )
                } else {
                    ReminderCopy(
                        title = "Build your daily streak",
                        message = "Start your reading streak today! Just $remainingPages pages to establish your habit.",
                        tone = BanditTone.LOSS_AVERSION
                    )
                }
            }
            BanditTone.CONTENT_CURIOSITY -> {
                if (!activeBookTitle.isNullOrBlank()) {
                    ReminderCopy(
                        title = "Continue your story",
                        message = "Pick up where you left off in '$activeBookTitle'. Just $remainingPages pages to reach today's goal.",
                        tone = BanditTone.CONTENT_CURIOSITY
                    )
                } else {
                    ReminderCopy(
                        title = "Your books are waiting",
                        message = "Pick up where you left off. Just $remainingPages pages to hit today's reading goal.",
                        tone = BanditTone.CONTENT_CURIOSITY
                    )
                }
            }
            BanditTone.PASSIVE_AGGRESSIVE -> ReminderCopy(
                title = "Still reading?",
                message = "These reminders don't seem to be working. Open DailyRead whenever you're ready to read.",
                tone = BanditTone.PASSIVE_AGGRESSIVE
            )
        }
    }

    private fun isUrgentWindow(hourOfDay: Int): Boolean {
        // 23:00 (11 PM) to 02:59 (3 AM logical rollover)
        return hourOfDay == 23 || hourOfDay in 0..2
    }
}
