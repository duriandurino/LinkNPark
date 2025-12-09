package com.example.linknpark.ui.home.fragments

import android.app.DatePickerDialog
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
import com.example.linknpark.data.FirebaseDriverRepository
import com.example.linknpark.model.ParkingSession
import com.example.linknpark.ui.home.adapters.HistoryAdapter
import com.google.android.material.chip.Chip
import java.util.*

class HistoryFragment : Fragment(), HistoryContract.View {

    private lateinit var presenter: HistoryContract.Presenter
    
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmptyState: View
    private lateinit var progressBar: ProgressBar
    private lateinit var tvDateRange: TextView
    
    // Filter chips
    private lateinit var chipAll: Chip
    private lateinit var chipCompleted: Chip
    private lateinit var chipCancelled: Chip
    private lateinit var chipDateRange: Chip
    
    private val historyAdapter = HistoryAdapter()
    
    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupChipListeners()
        
        // Get user info
        val authRepository = FirebaseAuthRepository.getInstance()
        val currentUser = authRepository.getCurrentUserSync()
        val userId = currentUser?.uid ?: "unknown"
        
        // Initialize presenter with repository injection
        presenter = HistoryPresenter(FirebaseDriverRepository())
        presenter.attach(this, userId)
    }

    private fun initViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        rvHistory = view.findViewById(R.id.rvHistory)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        progressBar = view.findViewById(R.id.progressBar)
        tvDateRange = view.findViewById(R.id.tvDateRange)
        
        chipAll = view.findViewById(R.id.chipAll)
        chipCompleted = view.findViewById(R.id.chipCompleted)
        chipCancelled = view.findViewById(R.id.chipCancelled)
        chipDateRange = view.findViewById(R.id.chipDateRange)
        
        swipeRefresh.setOnRefreshListener {
            presenter.onRefresh()
        }
    }

    private fun setupRecyclerView() {
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter
    }

    private fun setupChipListeners() {
        chipAll.setOnClickListener {
            presenter.onFilterChanged("ALL")
        }
        
        chipCompleted.setOnClickListener {
            presenter.onFilterChanged("COMPLETED")
        }
        
        chipCancelled.setOnClickListener {
            presenter.onFilterChanged("CANCELLED")
        }
        
        chipDateRange.setOnClickListener {
            showDateRangePicker()
        }
    }
    
    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()
        
        // Start date picker
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val startCal = Calendar.getInstance()
                startCal.set(year, month, day, 0, 0, 0)
                selectedStartDate = startCal.timeInMillis
                
                // End date picker
                DatePickerDialog(
                    requireContext(),
                    { _, endYear, endMonth, endDay ->
                        val endCal = Calendar.getInstance()
                        endCal.set(endYear, endMonth, endDay, 23, 59, 59)
                        selectedEndDate = endCal.timeInMillis
                        
                        presenter.onDateRangeSelected(selectedStartDate, selectedEndDate)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setTitle("Select End Date")
                    show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Select Start Date")
            show()
        }
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun showSessions(sessions: List<ParkingSession>) {
        rvHistory.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
        historyAdapter.submitList(sessions)
    }

    override fun showEmptyState() {
        rvHistory.visibility = View.GONE
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

    override fun showDateRangeLabel(label: String) {
        if (label.isNotEmpty()) {
            tvDateRange.text = "Showing sessions from $label"
            tvDateRange.visibility = View.VISIBLE
        } else {
            tvDateRange.visibility = View.GONE
        }
    }
}
