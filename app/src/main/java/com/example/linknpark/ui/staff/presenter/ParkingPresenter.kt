package com.example.linknpark.ui.staff.presenter

import com.example.linknpark.ui.staff.model.ParkingSpace

class ParkingPresenter(private val view: ParkingContract.View) : ParkingContract.Presenter {

    private var rows = 5
    private var cols = 8
    private val spaces = mutableListOf<ParkingSpace>()

    init {
        loadParkingSpaces()
    }

    override fun loadParkingSpaces() {
        spaces.clear()
        val total = rows * cols
        for (i in 0 until total) {
            spaces.add(ParkingSpace(i, false))
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
}
