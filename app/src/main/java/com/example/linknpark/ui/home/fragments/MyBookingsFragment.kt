package com.example.linknpark.ui.home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.data.FirebaseAuthRepository
import com.example.linknpark.model.ParkingSession
import com.example.linknpark.model.Reservation
import com.example.linknpark.ui.home.adapters.ReservationsAdapter
import com.example.linknpark.ui.home.adapters.SessionHistoryAdapter

class MyBookingsFragment : Fragment(), MyBookingsContract.View {

    private lateinit var presenter: MyBookingsContract.Presenter
    private lateinit var rvReservations: RecyclerView
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvNoReservations: TextView
    private lateinit var tvNoHistory: TextView
    private lateinit var progressBar: ProgressBar

    private val reservationsAdapter = ReservationsAdapter()
    private val historyAdapter = SessionHistoryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        rvReservations = view.findViewById(R.id.rvReservations)
        rvHistory = view.findViewById(R.id.rvHistory)
        tvNoReservations = view.findViewById(R.id.tvNoReservations)
        tvNoHistory = view.findViewById(R.id.tvNoHistory)
        progressBar = view.findViewById(R.id.progressBar)

        // Setup RecyclerViews
        rvReservations.layoutManager = LinearLayoutManager(requireContext())
        rvReservations.adapter = reservationsAdapter

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter

        // Get user info
        val authRepository = FirebaseAuthRepository()
        val currentUser = authRepository.getCurrentUserSync()
        val userId = currentUser?.uid ?: "unknown"

        // Initialize presenter
        presenter = MyBookingsPresenter()
        presenter.attach(this, userId)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun showActiveReservations(reservations: List<Reservation>) {
        rvReservations.visibility = View.VISIBLE
        tvNoReservations.visibility = View.GONE
        reservationsAdapter.submitList(reservations)
    }

    override fun showNoActiveReservations() {
        rvReservations.visibility = View.GONE
        tvNoReservations.visibility = View.VISIBLE
    }

    override fun showParkingHistory(sessions: List<ParkingSession>) {
        rvHistory.visibility = View.VISIBLE
        tvNoHistory.visibility = View.GONE
        historyAdapter.submitList(sessions)
    }

    override fun showNoHistory() {
        rvHistory.visibility = View.GONE
        tvNoHistory.visibility = View.VISIBLE
    }

    override fun showCancelSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showCancelError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

