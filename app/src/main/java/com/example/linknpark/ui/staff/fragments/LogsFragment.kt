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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.ui.staff.adapter.ActivityLog
import com.example.linknpark.ui.staff.adapter.ActivityLogAdapter
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class LogsFragment : Fragment() {

    private lateinit var rvLogs: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var btnFilterAll: MaterialButton
    private lateinit var btnFilterEntry: MaterialButton
    private lateinit var btnFilterExit: MaterialButton
    private lateinit var btnExportLogs: MaterialButton
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageButton
    
    private val adapter = ActivityLogAdapter()
    private val firestore = FirebaseFirestore.getInstance()
    private var currentFilter = "ALL"
    private var searchQuery = ""
    private var allLogs = listOf<ActivityLog>()

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

        // Setup RecyclerView
        rvLogs.layoutManager = LinearLayoutManager(requireContext())
        rvLogs.adapter = adapter

        // Setup filter buttons
        btnFilterAll.setOnClickListener { 
            currentFilter = "ALL"
            applyFilters()
        }
        btnFilterEntry.setOnClickListener { 
            currentFilter = "ENTRY"
            applyFilters()
        }
        btnFilterExit.setOnClickListener { 
            currentFilter = "EXIT"
            applyFilters()
        }
        
        // Setup search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilters()
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

        // Load logs
        loadLogs()
    }

    private fun loadLogs() {
        firestore.collection("parking_sessions")
            .orderBy("entryTime", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { snapshot ->
                val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                
                allLogs = snapshot.documents.mapNotNull { doc ->
                    try {
                        val status = doc.getString("status") ?: "ACTIVE"
                        val entryTime = doc.getTimestamp("entryTime")?.toDate()
                        val exitTime = doc.getTimestamp("exitTime")?.toDate()
                        val spotCode = doc.getString("spotCode") ?: ""
                        val licensePlate = doc.getString("licensePlate") ?: ""
                        
                        // Determine activity type
                        val isEntry = status == "ACTIVE" || exitTime == null
                        
                        val type = if (isEntry) "🚗 Vehicle Entry" else "✓ Vehicle Exit"
                        val time = if (isEntry && entryTime != null) {
                            dateFormat.format(entryTime)
                        } else if (!isEntry && exitTime != null) {
                            dateFormat.format(exitTime)
                        } else {
                            "N/A"
                        }
                        
                        ActivityLog(
                            type = type,
                            spotCode = spotCode,
                            licensePlate = licensePlate,
                            time = time,
                            isEntry = isEntry
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                applyFilters()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading logs: ${e.message}", Toast.LENGTH_SHORT).show()
                layoutEmptyState.visibility = View.VISIBLE
                rvLogs.visibility = View.GONE
            }
    }
    
    private fun applyFilters() {
        var filteredLogs = allLogs
        
        // Apply type filter
        filteredLogs = when (currentFilter) {
            "ENTRY" -> filteredLogs.filter { it.isEntry }
            "EXIT" -> filteredLogs.filter { !it.isEntry }
            else -> filteredLogs
        }
        
        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase()
            filteredLogs = filteredLogs.filter { log ->
                log.spotCode.lowercase().contains(query) ||
                log.licensePlate.lowercase().contains(query)
            }
        }
        
        if (filteredLogs.isEmpty()) {
            rvLogs.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            rvLogs.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
            adapter.submitList(filteredLogs)
        }
    }
    
    private fun exportLogs() {
        if (allLogs.isEmpty()) {
            Toast.makeText(requireContext(), "No logs to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Build CSV content
        val sb = StringBuilder()
        sb.append("Type,Spot Code,License Plate,Time\n")
        
        allLogs.forEach { log ->
            sb.append("${log.type.replace(",", "")},${log.spotCode},${log.licensePlate},${log.time}\n")
        }
        
        // Share via Intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Parking Activity Logs")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        
        startActivity(Intent.createChooser(shareIntent, "Export Logs"))
        Toast.makeText(requireContext(), "Exporting ${allLogs.size} logs...", Toast.LENGTH_SHORT).show()
    }
}
