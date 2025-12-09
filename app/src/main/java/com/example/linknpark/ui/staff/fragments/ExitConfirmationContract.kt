package com.example.linknpark.ui.staff.fragments

import com.example.linknpark.model.ParkingSession

interface ExitConfirmationContract {
    interface View {
        fun showPendingExits(sessions: List<ParkingSession>)
        fun showEmptyState()
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun setRefreshing(refreshing: Boolean)
        fun updatePendingCount(count: Int)
        fun showPaymentSuccess(message: String)
        fun showFeeOverrideDialog(session: ParkingSession, originalAmount: Double)
    }

    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun loadPendingExits()
        fun onConfirmPayment(session: ParkingSession)
        fun onOverrideFee(session: ParkingSession)
        fun onApplyFeeOverride(session: ParkingSession, newAmount: Double, reason: String)
        fun onRefresh()
    }
}
