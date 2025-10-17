package com.example.linknpark.ui.staff.presenter

import com.example.linknpark.ui.staff.model.ParkingSpace

interface ParkingContract {
    interface View {
        fun showParkingSpaces(spaces: List<ParkingSpace>, rows: Int, cols: Int)
        fun updateSpace(space: ParkingSpace)
        fun showModifyDialog(currentRows: Int, currentCols: Int)
    }

    interface Presenter {
        fun loadParkingSpaces()
        fun toggleSpace(id: Int)
        fun updateParkingLayout(rows: Int, cols: Int)
        fun onModifyParkingClicked()
    }
}
