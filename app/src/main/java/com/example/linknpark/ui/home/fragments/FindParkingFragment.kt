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

class FindParkingFragment : Fragment(), FindParkingMapContract.View {

    private lateinit var presenter: FindParkingMapContract.Presenter
    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var etSearchParkingLots: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    // Bottom sheet views
    private lateinit var tvBottomSheetLotName: TextView
    private lateinit var tvBottomSheetAddress: TextView
    private lateinit var tvBottomSheetAvailableSpots: TextView
    private lateinit var tvBottomSheetPrice: TextView
    private lateinit var btnViewParkingSpots: MaterialButton

    private val mapMarkers = mutableMapOf<String, Marker>()
    private var selectedParkingLot: ParkingLot? = null
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
        progressBar = view.findViewById(R.id.progressBar)

        // Initialize bottom sheet views
        val bottomSheet = view.findViewById<View>(R.id.bottomSheetParkingLotDetails)
        tvBottomSheetLotName = bottomSheet.findViewById(R.id.tvBottomSheetLotName)
        tvBottomSheetAddress = bottomSheet.findViewById(R.id.tvBottomSheetAddress)
        tvBottomSheetAvailableSpots = bottomSheet.findViewById(R.id.tvBottomSheetAvailableSpots)
        tvBottomSheetPrice = bottomSheet.findViewById(R.id.tvBottomSheetPrice)
        btnViewParkingSpots = bottomSheet.findViewById(R.id.btnViewParkingSpots)

        // Setup MapView
        setupMapView()

        // Setup bottom sheet
        setupBottomSheet(bottomSheet)

        // Setup search functionality
        setupSearchBar()

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

    private fun setupBottomSheet(bottomSheet: View) {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 200

        // Handle bottom sheet state changes
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    deselectAllMarkers()
                    selectedParkingLot = null
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Optional: handle slide animations
            }
        })

        // Handle view parking spots button click
        btnViewParkingSpots.setOnClickListener {
            selectedParkingLot?.let { lot ->
                navigateToParkingSpotSelection(lot)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
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

            // Set marker click listener
            marker.setOnMarkerClickListener { clickedMarker, _ ->
                onMarkerClicked(lot, clickedMarker)
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

    private fun onMarkerClicked(lot: ParkingLot, marker: Marker) {
        // Deselect previous marker
        deselectAllMarkers()

        // Highlight selected marker (change icon or color if needed)
        // For now, we'll just show the bottom sheet

        // Show bottom sheet with lot details
        showBottomSheet(lot)

        // Center map on marker
        mapView.controller.animateTo(marker.position)
    }

    private fun deselectAllMarkers() {
        // Reset all markers to default state
        // Implementation depends on how you want to style selected vs unselected markers
    }

    override fun showBottomSheet(lot: ParkingLot) {
        selectedParkingLot = lot

        // Populate bottom sheet with lot details
        tvBottomSheetLotName.text = lot.name
        tvBottomSheetAddress.text = lot.address
        tvBottomSheetPrice.text = String.format("PHP %.2f per hour", lot.pricePerHour)

        // Calculate availability percentage and set color
        val availabilityPercentage = if (lot.totalSpots > 0) {
            lot.availableSpots.toDouble() / lot.totalSpots.toDouble()
        } else {
            0.0
        }

        tvBottomSheetAvailableSpots.text = "${lot.availableSpots} / ${lot.totalSpots} spots"

        when {
            availabilityPercentage > 0.2 -> {
                tvBottomSheetAvailableSpots.setTextColor(Color.parseColor("#4CAF50")) // Green
            }
            availabilityPercentage in 0.05..0.2 -> {
                tvBottomSheetAvailableSpots.setTextColor(Color.parseColor("#FFC107")) // Yellow
            }
            else -> {
                tvBottomSheetAvailableSpots.setTextColor(Color.parseColor("#F44336")) // Red
            }
        }

        // Show bottom sheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun hideBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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
