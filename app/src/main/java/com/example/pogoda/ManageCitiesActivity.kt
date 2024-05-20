package com.example.pogoda

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ManageCitiesActivity : AppCompatActivity() {
    private lateinit var citiesAdapter: ArrayAdapter<String>
    private val citiesList = mutableListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_cities)

        val listView: ListView = findViewById(R.id.listViewCities)
        val editText: EditText = findViewById(R.id.editTextCityName)
        val addButton: Button = findViewById(R.id.buttonAddCity)

        loadCities()

        citiesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, citiesList)
        listView.adapter = citiesAdapter

        addButton.setOnClickListener {
            val city = editText.text.toString().trim()
            val cityLower = city.lowercase()
            if (city.isNotEmpty()) {
                if (!citiesList.map { it.lowercase() }.contains(cityLower) && citiesList.size < 5) {
                    citiesList.add(city)
                    citiesAdapter.notifyDataSetChanged()
                    saveCities()
                    editText.setText("")
                } else {
                    if (citiesList.size >= 5) {
                        Toast.makeText(this, "Maks 5 miast.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Miasto jest już na liście.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        listView.setOnItemClickListener { adapterView, view, position, id ->
            val selectedCity = citiesList[position]
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("selectedCity", selectedCity)
            startActivity(intent)
        }
        listView.setOnItemLongClickListener { adapterView, view, position, id ->
            val city = citiesList[position]
            AlertDialog.Builder(this)
                .setTitle("Wybierz akcje")
                .setItems(arrayOf("Edytuj", "Usuń")) { dialog, which ->
                    when (which) {
                        0 -> showEditCityDialog(position)
                        1 -> {
                            AlertDialog.Builder(this)
                                .setTitle("Potwierdź usunięcie")
                                .setMessage("Czy na pewno chcesz usunąć $city?")
                                .setPositiveButton("Tak") { dialog, which ->
                                    citiesList.removeAt(position)
                                    citiesAdapter.notifyDataSetChanged()
                                    removeCity(normalizeCityName(city))
                                    saveCities()
                                    Toast.makeText(this, "$city usunięto", Toast.LENGTH_SHORT).show()
                                }
                                .setNegativeButton("Nie", null)
                                .show()
                        }
                    }
                }
                .show()
            true
        }
    }
    private fun normalizeCityName(city: String): String {
        val polishToEnglishMap = mapOf(
            'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n',
            'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z',
            'Ą' to 'A', 'Ć' to 'C', 'Ę' to 'E', 'Ł' to 'L', 'Ń' to 'N',
            'Ó' to 'O', 'Ś' to 'S', 'Ź' to 'Z', 'Ż' to 'Z'
        )

        return city.map { polishToEnglishMap[it] ?: it }.joinToString("")
    }
    private fun showEditCityDialog(position: Int) {
        val oldCity = citiesList[position]
        val editText = EditText(this)
        editText.setText(oldCity)

        AlertDialog.Builder(this)
            .setTitle("Edytuj")
            .setView(editText)
            .setPositiveButton("Zapisz") { dialog, which ->
                val newCity = editText.text.toString().trim()
                val newCityLower = newCity.lowercase()
                if (newCity.isNotEmpty()) {
                    if (!citiesList.map { it.lowercase() }.contains(newCityLower)) {
                        citiesList[position] = newCity
                        citiesAdapter.notifyDataSetChanged()
                        saveCities()
                        removeCity(normalizeCityName(oldCity))
                        Toast.makeText(this, "$oldCity zmieniono na $newCity", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Miasto jest już w liście.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
    private fun loadCities() {
        val prefs = getSharedPreferences("weather_prefs", MODE_PRIVATE)
        citiesList.addAll(prefs.getStringSet("cities", emptySet())!!)
    }

    private fun saveCities() {
        val prefs = getSharedPreferences("weather_prefs", MODE_PRIVATE).edit()
        prefs.putStringSet("cities", citiesList.toSet())
        prefs.apply()
    }
    private fun removeCityFromPreferences(cityName: String) {
        val prefs = getSharedPreferences("weather_prefs", MODE_PRIVATE)
        val editor = prefs.edit()
        val cities = prefs.getStringSet("cities", mutableSetOf())!!.toMutableSet()
        cities.remove(cityName)
        editor.putStringSet("cities", cities)
        editor.apply()
    }
    private fun deleteWeatherDataFiles(cityName: String) {
        val filesDir = applicationContext.filesDir
        val weatherFile = File(filesDir, "$cityName-weather.json")
        if (weatherFile.exists()) {
            weatherFile.delete()
        }

        val forecastFile = File(filesDir, "$cityName-forecast.json")
        if (forecastFile.exists()) {
            forecastFile.delete()
        }
    }
    private fun removeCity(cityName: String) {
        removeCityFromPreferences(cityName)
        deleteWeatherDataFiles(cityName)
    }
}

