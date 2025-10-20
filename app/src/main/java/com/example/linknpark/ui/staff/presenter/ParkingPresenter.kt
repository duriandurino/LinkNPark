package com.example.linknpark.ui.staff.presenter

import com.example.linknpark.data.MockParkingRepository
import com.example.linknpark.data.ParkingRepository
import com.example.linknpark.model.ParkingSpace
import kotlinx.coroutines.*

class ParkingPresenter(
    private val view: ParkingContract.View,
    private val repository: ParkingRepository = MockParkingRepository()
) : ParkingContract.Presenter {

    private var rows = 5
    private var cols = 6
    private val spaces = mutableListOf<ParkingSpace>()

    // Coroutine scope for async operations
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Load mock parking spaces initially
        loadParkingSpaces()
        // Then load live data from API
        loadLiveParkingStatus()
    }

    override fun loadParkingSpaces() {
        spaces.clear()
        val total = rows * cols
        for (i in 0 until total) {
            spaces.add(ParkingSpace(i, false))
        }
        view.showParkingSpaces(spaces, rows, cols)
    }

    override fun loadLiveParkingStatus() {
        presenterScope.launch {
            view.showLoading(true)

            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getLiveParkingStatus()
                }

                result.onSuccess { response ->
                    view.showLoading(false)
                    view.showParkingData(response)
                    view.showParkingSpots(response.parking_spots)

                    // Update statistics
                    val totalSpots = response.parking_spots.size
                    val occupiedSpots = response.parking_spots.count { it.occupied }
                    val availableSpots = totalSpots - occupiedSpots

                    view.updateStatistics(
                        totalSpots = totalSpots,
                        occupied = occupiedSpots,
                        available = availableSpots,
                        entries = response.total_entries,
                        exits = response.total_exits
                    )

                    // Convert API spots to ParkingSpace for grid display
                    updateParkingGrid(response.parking_spots)
                }.onFailure { error ->
                    view.showLoading(false)
                    view.showError(error.message ?: "Failed to load parking data")
                }
            } catch (e: Exception) {
                view.showLoading(false)
                view.showError("Error: ${e.message}")
            }
        }
    }

    override fun refreshParkingStatus() {
        presenterScope.launch {
            view.showLoading(true)

            try {
                val result = withContext(Dispatchers.IO) {
                    repository.refreshParkingStatus()
                }

                result.onSuccess { response ->
                    view.showLoading(false)
                    view.showParkingData(response)
                    view.showParkingSpots(response.parking_spots)

                    val totalSpots = response.parking_spots.size
                    val occupiedSpots = response.parking_spots.count { it.occupied }
                    val availableSpots = totalSpots - occupiedSpots

                    view.updateStatistics(
                        totalSpots = totalSpots,
                        occupied = occupiedSpots,
                        available = availableSpots,
                        entries = response.total_entries,
                        exits = response.total_exits
                    )

                    updateParkingGrid(response.parking_spots)
                }.onFailure { error ->
                    view.showLoading(false)
                    view.showError(error.message ?: "Failed to refresh parking data")
                }
            } catch (e: Exception) {
                view.showLoading(false)
                view.showError("Refresh error: ${e.message}")
            }
        }
    }

    private fun updateParkingGrid(apiSpots: List<com.example.linknpark.model.ParkingSpot>) {
        spaces.clear()

        // Calculate grid dimensions based on number of spots
        val totalSpots = apiSpots.size
        cols = 6 // Keep 6 columns
        rows = (totalSpots + cols - 1) / cols // Calculate rows needed

        // Convert API spots to ParkingSpace
        apiSpots.forEach { apiSpot ->
            spaces.add(
                ParkingSpace(
                    id = apiSpot.spot_id - 1, // Convert to 0-based index
                    isOccupied = apiSpot.occupied
                )
            )
        }

        view.showParkingSpaces(spaces, rows, cols)
    }

    override fun toggleSpace(id: Int) {
        val space = spaces.find { it.id == id } ?: return
        space.isOccupied = !space.isOccupied
        view.updateSpace(space)
    }

    override fun updateParkingLayout(rows: Int, cols: Int) {
        this.rows = rows
        this.cols = cols
        loadParkingSpaces()
    }

    override fun onModifyParkingClicked() {
        view.showModifyDialog(rows, cols)
    }

    override fun onDestroy() {
        presenterScope.cancel()
    }
}
