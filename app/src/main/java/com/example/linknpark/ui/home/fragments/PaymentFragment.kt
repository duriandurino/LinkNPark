package com.example.linknpark.ui.home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.linknpark.R
import com.example.linknpark.model.ParkingSession
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Timestamp

class PaymentFragment : Fragment(), PaymentContract.View {

    private lateinit var presenter: PaymentContract.Presenter
    
    // Session summary views
    private lateinit var tvSpotCode: TextView
    private lateinit var tvEntryTime: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvRate: TextView
    private lateinit var tvTotalAmount: TextView
    
    // Payment method cards
    private lateinit var cardCash: MaterialCardView
    private lateinit var cardGcash: MaterialCardView
    private lateinit var cardMaya: MaterialCardView
    private lateinit var cardCredit: MaterialCardView
    
    // Check icons
    private lateinit var ivCashCheck: ImageView
    private lateinit var ivGcashCheck: ImageView
    private lateinit var ivMayaCheck: ImageView
    private lateinit var ivCardCheck: ImageView
    
    private lateinit var btnConfirmPayment: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar

    companion object {
        private const val ARG_SESSION_ID = "session_id"
        private const val ARG_SPOT_CODE = "spot_code"
        private const val ARG_SPOT_ID = "spot_id"
        private const val ARG_ENTRY_TIME = "entry_time"
        private const val ARG_HOURLY_RATE = "hourly_rate"

        fun newInstance(session: ParkingSession): PaymentFragment {
            return PaymentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SESSION_ID, session.sessionId)
                    putString(ARG_SPOT_CODE, session.spotCode)
                    putString(ARG_SPOT_ID, session.spotId)
                    putLong(ARG_ENTRY_TIME, session.enteredAt?.seconds ?: 0)
                    putDouble(ARG_HOURLY_RATE, session.hourlyRate ?: 50.0)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        
        // Reconstruct session from arguments
        val session = reconstructSession()
        
        presenter = PaymentPresenter()
        presenter.attach(this, session)
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        tvSpotCode = view.findViewById(R.id.tvSpotCode)
        tvEntryTime = view.findViewById(R.id.tvEntryTime)
        tvDuration = view.findViewById(R.id.tvDuration)
        tvRate = view.findViewById(R.id.tvRate)
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount)
        
        cardCash = view.findViewById(R.id.cardCash)
        cardGcash = view.findViewById(R.id.cardGcash)
        cardMaya = view.findViewById(R.id.cardMaya)
        cardCredit = view.findViewById(R.id.cardCredit)
        
        ivCashCheck = view.findViewById(R.id.ivCashCheck)
        ivGcashCheck = view.findViewById(R.id.ivGcashCheck)
        ivMayaCheck = view.findViewById(R.id.ivMayaCheck)
        ivCardCheck = view.findViewById(R.id.ivCardCheck)
        
        btnConfirmPayment = view.findViewById(R.id.btnConfirmPayment)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        cardCash.setOnClickListener { presenter.onPaymentMethodSelected("Cash") }
        cardGcash.setOnClickListener { presenter.onPaymentMethodSelected("GCash") }
        cardMaya.setOnClickListener { presenter.onPaymentMethodSelected("Maya") }
        cardCredit.setOnClickListener { presenter.onPaymentMethodSelected("Card") }
        
        btnConfirmPayment.setOnClickListener {
            presenter.onConfirmPayment()
        }
    }

    private fun reconstructSession(): ParkingSession {
        val args = arguments ?: Bundle()
        return ParkingSession(
            sessionId = args.getString(ARG_SESSION_ID) ?: "",
            spotCode = args.getString(ARG_SPOT_CODE) ?: "",
            spotId = args.getString(ARG_SPOT_ID),
            enteredAt = Timestamp(args.getLong(ARG_ENTRY_TIME), 0),
            hourlyRate = args.getDouble(ARG_HOURLY_RATE, 50.0),
            status = "ACTIVE"
        )
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun showSessionDetails(
        spotCode: String,
        entryTime: String,
        duration: String,
        rate: String,
        totalAmount: String
    ) {
        tvSpotCode.text = spotCode
        tvEntryTime.text = entryTime
        tvDuration.text = duration
        tvRate.text = rate
        tvTotalAmount.text = totalAmount
    }

    override fun showPaymentMethodSelected(method: String) {
        // Reset all cards
        val allCards = listOf(cardCash, cardGcash, cardMaya, cardCredit)
        val allChecks = listOf(ivCashCheck, ivGcashCheck, ivMayaCheck, ivCardCheck)
        
        allCards.forEach { card ->
            card.strokeColor = ContextCompat.getColor(requireContext(), android.R.color.transparent)
        }
        allChecks.forEach { check ->
            check.visibility = View.GONE
        }
        
        // Highlight selected card
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.blue_primary)
        when (method) {
            "Cash" -> {
                cardCash.strokeColor = selectedColor
                ivCashCheck.visibility = View.VISIBLE
            }
            "GCash" -> {
                cardGcash.strokeColor = selectedColor
                ivGcashCheck.visibility = View.VISIBLE
            }
            "Maya" -> {
                cardMaya.strokeColor = selectedColor
                ivMayaCheck.visibility = View.VISIBLE
            }
            "Card" -> {
                cardCredit.strokeColor = selectedColor
                ivCardCheck.visibility = View.VISIBLE
            }
        }
        
        // Enable confirm button
        btnConfirmPayment.isEnabled = true
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnConfirmPayment.isEnabled = !show
    }

    override fun showPaymentSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun showPaymentError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun navigateBack() {
        parentFragmentManager.popBackStack()
    }
}
