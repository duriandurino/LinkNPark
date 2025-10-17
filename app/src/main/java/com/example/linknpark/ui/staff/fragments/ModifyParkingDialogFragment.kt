package com.example.linknpark.ui.staff.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.example.linknpark.R

class ModifyParkingDialogFragment : DialogFragment() {

    var onSave: ((rows: Int, cols: Int) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_modify_parking, container, false)
        val etRows = view.findViewById<EditText>(R.id.etRows)
        val etCols = view.findViewById<EditText>(R.id.etCols)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        val currentRows = arguments?.getInt("rows") ?: 12
        val currentCols = arguments?.getInt("cols") ?: 15
        etRows.setText(currentRows.toString())
        etCols.setText(currentCols.toString())

        btnSave.setOnClickListener {
            val newRows = etRows.text.toString().toIntOrNull() ?: currentRows
            val newCols = etCols.text.toString().toIntOrNull() ?: currentCols
            onSave?.invoke(newRows, newCols)
            dismiss()
        }

        return view
    }

    companion object {
        fun newInstance(rows: Int, cols: Int): ModifyParkingDialogFragment {
            val fragment = ModifyParkingDialogFragment()
            val args = Bundle()
            args.putInt("rows", rows)
            args.putInt("cols", cols)
            fragment.arguments = args
            return fragment
        }
    }
}
