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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.linknpark.R
import com.example.linknpark.data.FirebaseAuthRepository
import com.example.linknpark.model.ParkingSession
import com.example.linknpark.model.Reservation
import com.example.linknpark.ui.home.UserHomeActivity
import com.example.linknpark.ui.home.adapters.ReservationsAdapter
import com.example.linknpark.ui.home.adapters.SessionsAdapter
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment(), HomeContract.View {

    private lateinit var presenter: HomeContract.Presenter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvWelcome: TextView
    private lateinit var tvAvailableCount: TextView
    private lateinit var tvNoReservations: TextView
    private lateinit var tvNoSessions: TextView
    private lateinit var rvReservations: RecyclerView
    private lateinit var rvActiveSessions: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnFindParking: MaterialButton
    private lateinit var btnViewBookings: MaterialButton

    private val reservationsAdapter = ReservationsAdapter()
    private val sessionsAdapter = SessionsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvAvailableCount = view.findViewById(R.id.tvAvailableCount)
        tvNoReservations = view.findViewById(R.id.tvNoReservations)
        tvNoSessions = view.findViewById(R.id.tvNoSessions)
        rvReservations = view.findViewById(R.id.rvReservations)
        rvActiveSessions = view.findViewById(R.id.rvActiveSessions)
        progressBar = view.findViewById(R.id.progressBar)
        btnFindParking = view.findViewById(R.id.btnFindParking)
        btnViewBookings = view.findViewById(R.id.btnViewBookings)

        // Setup RecyclerViews
        rvReservations.layoutManager = LinearLayoutManager(requireContext())
        rvReservations.adapter = reservationsAdapter

        rvActiveSessions.layoutManager = LinearLayoutManager(requireContext())
        rvActiveSessions.adapter = sessionsAdapter

        // Get user info from auth repository using singleton
        val authRepository = FirebaseAuthRepository.getInstance()
        val currentUser = authRepository.getCurrentUserSync()
        
        val userId = currentUser?.uid ?: "unknown"
        val userName = currentUser?.name ?: "User"

        // Initialize presenter
        presenter = HomePresenter()
        presenter.attach(this, userId, userName)

        // Set up swipe to refresh
        swipeRefresh.setOnRefreshListener {
            presenter.onRefresh()
        }

        // Set up click listeners
        btnFindParking.setOnClickListener {
            (activity as? UserHomeActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottomNavigation
            )?.selectedItemId = R.id.nav_find_parking
        }

        btnViewBookings.setOnClickListener {
            (activity as? UserHomeActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottomNavigation
            )?.selectedItemId = R.id.nav_bookings
        }
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun showWelcome(userName: String) {
        tvWelcome.text = "Welcome, $userName!"
    }

    override fun showAvailableCount(count: Int) {
        tvAvailableCount.text = count.toString()
    }

    override fun showActiveReservations(reservations: List<Reservation>) {
        rvReservations.visibility = View.VISIBLE
        tvNoReservations.visibility = View.GONE
        reservationsAdapter.submitList(reservations)
    }

    override fun showActiveSessions(sessions: List<ParkingSession>) {
        rvActiveSessions.visibility = View.VISIBLE
        tvNoSessions.visibility = View.GONE
        sessionsAdapter.submitList(sessions)
    }

    override fun showNoReservations() {
        rvReservations.visibility = View.GONE
        tvNoReservations.visibility = View.VISIBLE
    }

    override fun showNoSessions() {
        rvActiveSessions.visibility = View.GONE
        tvNoSessions.visibility = View.VISIBLE
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun setRefreshing(refreshing: Boolean) {
        swipeRefresh.isRefreshing = refreshing
    }
}

