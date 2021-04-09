package com.example.weatherapp

// import kotlinx.android.synthetic.main.activity_main.*
import android.annotation.SuppressLint
import com.google.android.gms.maps.OnMapReadyCallback
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject
import java.lang.Exception
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    var CITY: String = ""
    val API: String = "f0a92827eecc3a9b8576601b278abdc1"

    lateinit var locationManager: LocationManager
    private var hasGps = false
    private var hasNetwork = false
    private var locationGps: Location? = null
    private var locationNetwork: Location? = null
    var latitudeGps: String? = null
    var longitudeGps: String? = null
    var latitudeNetwork: String? = null
    var longitudeNetwork: String? = null
    var latitude: String? = null
    var longitude: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
        findViewById<TextView>(R.id.error_text).visibility = View.GONE
        checkPermissions()


        findViewById<ImageView>(R.id.my_geo).setOnClickListener {
            getLocation()
            findViewById<RelativeLayout>(R.id.MapContainer).visibility = View.VISIBLE
            val supportMapFragment = (supportFragmentManager.findFragmentById(R.id.myMap) as
                    SupportMapFragment?)!!
            WeatherTaskPlaceholder().execute()
            supportMapFragment.getMapAsync(this)

        }

        findViewById<ImageView>(R.id.remove).setOnClickListener {
            findViewById<RelativeLayout>(R.id.MapContainer).visibility = View.GONE

        }

        findViewById<SearchView>(R.id.search).setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {

                CITY = query.toString()
                WeatherTask().execute()
                return true


            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }

        })


    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1
            )

        }
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                ), 1
            )
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (hasGps || hasNetwork) {
            if (hasGps) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0F, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (location != null) {
                            locationGps = location
                            latitudeGps = locationGps!!.latitude.toString()
                            longitudeGps = locationGps!!.longitude.toString()
                        }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

                    }

                    override fun onProviderEnabled(provider: String) {

                    }

                    override fun onProviderDisabled(provider: String) {

                    }
                })

                val localGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (localGpsLocation != null)
                    locationGps = localGpsLocation

            }

            if (hasNetwork) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0F, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (location != null) {
                            locationNetwork = location
                            latitudeNetwork = locationNetwork!!.latitude.toString()
                            longitudeNetwork = locationNetwork!!.longitude.toString()
                        }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

                    }

                    override fun onProviderEnabled(provider: String) {

                    }

                    override fun onProviderDisabled(provider: String) {

                    }
                })

                val localNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (localNetworkLocation != null)
                    locationNetwork = localNetworkLocation

            }

            if(locationGps!= null && locationNetwork!= null){
                if(locationGps!!.accuracy > locationNetwork!!.accuracy){
                    latitude = locationNetwork!!.latitude.toString()
                    longitude = locationNetwork!!.longitude.toString()
                }else{
                    latitude = locationGps!!.latitude.toString()
                    longitude = locationGps!!.longitude.toString()
                }
            }
        } else {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        val latLng = LatLng(locationGps!!.latitude, locationGps!!.longitude)
        val map = googleMap
        googleMap!!.isMyLocationEnabled = true

        val markerOptions = MarkerOptions().position(latLng).title("You are here!")
        // googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 5f))
        map?.addMarker(markerOptions)
        Toast.makeText(applicationContext, "Карта не отображается", Toast.LENGTH_SHORT).show()

    }




    inner class WeatherTask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.error_text).visibility = View.GONE

        }

        override fun doInBackground(vararg p0: String?): String? {
            var response : String?
            try {
              response = URL("https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&appid=$API")
               .readText(Charsets.UTF_8)
            } catch (e: Exception) {
                response = null
                Log.d("TAG", "AAAA")

            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result == null) {

                val toast: Toast = Toast.makeText(applicationContext, "Oops! Not found, learn geography!", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                return
            } else
            try {
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val updateAt: Long = jsonObj.getLong("dt")
                val updateAtText = "Updated at" + SimpleDateFormat("dd/MM/YYYY hh.mm a", Locale.ENGLISH)
                    .format(Date(updateAt*1000))
                val temp = main.getString("temp")+"°C"
                val tempMin = "Min temp: " + main.getString("temp_min")+"°C"
                val tempMax = "Max temp: " + main.getString("temp_max")+"°C"
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")
                val sunrise: Long = sys.getLong("sunrise")
                val sunset: Long = sys.getLong("sunset")
                val windSpeed = wind.getString("speed")
                val weatherDescription = weather.getString("description")
                val address = jsonObj.getString("name")

                findViewById<TextView>(R.id.address).text = address
                findViewById<TextView>(R.id.updated_at).text = updateAtText
                findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                findViewById<TextView>(R.id.temperature).text = temp
                findViewById<TextView>(R.id.max_temp).text = tempMax
                findViewById<TextView>(R.id.min_temp).text = tempMin
                findViewById<TextView>(R.id.humidity).text = humidity
                findViewById<TextView>(R.id.wind).text = windSpeed
                findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                    .format(Date(sunrise*1000))
                findViewById<TextView>(R.id.sunset).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                    .format(Date(sunset*1000))
                findViewById<TextView>(R.id.pressure).text = pressure

                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE

            }

            catch (e : Exception) {
                val toast: Toast = Toast.makeText(applicationContext, "Oops! Error, try again!", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()



            }
        }

    }

    inner class WeatherTaskPlaceholder() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.error_text).visibility = View.GONE

        }

        override fun doInBackground(vararg p0: String?): String? {
            var response : String?
            try {
                response = URL("https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&units=metric&appid=$API").readText(
                    Charsets.UTF_8
                )
            } catch (e: Exception) {
                response = null
                Log.d("TAG", "AAAA")

            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result == null) {

                val toast: Toast = Toast.makeText(applicationContext, "Oops! Not found, Where you?", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                return
            } else
                try {
                    val jsonObj = JSONObject(result)
                    val main = jsonObj.getJSONObject("main")
                    val sys = jsonObj.getJSONObject("sys")
                    val wind = jsonObj.getJSONObject("wind")
                    val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                    val updateAt: Long = jsonObj.getLong("dt")
                    val updateAtText = "Updated at" + SimpleDateFormat("dd/MM/YYYY hh.mm a", Locale.ENGLISH)
                        .format(Date(updateAt*1000))
                    val temp = main.getString("temp")+"°C"
                    val tempMin = "Min temp: " + main.getString("temp_min")+"°C"
                    val tempMax = "Max temp: " + main.getString("temp_max")+"°C"
                    val pressure = main.getString("pressure")
                    val humidity = main.getString("humidity")
                    val sunrise: Long = sys.getLong("sunrise")
                    val sunset: Long = sys.getLong("sunset")
                    val windSpeed = wind.getString("speed")
                    val weatherDescription = weather.getString("description")
                    val address = jsonObj.getString("name")

                    findViewById<TextView>(R.id.address).text = address
                    findViewById<TextView>(R.id.updated_at).text = updateAtText
                    findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                    findViewById<TextView>(R.id.temperature).text = temp
                    findViewById<TextView>(R.id.max_temp).text = tempMax
                    findViewById<TextView>(R.id.min_temp).text = tempMin
                    findViewById<TextView>(R.id.humidity).text = humidity
                    findViewById<TextView>(R.id.wind).text = windSpeed
                    findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                        .format(Date(sunrise*1000))
                    findViewById<TextView>(R.id.sunset).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                        .format(Date(sunset*1000))
                    findViewById<TextView>(R.id.pressure).text = pressure

                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE

                }

                catch (e : Exception) {
                    val toast: Toast = Toast.makeText(applicationContext, "Oops! Error, try again!", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()



                }
        }

    }
}