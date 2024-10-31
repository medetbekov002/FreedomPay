package com.medetbekov.freedompay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.medetbekov.freedompay.databinding.ActivityMainBinding
import com.medetbekov.freedompay.models.PaymentConfig
import com.medetbekov.freedompay.models.PaymentInfo
import money.paybox.payboxsdk.PayboxSdk
import money.paybox.payboxsdk.config.Language
import money.paybox.payboxsdk.config.PaymentSystem
import money.paybox.payboxsdk.config.Region
import money.paybox.payboxsdk.config.RequestMethod
import com.google.android.gms.wallet.*
import com.google.android.gms.wallet.button.ButtonConstants
import com.google.android.gms.wallet.button.ButtonOptions

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sdk: PayboxSdk
    private lateinit var googlePaymentsClient: PaymentsClient
    private lateinit var paymentConfig: PaymentConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка конфигурации платежа
        paymentConfig = PaymentConfig(
            merchantID = "yourMerchantID",
            secretKey = "yourSecretKey",
            enabled = true,
            paymentSystem = "VISA",
            encoding = "UTF-8",
            lifetime = 36,
            userPhone = "+123456789",
            userEmail = "user@example.com",
            language = "en",
            url = "https://example.com",
            requestMethod = "POST"
        )

        // Инициализация SDK Paybox и Google Pay
        setupPayboxSdk()
        setupGooglePayButton()
    }

    private fun setupPayboxSdk() {
        sdk = PayboxSdk.initialize(paymentConfig.merchantID, paymentConfig.secretKey)

        sdk.config().apply {
            testMode(paymentConfig.enabled)
            setRegion(Region.DEFAULT)
            setPaymentSystem(PaymentSystem.VISA)
            setCurrencyCode("KZT")
            autoClearing(paymentConfig.enabled)
            setEncoding(paymentConfig.encoding)
            setRecurringLifetime(paymentConfig.lifetime)
            setPaymentLifetime(300)
            recurringMode(paymentConfig.enabled)
            setUserPhone(paymentConfig.userPhone)
            setUserEmail(paymentConfig.userEmail)
            setLanguage(Language.EN)
            setCheckUrl(paymentConfig.url)
            setResultUrl(paymentConfig.url)
            setRefundUrl(paymentConfig.url)
            setClearingUrl(paymentConfig.url)
            setRequestMethod(RequestMethod.POST)
            setFrameRequired(true)
        }
    }

    private fun setupGooglePayButton() {
        val googlePayButton = binding.googlePayButton
        googlePayButton.initialize(
            ButtonOptions.newBuilder()
                .setButtonType(ButtonConstants.ButtonType.CHECKOUT)
                .setButtonTheme(ButtonConstants.ButtonTheme.LIGHT)
                .build()
        )

        googlePaymentsClient = Wallet.getPaymentsClient(
            this,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION)
                .setTheme(WalletConstants.THEME_LIGHT)
                .build()
        )

        googlePayButton.setOnClickListener {
            val paymentInfo = PaymentInfo(
                amount = "12.00",
                description = "Sample description",
                orderId = "order123",
                userId = "user123",
                extraParams = hashMapOf("param1" to "value1")
            )

            sdk.createGooglePayment(
                paymentInfo.amount.toFloat(),
                paymentInfo.description,
                paymentInfo.orderId,
                paymentInfo.userId,
                paymentInfo.extraParams as HashMap<String, String>?
            ) { payment, error ->
                payment?.let {
                    AutoResolveHelper.resolveTask(
                        googlePaymentsClient.loadPaymentData(createPaymentDataRequest(paymentInfo)),
                        this,
                        REQUEST_CODE
                    )
                } ?: run {
                    Toast.makeText(this, "Payment creation failed: ${error?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val REQUEST_CODE = 123
    }

    private fun createPaymentDataRequest(paymentInfo: PaymentInfo): PaymentDataRequest {
        return PaymentDataRequest.newBuilder()
            .setTransactionInfo(
                TransactionInfo.newBuilder()
                    .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                    .setTotalPrice(paymentInfo.amount)
                    .setCurrencyCode("KZT")
                    .build()
            )
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
            .setCardRequirements(
                CardRequirements.newBuilder()
                    .addAllowedCardNetworks(
                        listOf(WalletConstants.CARD_NETWORK_VISA, WalletConstants.CARD_NETWORK_MASTERCARD)
                    )
                    .build()
            )
            .setPaymentMethodTokenizationParameters(
                PaymentMethodTokenizationParameters.newBuilder()
                    .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                    .addParameter("gateway", "yourGateway")
                    .addParameter("gatewayMerchantId", "yourMerchantIdGivenFromYourGateway")
                    .build()
            )
            .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val paymentData = PaymentData.getFromIntent(it) ?: return
                        val token = paymentData.paymentMethodToken?.token ?: return
                        sdk.confirmGooglePayment(paymentConfig.url, token) { payment, error ->
                            if (error == null) {
                                Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Payment confirmation failed: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    Toast.makeText(this, "Google Pay error: ${AutoResolveHelper.getStatusFromIntent(data)?.statusMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
