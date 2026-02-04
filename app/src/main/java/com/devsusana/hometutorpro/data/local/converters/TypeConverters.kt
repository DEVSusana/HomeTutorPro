package com.devsusana.hometutorpro.data.local.converters

import androidx.room.TypeConverter
import com.devsusana.hometutorpro.domain.entities.PaymentType
import java.time.DayOfWeek

/**
 * Type converters for Room database to handle non-primitive types.
 * Converts between domain types and database-compatible types.
 */
class TypeConverters {

    @TypeConverter
    fun fromDayOfWeek(value: DayOfWeek?): String? {
        return value?.name
    }

    @TypeConverter
    fun toDayOfWeek(value: String?): DayOfWeek? {
        return value?.let { DayOfWeek.valueOf(it) }
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
