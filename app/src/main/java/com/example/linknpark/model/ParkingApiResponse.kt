package com.example.linknpark.model

data class ParkingApiResponse(
    val timestamp: String,
    val total_entries: Int,
    val total_exits: Int,
    val all_active_cars: List<ActiveCar>,
    val parking_spots: List<ParkingSpot>
)

data class ActiveCar(
    val car_id: Int,
    val label: String,
    val coordinates: Coordinates
)

data class Coordinates(
    val x: Int?,
    val y: Int?
)

data class ParkingSpot(
    val spot_id: Int,
    val occupied: Boolean,
    val current_car: CurrentCar?,
    val history: List<Int>
)

data class CurrentCar(
    val car_id: Int,
    val label: String,
    val coordinates: Coordinates
)

// Summary data for dashboard
data class ParkingSummary(
    val totalSpots: Int,
    val occupiedSpots: Int,
    val availableSpots: Int,
    val totalEntries: Int,
    val totalExits: Int,
    val activeCarsCount: Int
)

