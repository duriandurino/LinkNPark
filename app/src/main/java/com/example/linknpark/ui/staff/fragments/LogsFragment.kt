package com.example.linknpark.ui.staff.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.ui.staff.adapter.ActivityLog
import com.example.linknpark.ui.staff.adapter.ActivityLogAdapter
import com.example.linknpark.ui.staff.presenter.LogsPresenter
import com.google.android.material.button.MaterialButton

class LogsFragment : Fragment(), LogsContract.View {

    private lateinit var rvLogs: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var btnFilterAll: MaterialButton
    private lateinit var btnFilterEntry: MaterialButton
    private lateinit var btnFilterExit: MaterialButton
    private lateinit var btnExportLogs: MaterialButton
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageButton
    private var progressBar: ProgressBar? = null
    
    private val adapter = ActivityLogAdapter()
    private lateinit var presenter: LogsContract.Presenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        rvLogs = view.findViewById(R.id.rvLogs)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        btnFilterAll = view.findViewById(R.id.btnFilterAll)
        btnFilterEntry = view.findViewById(R.id.btnFilterEntry)
        btnFilterExit = view.findViewById(R.id.btnFilterExit)
        btnExportLogs = view.findViewById(R.id.btnExportLogs)
        etSearch = view.findViewById(R.id.etSearch)
        btnClearSearch = view.findViewById(R.id.btnClearSearch)
        progressBar = view.findViewById(R.id.progressBar)

        // Setup RecyclerView
        rvLogs.layoutManager = LinearLayoutManager(requireContext())
        rvLogs.adapter = adapter

        // Setup filter buttons
        btnFilterAll.setOnClickListener { 
            presenter.onFilterChanged("ALL")
        }
        btnFilterEntry.setOnClickListener { 
            presenter.onFilterChanged("ENTRY")
        }
        btnFilterExit.setOnClickListener { 
            presenter.onFilterChanged("EXIT")
        }
        
        // Setup search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                presenter.onSearchQueryChanged(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
        }
        
        // Setup export
        btnExportLogs.setOnClickListener {
            exportLogs()
        }

        // Initialize presenter and load logs
        presenter = LogsPresenter()
        presenter.attach(this)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detach()
    }

    // LogsContract.View implementation
    override fun showLogs(logs: List<ActivityLog>) {
        rvLogs.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
        adapter.submitList(logs)
    }

    override fun showEmptyState() {
        rvLogs.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showLoading(isLoading: Boolean) {
        progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    private fun exportLogs() {
        val logs = presenter.getFilteredLogs()
        
        if (logs.isEmpty()) {
            Toast.makeText(requireContext(), "No logs to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Build CSV content
        val sb = StringBuilder()
        sb.append("Type,Spot Code,License Plate,Time\n")
        
        logs.forEach { log ->
            sb.append("${log.type.replace(",", "")},${log.spotCode},${log.licensePlate},${log.time}\n")
        }
        
        // Share via Intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Parking Activity Logs")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        
        startActivity(Intent.createChooser(shareIntent, "Export Logs"))
        Toast.makeText(requireContext(), "Exporting ${logs.size} logs...", Toast.LENGTH_SHORT).show()
    }
}
