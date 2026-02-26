package com.devsusana.hometutorpro.data.local.converters

import androidx.room.TypeConverter
import com.devsusana.hometutorpro.domain.entities.PaymentType
import java.time.DayOfWeek

/**
 * Type converters for Room database to handle non-primitive types.
 * Converts between domain types and database-compatible types.
 */
/**
 * Room type converters for local persistence.
 */
class TypeConverters {

    @TypeConverter
    fun fromDayOfWeek(value: DayOfWeek?): Int? {
        return value?.value
    }

    @TypeConverter
    fun toDayOfWeek(value: Int?): DayOfWeek? {
        return value?.let { DayOfWeek.of(it) }
    }

    @TypeConverter
    fun fromPaymentType(value: PaymentType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toPaymentType(value: String?): PaymentType? {
        return value?.let { PaymentType.valueOf(it) }
    }
}
