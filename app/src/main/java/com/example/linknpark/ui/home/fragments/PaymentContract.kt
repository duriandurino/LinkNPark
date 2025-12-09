package com.example.linknpark.ui.home.fragments

import com.example.linknpark.model.ParkingSession

interface PaymentContract {
    interface View {
        fun showSessionDetails(
            spotCode: String,
            entryTime: String,
            duration: String,
            rate: String,
            totalAmount: String
        )
        fun showPaymentMethodSelected(method: String)
        fun showLoading(show: Boolean)
        fun showPaymentSuccess(message: String)
        fun showPaymentError(message: String)
        fun navigateBack()
    }

    interface Presenter {
        fun attach(view: View, session: ParkingSession)
        fun detach()
        fun onPaymentMethodSelected(method: String)
        fun onConfirmPayment()
    }
}
