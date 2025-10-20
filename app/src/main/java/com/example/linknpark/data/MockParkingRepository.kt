package com.example.linknpark.data

import com.example.linknpark.model.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MockParkingRepository : ParkingRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())

    override suspend fun getLiveParkingStatus(): Result<ParkingApiResponse> {
        // Simulate network delay
        delay(500)
        
        return try {
            val mockData = generateMockData()
            Result.success(mockData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshParkingStatus(): Result<ParkingApiResponse> {
        // Simulate network delay for refresh
        delay(800)
        
        return try {
            val mockData = generateMockData()
            Result.success(mockData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateMockData(): ParkingApiResponse {
        val timestamp = dateFormat.format(Date())
        
        // Generate 17 active cars (similar to provided data)
        val activeCars = listOf(
            ActiveCar(898, "UNKNOWN", Coordinates(111, 213)),
            ActiveCar(905, "UNKNOWN", Coordinates(208, 129)),
            ActiveCar(781, "UNKNOWN", Coordinates(154, 167)),
            ActiveCar(782, "UNKNOWN", Coordinates(165, 200)),
            ActiveCar(783, "UNKNOWN", Coordinates(220, 195)),
            ActiveCar(720, "UNKNOWN", Coordinates(42, 180)),
            ActiveCar(752, "UNKNOWN", Coordinates(62, 211)),
            ActiveCar(722, "UNKNOWN", Coordinates(50, 350)),
            ActiveCar(785, "UNKNOWN", Coordinates(197, 162)),
            ActiveCar(911, "UNKNOWN", Coordinates(249, 183)),
            ActiveCar(915, "UNKNOWN", Coordinates(164, 287)),
            ActiveCar(790, "UNKNOWN", Coordinates(265, 294)),
            ActiveCar(917, "UNKNOWN", Coordinates(73, 275)),
            ActiveCar(919, "UNKNOWN", Coordinates(155, 279)),
            ActiveCar(761, "UNKNOWN", Coordinates(236, 160)),
            ActiveCar(730, "UNKNOWN", Coordinates(73, 178)),
            ActiveCar(412, "UNKNOWN", Coordinates(226, 336))
        )

        // Generate parking spots (let's create a 5x6 grid = 30 spots)
        val parkingSpots = mutableListOf<ParkingSpot>()
        val occupiedSpotIds = listOf(3, 4, 7, 12, 15, 18, 21, 23, 27, 29) // 10 occupied spots
        
        for (i in 1..30) {
            if (occupiedSpotIds.contains(i)) {
                // Occupied spot with car
                val carId = 100 + i
                val label = if (i == 3) "DBA 3163" 
                           else if (i == 4) "UNKNOWN"
                           else if (i % 3 == 0) "ABC ${1000 + i}"
                           else if (i % 2 == 0) "XYZ ${2000 + i}"
                           else "UNKNOWN"
                
                parkingSpots.add(
                    ParkingSpot(
                        spot_id = i,
                        occupied = true,
                        current_car = CurrentCar(
                            car_id = carId,
                            label = label,
                            coordinates = Coordinates(
                                Random.nextInt(50, 300),
                                Random.nextInt(100, 400)
                            )
                        ),
                        history = generateHistory(i)
                    )
                )
            } else {
                // Available spot
                parkingSpots.add(
                    ParkingSpot(
                        spot_id = i,
                        occupied = false,
                        current_car = null,
                        history = if (Random.nextBoolean()) generateHistory(i) else emptyList()
                    )
                )
            }
        }

        return ParkingApiResponse(
            timestamp = timestamp,
            total_entries = Random.nextInt(50, 100),
            total_exits = Random.nextInt(45, 95),
            all_active_cars = activeCars,
            parking_spots = parkingSpots
        )
    }

    private fun generateHistory(spotId: Int): List<Int> {
        val historySize = Random.nextInt(0, 5)
        return List(historySize) { Random.nextInt(100, 999) }
    }
}

