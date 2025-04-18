package com.example.spurthi25

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var qrCodeEditText: EditText
    private lateinit var scanButton: Button
    private lateinit var doneButton: Button
    private lateinit var detailsBox: TextView
    private lateinit var eventsContainer: LinearLayout
    private lateinit var scannerLauncher: ActivityResultLauncher<Intent>

    private val appScriptUrl =
        "https://script.google.com/macros/s/AKfycbwkwqkSf5LJOe3tjefM9B1j19O4knWgnLUEmS89rWqj9ZPqCiQy7aHzn1LqaLLoV_FvxA/exec"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        qrCodeEditText = findViewById(R.id.barcode)
        scanButton = findViewById(R.id.scanButton)
        doneButton = findViewById(R.id.doneButton) // Initialize Done button
        detailsBox = findViewById(R.id.detailsBox)
        eventsContainer = findViewById(R.id.eventsContainer)

        // Register activity result for ScannerActivity
        scannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scannedData = result.data?.getStringExtra("SCANNED_RESULT")
                qrCodeEditText.setText(scannedData)
                scannedData?.let { fetchDataFromScript(it) }
            }
        }

        // Start ScannerActivity for result
        scanButton.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            scannerLauncher.launch(intent)
        }

        // Clear all fields when Done is clicked
        doneButton.setOnClickListener {
            clearFields()
        }
    }

    private fun fetchDataFromScript(barcode: String) {
        val client = OkHttpClient()
        val requestUrl = "$appScriptUrl?action=getFilteredItems&barcodeno=$barcode"

        val request = Request.Builder()
            .url(requestUrl)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to fetch data", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("MainActivity", "Error fetching data: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseData = responseBody.string()
                    try {
                        val jsonResponse = JSONObject(responseData)
                        val itemsArray = jsonResponse.getJSONArray("items")

                        if (itemsArray.length() > 0) {
                            val item = itemsArray.getJSONObject(0)
                            val name = item.getString("name")
                            val regdno = item.getString("regdno")
                            val email = item.getString("email")
                            val branch = item.getString("branch")
                            val events = item.getJSONArray("events")

                            val details =
                                "\tName: $name\n\tReg No: $regdno\n\tEmail: $email\n\tBranch: $branch"
                            runOnUiThread {
                                detailsBox.text = details
                                displayEvents(events)
                            }
                        } else {
                            runOnUiThread {
                                detailsBox.text = "No records found"
                                eventsContainer.removeAllViews()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error processing data", Toast.LENGTH_SHORT)
                                .show()
                            Log.e("MainActivity", "JSON Parsing Error: ${e.message}")
                        }
                    }
                }
            }
        })
    }

    private fun displayEvents(events: org.json.JSONArray) {
        runOnUiThread {
            eventsContainer.removeAllViews()
            val eventsText = StringBuilder("")

            for (i in 0 until events.length()) {
                val eventName = events.getString(i)
                eventsText.append("$eventName\n\n")
            }

            val textView = TextView(this).apply {
                text = eventsText.toString()
                setTextColor(resources.getColor(R.color.white, theme))
                textSize = 20f
            }

            eventsContainer.addView(textView)
        }
    }

    private fun clearFields() {
        qrCodeEditText.text.clear()
        detailsBox.text = ""
        eventsContainer.removeAllViews()
    }
}
