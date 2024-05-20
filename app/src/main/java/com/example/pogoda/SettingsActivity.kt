package com.example.pogoda

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val spinnerTemperature = findViewById<Spinner>(R.id.spinnerTemperature)
        val spinnerWind = findViewById<Spinner>(R.id.spinnerWind)
        val spinnerPressure = findViewById<Spinner>(R.id.spinnerPressure)
        val spinnerDirection = findViewById<Spinner>(R.id.spinnerDirection)
        val spinnerVisibility = findViewById<Spinner>(R.id.spinnerVisibility)

        setupSpinner(spinnerTemperature, R.array.temperature_units_array)
        setupSpinner(spinnerWind, R.array.wind_units_array)
        setupSpinner(spinnerPressure, R.array.pressure_units_array)
        setupSpinner(spinnerDirection, R.array.wind_direction_array)
        setupSpinner(spinnerVisibility, R.array.visibility_unit_array)

        initializeSpinnerPosition(spinnerTemperature)
        initializeSpinnerPosition(spinnerWind)
        initializeSpinnerPosition(spinnerPressure)
        initializeSpinnerPosition(spinnerDirection)
        initializeSpinnerPosition(spinnerVisibility)
    }

    private fun setupSpinner(spinner: Spinner, arrayId: Int) {
        ArrayAdapter.createFromResource(
            this,
            arrayId,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (view != null) {
                    val selectedUnit = parent.getItemAtPosition(position).toString()
                    val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString(getPreferenceKey(parent.id), selectedUnit)
                        apply()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    private fun initializeSpinnerPosition(spinner: Spinner) {
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val key = getPreferenceKey(spinner.id)
        val selectedValue = sharedPref.getString(key, null) ?: return

        val adapter = spinner.adapter
        for (position in 0 until adapter.count) {
            if (adapter.getItem(position).toString() == selectedValue) {
                spinner.setSelection(position)
                break
            }
        }
    }

    private fun getPreferenceKey(spinnerId: Int): String {
        return when (spinnerId) {
            R.id.spinnerTemperature -> "TemperatureUnit"
            R.id.spinnerWind -> "WindUnit"
            R.id.spinnerPressure -> "PressureUnit"
            R.id.spinnerDirection -> "DirectionUnit"
            R.id.spinnerVisibility -> "VisibilityUnit"
            else -> ""
        }
    }
}
