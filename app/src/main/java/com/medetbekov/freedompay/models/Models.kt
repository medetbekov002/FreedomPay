package com.medetbekov.freedompay.models

// Конфигурация для Paybox SDK
data class PaymentConfig(
    val merchantID: String,
    val secretKey: String,
    val enabled: Boolean,
    val paymentSystem: String,
    val encoding: String = "UTF-8",
    val lifetime: Int = 36, // период действия по умолчанию 36 месяцев
    val userPhone: String,
    val userEmail: String,
    val language: String,
    val url: String,
    val requestMethod: String = "POST"
)

// Данные платежа для транзакций
data class PaymentInfo(
    val amount: String,
    val description: String,
    val orderId: String,
    val userId: String,
    val extraParams: Map<String, String> = emptyMap()
)

// Идентификаторы для различных объектов
data class PaymentId(val id: String)
data class CardId(val id: String)
data class MerchantId(val id: String)

// Mock-интерфейс для PaymentView, который соответствует типу SDK (измените по мере необходимости)
interface PaymentView {
    var listener: Any? // Укажите здесь правильный тип, если известно
}