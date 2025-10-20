package com.example.linknpark.data

import com.example.linknpark.model.ParkingApiResponse

interface ParkingRepository {
    suspend fun getLiveParkingStatus(): Result<ParkingApiResponse>
    suspend fun refreshParkingStatus(): Result<ParkingApiResponse>
}

