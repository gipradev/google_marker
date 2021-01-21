package com.android.googlemarker

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*
import kotlin.collections.ArrayList


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private val SIZE_LIMIT: Int = 3
    private var pointerCounter: Int = 0
    private val TAG = this.javaClass.simpleName.toString()
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var mLocationPermissionsGranted = false
    private lateinit var mMap: GoogleMap
    var lat: Double? = null
    var lon: Double? = null
    var markerList: ArrayList<LatLng> = ArrayList()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1234
    private val DEFAULT_ZOOM = 15f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        Log.i(TAG, "MapsActivity onCreate")

        fabClear.enable(false)
        fabDraw.enable(false)
        buttonContinue.visible(false)
        fabMyLocation.enable(false)

        getLocationPermission()


    }

    //setPermission
    private fun getLocationPermission() {
        val permissions = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)

        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionsGranted = true
                initMap()

            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                this, permissions, LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }


    //onMapReady Function
    override fun onMapReady(googleMap: GoogleMap) {
        this.mMap = googleMap

        if (mLocationPermissionsGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return;
            }
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = false

        }

        mMap.setOnMapLongClickListener(this)

    }

    private fun checkClickables(fabDraw: MaterialButton?, fabClear: MaterialButton?) {
        if (markerList.size > 0) fabClear?.enable(true)
        else fabClear?.enable(true)


        if (markerList.size >= SIZE_LIMIT) fabDraw?.enable(true)
        else fabDraw?.enable(false)
    }

    //get Current Location
    private fun getDeviceLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (mLocationPermissionsGranted) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

            }
            fusedLocationClient?.lastLocation?.addOnSuccessListener {

                if (it != null) {
                    this.lat = it.latitude
                    this.lon = it.longitude
                    moveCamera(LatLng(it.getLatitude(), it.getLongitude()), DEFAULT_ZOOM)

                }
            }
        }
    }


    // Run time permisstion
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        mLocationPermissionsGranted = false
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.size > 0) {
                    for (i in 0 until grantResults.size) {
                        if (grantResults[i] !== PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false
                            return
                        }
                    }
                    mLocationPermissionsGranted = true
                    //initialize our map
                    initMap()
                }
            }
        }
    }

    //Map initialing
    private fun initMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fabMyLocation.enable(true)

    }


    private fun moveCamera(latLng: LatLng, zoom: Float) =
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))


    fun drawPolyGon(view: View) {

        val poly = PolygonOptions()
        for (latLng in markerList) {
            poly.addAll(Collections.singleton(latLng))
        }
        val polygon: Polygon = mMap.addPolygon(poly)
        buttonContinue.visible(true)
    }


    fun clearMarker(view: View) {
        mMap.clear()
        markerList.clear()
        checkClickables(fabDraw, fabClear)
        buttonContinue.visible(false)
        pointerCounter = 0
    }


    fun myLocation(view: View) {
        var latLng = lon?.let { lat?.let { it1 -> LatLng(it1, it) } }
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 30f)
        mMap.animateCamera(cameraUpdate)
    }

    override fun onMapLongClick(latLng: LatLng) {
        pointerCounter++
        val location = latLng
        markerList.add(location)
        checkClickables(fabDraw, fabClear)

        mMap.addMarker(MarkerOptions().position(location).title("Point $pointerCounter"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location))
    }

    fun SendToServer(view: View) {
        val  intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
     //   Toast.makeText(applicationContext, "Sending to sever", Toast.LENGTH_SHORT).show()
    }


    override fun onStart() {
        super.onStart()
        Log.i(TAG, "MapsActivity onStart")

    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "MapsActivity onResume")

    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "MapsActivity onPause")

    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "MapsActivity onStop")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "MapsActivity onDestroy")

    }


}