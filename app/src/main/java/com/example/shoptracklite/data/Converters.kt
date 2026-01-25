package com.example.shoptracklite.data

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromPaymentMethod(paymentMethod: PaymentMethod?): String? {
        return paymentMethod?.name
    }

    @TypeConverter
    fun toPaymentMethod(paymentMethod: String?): PaymentMethod? {
        return paymentMethod?.let { PaymentMethod.valueOf(it) }
    }
}
