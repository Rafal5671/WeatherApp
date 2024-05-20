package com.example.pogoda

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var citiesAdapter: ArrayAdapter<String>
    private lateinit var manageCitiesButton: Button
    private val citiesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listViewCities)

        citiesAdapter = ArrayAdapter(this, R.layout.spinner_item, citiesList)
        listView.adapter = citiesAdapter
        loadCities()
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCity = citiesList[position]
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("selectedCity", selectedCity)
            startActivity(intent)
        }
        manageCitiesButton = findViewById(R.id.btnManageCities)
        manageCitiesButton.setOnClickListener {
            val intent = Intent(this, ManageCitiesActivity::class.java)
            startActivity(intent)
        }
    }
    override fun onResume() {
        super.onResume()
        loadCities()
    }
    private fun loadCities() {
        val prefs = getSharedPreferences("weather_prefs", MODE_PRIVATE)
        val newCitiesSet = prefs.getStringSet("cities", emptySet())!!

        if (newCitiesSet != citiesList.toSet()) {
            citiesList.clear()
            citiesList.addAll(newCitiesSet)
            citiesList.sort()
            citiesAdapter.notifyDataSetChanged()
        }
    }

}
