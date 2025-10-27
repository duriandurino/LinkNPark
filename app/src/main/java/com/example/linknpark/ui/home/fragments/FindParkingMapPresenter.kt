package com.example.linknpark.ui.home.fragments

import android.location.Location
import android.util.Log
import com.example.linknpark.data.FirebaseDriverRepository
import com.example.linknpark.model.ParkingLot
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class FindParkingMapPresenter : FindParkingMapContract.Presenter {

    private var view: FindParkingMapContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val repository = FirebaseDriverRepository()
    private var userId: String = ""
    private var allParkingLots = listOf<ParkingLot>()
    private val TAG = "FindParkingMapPresenter"

    override fun attach(view: FindParkingMapContract.View, userId: String) {
        this.view = view
        this.userId = userId
        loadParkingLots()
    }

    override fun detach() {
        repository.removeParkingLotListener()
        presenterScope.cancel()
        view = null
    }

    override fun loadParkingLots() {
        Log.d(TAG, "Loading parking lots")
        view?.showLoading(true)

        repository.observeParkingLots { lots ->
            Log.d(TAG, "Received ${lots.size} parking lots")
            allParkingLots = lots
            view?.displayParkingLots(lots)
            view?.showLoading(false)
        }
    }

    override fun searchParkingLots(query: String) {
        Log.d(TAG, "Searching parking lots with query: $query")

        presenterScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    repository.searchParkingLots(query)
                }

                Log.d(TAG, "Search returned ${results.size} results")

                if (query.isNotEmpty() && results.isEmpty()) {
                    view?.showNoResultsMessage(query)
                    // Keep all markers visible when no results
                    view?.displayParkingLots(allParkingLots)
                } else {
                    view?.displayParkingLots(results)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error searching parking lots", e)
                view?.showError("Error searching parking lots")
            }
        }
    }

    override fun requestUserLocation() {
        Log.d(TAG, "Requesting user location")
        // User location is handled by MyLocationNewOverlay in the fragment
        // This method is called to trigger location permission check
        // The actual location centering is done by the overlay
    }
}
