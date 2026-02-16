package com.example.rcslocationtracking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.maptiler.sdk.Maptiler
import com.maptiler.sdk.maps.MapView
import com.maptiler.sdk.maps.MapLibreMap

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var maptilerMap: MapLibreMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialize Maptiler SDK with your API key
        Maptiler.getInstance().setApiKey("YOUR_MAPTILER_API_KEY")

        mapView = findViewById(R.id.mapView)
        mapView.getMapAsync { map ->
            maptilerMap = map
            // Map is ready, configure it here
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}