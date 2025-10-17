package com.example.linknpark.ui.staff.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import androidx.fragment.app.Fragment
import com.example.linknpark.R
import com.example.linknpark.ui.staff.model.ParkingSpace
import com.example.linknpark.ui.staff.presenter.ParkingContract
import com.example.linknpark.ui.staff.presenter.ParkingPresenter

class ParkingFragment : Fragment(), ParkingContract.View {

    lateinit var presenter: ParkingPresenter
    private lateinit var gridLayout: GridLayout
    private val buttons = mutableListOf<Button>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_parking, container, false)
        gridLayout = view.findViewById(R.id.gridParking)

        presenter = ParkingPresenter(this)
        presenter.loadParkingSpaces()
        return view
    }

    override fun showParkingSpaces(spaces: List<ParkingSpace>, rows: Int, cols: Int) {
        gridLayout.removeAllViews()
        gridLayout.columnCount = cols
        gridLayout.rowCount = rows
        buttons.clear()

        for (space in spaces) {
            val button = Button(requireContext())
            button.text = space.id.toString()
            button.setBackgroundColor(if (space.isOccupied) Color.RED else Color.GREEN)
            button.setOnClickListener { presenter.toggleSpace(space.id) }

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = 0
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(4, 4, 4, 4)

            button.layoutParams = params
            gridLayout.addView(button)
            buttons.add(button)
        }
    }

    override fun updateSpace(space: ParkingSpace) {
        val button = buttons.getOrNull(space.id) ?: return
        button.setBackgroundColor(if (space.isOccupied) Color.RED else Color.GREEN)
    }

    override fun showModifyDialog(currentRows: Int, currentCols: Int) {
        val dialog = ModifyParkingDialogFragment.newInstance(currentRows, currentCols)
        dialog.onSave = { rows, cols ->
            presenter.updateParkingLayout(rows, cols)
        }
        dialog.show(parentFragmentManager, "ModifyParkingDialog")
    }
}
