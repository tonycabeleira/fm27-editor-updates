package com.cabincrew.academy.domain

import com.cabincrew.academy.data.db.DailyActivityEntity
import java.time.LocalDate

data class StreakInfo(val current: Int, val best: Int, val studiedToday: Boolean)

object Streaks {
    /** A day counts towards the streak when at least one question was answered. */
    fun compute(days: List<DailyActivityEntity>, today: Long = LocalDate.now().toEpochDay()): StreakInfo {
        val active = days.filter { it.answered > 0 }.map { it.epochDay }.toSortedSet()
        val studiedToday = today in active
        var current = 0
        var d = if (studiedToday) today else today - 1
        while (d in active) { current++; d-- }
        var best = 0
        var run = 0
        var prev: Long? = null
        for (day in active) {
            run = if (prev != null && day == prev + 1) run + 1 else 1
            best = maxOf(best, run)
            prev = day
        }
        return StreakInfo(current, maxOf(best, current), studiedToday)
    }
}
