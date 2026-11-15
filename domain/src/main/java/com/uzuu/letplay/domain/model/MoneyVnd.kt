package com.uzuu.letplay.domain.model

/**
 * Value class representing an amount in Vietnamese Dong (VND).
 * Strictly non-negative, with overflow protection using Math.*Exact operations.
 */
@JvmInline
value class MoneyVnd(val amount: Long) : Comparable<MoneyVnd> {
    init {
        require(amount >= 0) { "MoneyVnd amount must be non-negative: $amount" }
    }

    operator fun plus(other: MoneyVnd): MoneyVnd {
        val sum = Math.addExact(this.amount, other.amount)
        return MoneyVnd(sum)
    }

    operator fun minus(other: MoneyVnd): MoneyVnd {
        require(this.amount >= other.amount) {
            "Subtraction would result in negative MoneyVnd: ${this.amount} - ${other.amount}"
        }
        val diff = Math.subtractExact(this.amount, other.amount)
        return MoneyVnd(diff)
    }

    operator fun times(factor: Long): MoneyVnd {
        require(factor >= 0) { "Multiplier must be non-negative: $factor" }
        val prod = Math.multiplyExact(this.amount, factor)
        return MoneyVnd(prod)
    }

    operator fun div(divisor: Long): MoneyVnd {
        require(divisor > 0) { "Divisor must be positive: $divisor" }
        return MoneyVnd(this.amount / divisor)
    }

    operator fun rem(divisor: Long): MoneyVnd {
        require(divisor > 0) { "Divisor must be positive: $divisor" }
        return MoneyVnd(this.amount % divisor)
    }

    override fun compareTo(other: MoneyVnd): Int = this.amount.compareTo(other.amount)

    fun formatVnd(): String = formatVndAmount(amount)

    override fun toString(): String = formatVnd()

    companion object {
        val ZERO = MoneyVnd(0L)
        val MAX_LOCAL_EXPENSE = MoneyVnd(100_000_000L)

        fun formatVndAmount(value: Long): String {
            val isNegative = value < 0
            val absVal = if (value == Long.MIN_VALUE) Long.MAX_VALUE else Math.abs(value)
            val absStr = absVal.toString()
            val sb = StringBuilder()
            val len = absStr.length
            for (i in 0 until len) {
                if (i > 0 && (len - i) % 3 == 0) {
                    sb.append('.')
                }
                sb.append(absStr[i])
            }
            sb.append('đ')
            return if (isNegative) "-$sb" else sb.toString()
        }

        fun parse(text: String): MoneyVnd {
            val cleaned = text.replace(".", "").replace("đ", "").replace(",", "").trim()
            val parsed = cleaned.toLong()
            return MoneyVnd(parsed)
        }
    }
}
