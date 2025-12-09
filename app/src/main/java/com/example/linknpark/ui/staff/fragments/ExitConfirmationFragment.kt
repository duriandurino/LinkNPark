package com.example.linknpark.ui.staff.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.linknpark.R
import com.example.linknpark.model.ParkingSession
import com.example.linknpark.ui.staff.adapter.PendingExitsAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText

class ExitConfirmationFragment : Fragment(), ExitConfirmationContract.View {

    private lateinit var presenter: ExitConfirmationContract.Presenter
    
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvPendingExits: RecyclerView
    private lateinit var layoutEmptyState: View
    private lateinit var progressBar: ProgressBar
    private lateinit var tvPendingCount: TextView
    
    private lateinit var pendingExitsAdapter: PendingExitsAdapter

    companion object {
        fun newInstance() = ExitConfirmationFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exit_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        
        presenter = ExitConfirmationPresenter()
        presenter.attach(this)
    }

    private fun initViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        rvPendingExits = view.findViewById(R.id.rvPendingExits)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        progressBar = view.findViewById(R.id.progressBar)
        tvPendingCount = view.findViewById(R.id.tvPendingCount)
        
        swipeRefresh.setOnRefreshListener {
            presenter.onRefresh()
        }
    }

    private fun setupRecyclerView() {
        pendingExitsAdapter = PendingExitsAdapter(
            onConfirmPayment = { session ->
                showConfirmPaymentDialog(session)
            },
            onOverrideFee = { session ->
                presenter.onOverrideFee(session)
            }
        )
        
        rvPendingExits.layoutManager = LinearLayoutManager(requireContext())
        rvPendingExits.adapter = pendingExitsAdapter
    }

    private fun showConfirmPaymentDialog(session: ParkingSession) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Payment")
            .setMessage("Confirm cash payment for ${session.licensePlate.ifEmpty { "vehicle" }} at spot ${session.spotCode}?")
            .setPositiveButton("Confirm") { _, _ ->
                presenter.onConfirmPayment(session)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun showPendingExits(sessions: List<ParkingSession>) {
        rvPendingExits.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
        pendingExitsAdapter.submitList(sessions)
    }

    override fun showEmptyState() {
        rvPendingExits.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun setRefreshing(refreshing: Boolean) {
        swipeRefresh.isRefreshing = refreshing
    }

    override fun updatePendingCount(count: Int) {
        tvPendingCount.text = "$count vehicle${if (count != 1) "s" else ""}"
    }

    override fun showPaymentSuccess(message: String) {
        Toast.makeText(requireContext(), "✓ $message", Toast.LENGTH_LONG).show()
    }

    override fun showFeeOverrideDialog(session: ParkingSession, originalAmount: Double) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_fee_override, null)
        
        val tvOriginalAmount = dialogView.findViewById<TextView>(R.id.tvOriginalAmount)
        val etNewAmount = dialogView.findViewById<TextInputEditText>(R.id.etNewAmount)
        val etReason = dialogView.findViewById<TextInputEditText>(R.id.etReason)
        val chipFree = dialogView.findViewById<Chip>(R.id.chipFree)
        val chip50Off = dialogView.findViewById<Chip>(R.id.chip50Off)
        val chipFlat50 = dialogView.findViewById<Chip>(R.id.chipFlat50)
        
        tvOriginalAmount.text = "₱${String.format("%.2f", originalAmount)}"
        etNewAmount.setText(String.format("%.2f", originalAmount))
        
        // Quick adjustment chips
        chipFree.setOnClickListener {
            etNewAmount.setText("0.00")
            etReason.setText("Complimentary parking")
        }
        chip50Off.setOnClickListener {
            etNewAmount.setText(String.format("%.2f", originalAmount * 0.5))
            etReason.setText("50% discount")
        }
        chipFlat50.setOnClickListener {
            etNewAmount.setText("50.00")
            etReason.setText("Flat rate adjustment")
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<View>(R.id.btnApplyOverride).setOnClickListener {
            val newAmount = etNewAmount.text.toString().toDoubleOrNull() ?: originalAmount
            val reason = etReason.text.toString().ifEmpty { "Staff override" }
            
            dialog.dismiss()
            presenter.onApplyFeeOverride(session, newAmount, reason)
        }
        
        dialog.show()
    }
}
