package com.prestamos.app.data.local.db

import androidx.room.TypeConverter
import com.prestamos.app.domain.model.CurrencyType
import com.prestamos.app.domain.model.InstallmentStatus

import com.prestamos.app.domain.model.LoanStatus
import com.prestamos.app.domain.model.PaymentFrequency
import com.prestamos.app.domain.model.PaymentMethod
import java.math.BigDecimal

class Converters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }

    @TypeConverter
    fun fromCurrencyType(currency: CurrencyType?): String? {
        return currency?.name
    }

    @TypeConverter
    fun toCurrencyType(value: String?): CurrencyType? {
        return value?.let { CurrencyType.valueOf(it) }
    }

    @TypeConverter
    fun fromPaymentFrequency(frequency: PaymentFrequency?): String? {
        return frequency?.name
    }

    @TypeConverter
    fun toPaymentFrequency(value: String?): PaymentFrequency? {
        return value?.let { PaymentFrequency.valueOf(it) }
    }

    @TypeConverter
    fun fromLoanStatus(status: LoanStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toLoanStatus(value: String?): LoanStatus? {
        return value?.let { LoanStatus.valueOf(it) }
    }

    @TypeConverter
    fun fromInstallmentStatus(status: InstallmentStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toInstallmentStatus(value: String?): InstallmentStatus? {
        return value?.let { InstallmentStatus.valueOf(it) }
    }

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod?): String? {
        return method?.name
    }

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? {
        return value?.let { PaymentMethod.valueOf(it) }
    }
}
