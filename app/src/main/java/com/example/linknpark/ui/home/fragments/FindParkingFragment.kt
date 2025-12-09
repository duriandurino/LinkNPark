package com.example.linknpark.ui.home.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.linknpark.R
import com.example.linknpark.data.FirebaseAuthRepository
import com.example.linknpark.model.ParkingLot
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.ui.home.adapters.ParkingLotsAdapter

class FindParkingFragment : Fragment(), FindParkingMapContract.View {

    private lateinit var presenter: FindParkingMapContract.Presenter
    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var etSearchParkingLots: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var fabToggleView: FloatingActionButton
    private lateinit var rvParkingLots: RecyclerView
    private lateinit var progressBar: ProgressBar
    
    // Filter chips
    private lateinit var chipAll: com.google.android.material.chip.Chip
    private lateinit var chipMotorcycle: com.google.android.material.chip.Chip
    private lateinit var chipCar: com.google.android.material.chip.Chip
    private lateinit var chipSUV: com.google.android.material.chip.Chip
    private lateinit var chipPriceLow: com.google.android.material.chip.Chip
    private lateinit var chipNearby: com.google.android.material.chip.Chip

    // List adapter
    private lateinit var parkingLotsAdapter: ParkingLotsAdapter
    private var isListViewVisible = false
    private var allParkingLots = listOf<ParkingLot>()
    
    // Filter state
    private var selectedVehicleType: String? = null
    private var sortByPriceAsc = false
    private var sortByDistanceAsc = false

    private val mapMarkers = mutableMapOf<String, Marker>()
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val DEFAULT_ZOOM = 15.0
        const val USER_LOCATION_ZOOM = 16.0
        const val SEARCH_DEBOUNCE_DELAY = 300L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_find_parking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize osmdroid configuration
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", 0)
        )

        // Initialize views
        mapView = view.findViewById(R.id.mapView)
        etSearchParkingLots = view.findViewById(R.id.etSearchParkingLots)
        btnClearSearch = view.findViewById(R.id.btnClearSearch)
        fabMyLocation = view.findViewById(R.id.fabMyLocation)
        fabToggleView = view.findViewById(R.id.fabToggleView)
        rvParkingLots = view.findViewById(R.id.rvParkingLots)
        progressBar = view.findViewById(R.id.progressBar)
        
        // Initialize filter chips
        chipAll = view.findViewById(R.id.chipAll)
        chipMotorcycle = view.findViewById(R.id.chipMotorcycle)
        chipCar = view.findViewById(R.id.chipCar)
        chipSUV = view.findViewById(R.id.chipSUV)
        chipPriceLow = view.findViewById(R.id.chipPriceLow)
        chipNearby = view.findViewById(R.id.chipNearby)

        // Setup parking lots list adapter
        setupParkingLotsList()
        
        // Setup filter chips
        setupFilterChips()

        // Setup MapView
        setupMapView()

        // Setup search functionality
        setupSearchBar()

        // Setup toggle view button (switch between map and list)
        fabToggleView.setOnClickListener {
            toggleListView()
        }

        // Setup my location button
        fabMyLocation.setOnClickListener {
            if (checkLocationPermission()) {
                myLocationOverlay.enableFollowLocation()
                myLocationOverlay.myLocation?.let { geoPoint ->
                    mapView.controller.animateTo(geoPoint)
                    mapView.controller.setZoom(USER_LOCATION_ZOOM)
                }
            } else {
                requestLocationPermission()
            }
        }

        // Initialize presenter
        val authRepository = FirebaseAuthRepository.getInstance()
        val currentUser = authRepository.getCurrentUserSync()
        val userId = currentUser?.uid ?: "unknown"

        presenter = FindParkingMapPresenter()
        presenter.attach(this, userId)

        // Request location permission and load parking lots
        if (checkLocationPermission()) {
            presenter.requestUserLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun setupParkingLotsList() {
        parkingLotsAdapter = ParkingLotsAdapter(
            onLotClick = { lot ->
                // Center map on this lot and switch to map view
                val geoPoint = GeoPoint(lot.location.latitude, lot.location.longitude)
                mapView.controller.animateTo(geoPoint)
                mapView.controller.setZoom(USER_LOCATION_ZOOM)
                
                // Switch to map view
                if (isListViewVisible) {
                    toggleListView()
                }
            },
            onGoToMapClick = { lot ->
                // Center map on this lot
                val geoPoint = GeoPoint(lot.location.latitude, lot.location.longitude)
                mapView.controller.animateTo(geoPoint)
                mapView.controller.setZoom(USER_LOCATION_ZOOM)
                
                // Switch to map view
                if (isListViewVisible) {
                    toggleListView()
                }
                
                Toast.makeText(requireContext(), "Centered on ${lot.name}", Toast.LENGTH_SHORT).show()
            },
            onViewSpotsClick = { lot ->
                navigateToParkingSpotSelection(lot)
            }
        )
        
        rvParkingLots.layoutManager = LinearLayoutManager(requireContext())
        rvParkingLots.adapter = parkingLotsAdapter
    }
    
    private fun setupFilterChips() {
        // Vehicle type chips - mutually exclusive with "All"
        chipAll.setOnClickListener {
            resetVehicleFilters()
            chipAll.isChecked = true
            applyFilters()
        }
        
        chipMotorcycle.setOnClickListener {
            chipAll.isChecked = false
            selectedVehicleType = if (chipMotorcycle.isChecked) "MOTORCYCLE" else null
            applyFilters()
        }
        
        chipCar.setOnClickListener {
            chipAll.isChecked = false
            selectedVehicleType = if (chipCar.isChecked) "CAR" else null
            applyFilters()
        }
        
        chipSUV.setOnClickListener {
            chipAll.isChecked = false
            selectedVehicleType = if (chipSUV.isChecked) "SUV" else null
            applyFilters()
        }
        
        // Sort chips - toggle behavior
        chipPriceLow.setOnClickListener {
            sortByPriceAsc = chipPriceLow.isChecked
            if (sortByPriceAsc) {
                sortByDistanceAsc = false
                chipNearby.isChecked = false
            }
            applyFilters()
        }
        
        chipNearby.setOnClickListener {
            sortByDistanceAsc = chipNearby.isChecked
            if (sortByDistanceAsc) {
                sortByPriceAsc = false
                chipPriceLow.isChecked = false
            }
            applyFilters()
        }
    }
    
    private fun resetVehicleFilters() {
        selectedVehicleType = null
        chipMotorcycle.isChecked = false
        chipCar.isChecked = false
        chipSUV.isChecked = false
    }
    
    private fun applyFilters() {
        var filteredLots = allParkingLots.toList()
        
        // Apply vehicle type filter
        selectedVehicleType?.let { vehicleType ->
            filteredLots = filteredLots.filter { lot ->
                // For now, assume all lots support all vehicle types
                // In production, this would check lot.supportedVehicleTypes
                when (vehicleType) {
                    "MOTORCYCLE" -> lot.pricePerHour <= 30.0  // Lower rate for motorcycles
                    "SUV" -> lot.availableSpots > 0  // SUVs need more space
                    else -> true  // CAR - all lots support cars
                }
            }
        }
        
        // Apply sorting
        filteredLots = when {
            sortByPriceAsc -> filteredLots.sortedBy { it.pricePerHour }
            sortByDistanceAsc -> {
                // Mock distance sorting - in production use actual user location
                filteredLots.shuffled()  // Simulated "nearby" sorting
            }
            else -> filteredLots
        }
        
        // Update the list and map
        parkingLotsAdapter.submitList(filteredLots)
        displayParkingLots(filteredLots)
        
        // Show filter result message
        val filterMsg = when {
            selectedVehicleType != null -> "${filteredLots.size} ${selectedVehicleType?.lowercase()} parking lots"
            sortByPriceAsc -> "${filteredLots.size} lots sorted by price"
            sortByDistanceAsc -> "${filteredLots.size} nearby lots"
            else -> "${filteredLots.size} parking lots"
        }
        Toast.makeText(requireContext(), filterMsg, Toast.LENGTH_SHORT).show()
    }

    private fun toggleListView() {
        isListViewVisible = !isListViewVisible
        
        if (isListViewVisible) {
            mapView.visibility = View.GONE
            rvParkingLots.visibility = View.VISIBLE
            fabToggleView.setImageResource(android.R.drawable.ic_dialog_map)
            fabMyLocation.visibility = View.GONE
            
            // Update list with current parking lots
            parkingLotsAdapter.submitList(allParkingLots)
        } else {
            mapView.visibility = View.VISIBLE
            rvParkingLots.visibility = View.GONE
            fabToggleView.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            fabMyLocation.visibility = View.VISIBLE
        }
    }

    private fun setupMapView() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 10.0
        mapView.maxZoomLevel = 20.0

        // Set initial zoom and center
        mapView.controller.setZoom(DEFAULT_ZOOM)

        // Setup my location overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
        myLocationOverlay.enableMyLocation()
        mapView.overlays.add(myLocationOverlay)

        // Set default center (will be updated by user location or default coordinates)
        val defaultCenter = GeoPoint(14.5995, 120.9842) // Manila, Philippines
        mapView.controller.setCenter(defaultCenter)
    }

    private fun setupSearchBar() {
        etSearchParkingLots.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE

                // Cancel previous search
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                // Schedule new search with debounce
                searchRunnable = Runnable {
                    presenter.searchParkingLots(s?.toString() ?: "")
                }
                searchHandler.postDelayed(searchRunnable!!, SEARCH_DEBOUNCE_DELAY)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearchParkingLots.text.clear()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.requestUserLocation()
                myLocationOverlay.enableMyLocation()
            }
        }
    }

    override fun displayParkingLots(lots: List<ParkingLot>) {
        // Store lots for list view
        allParkingLots = lots
        
        // Update list adapter if visible
        if (isListViewVisible) {
            parkingLotsAdapter.submitList(lots)
        }
        
        // Clear existing markers
        mapMarkers.values.forEach { marker ->
            mapView.overlays.remove(marker)
        }
        mapMarkers.clear()

        // Add new markers
        lots.forEach { lot ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(lot.location.latitude, lot.location.longitude)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = lot.name
            marker.snippet = lot.address

            // Set marker click listener - centers map on the marker
            marker.setOnMarkerClickListener { clickedMarker, _ ->
                mapView.controller.animateTo(clickedMarker.position)
                mapView.controller.setZoom(USER_LOCATION_ZOOM)
                true
            }

            // Store marker
            mapMarkers[lot.lotId] = marker
            mapView.overlays.add(marker)
        }

        mapView.invalidate()

        // Auto-zoom to single result
        if (lots.size == 1) {
            val lot = lots.first()
            val geoPoint = GeoPoint(lot.location.latitude, lot.location.longitude)
            mapView.controller.animateTo(geoPoint)
            mapView.controller.setZoom(USER_LOCATION_ZOOM)
        }
    }


    override fun centerMapOnUserLocation(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        mapView.controller.animateTo(geoPoint)
        mapView.controller.setZoom(USER_LOCATION_ZOOM)
        myLocationOverlay.disableFollowLocation()
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showNoResultsMessage(query: String) {
        Toast.makeText(requireContext(), "No parking lots found matching '$query'", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToParkingSpotSelection(lot: ParkingLot) {
        val fragment = ParkingSpotSelectionFragment.newInstance(lot.lotId, lot.name)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        presenter.detach()
        mapView.onDetach()
        searchHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }
}
