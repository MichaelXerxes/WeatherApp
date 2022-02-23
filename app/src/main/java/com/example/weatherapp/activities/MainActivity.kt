package com.example.weatherapp.activities

//import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherapp.R


import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.example.weatherapp.utils.Constants
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener.Builder.withContext

import retrofit.*
import java.text.SimpleDateFormat
import java.util.*


import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding?=null
    private lateinit var mFusedLocationClient:FusedLocationProviderClient
    private lateinit var mSharedPReferences:SharedPreferences

    private var mProgressDialog: Dialog?=null
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        mFusedLocationClient=LocationServices.getFusedLocationProviderClient(this)
        mSharedPReferences=getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE)

        setupUI()

        if(!isLocationEnabled()){
            Toast.makeText(this,"Location not found, Please Turn on GPS",Toast.LENGTH_SHORT).show()

            val intent= Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
            Dexter.withActivity (this).withPermissions(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport){
                    if(report!!.areAllPermissionsGranted()){

                        requestLocationData()

                    }
                }
                override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>,
                                                                token: PermissionToken
                ){
                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()
        }
    }
    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this).setMessage("" +
                "You have Turned-Off permission required for this feature." +
                " You can enabled under Application Settings")
            .setPositiveButton("Go To Settings")
            { _,_ ->
                try {
                    val intent=Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri= Uri.fromParts("package",packageName,null)
                    intent.data=uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){dialog , _ ->
                dialog.dismiss()
            }.show()

    }


    private fun isLocationEnabled():Boolean{

        val locationManager:LocationManager=
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER )
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")


            getLocationWeatherDetails(latitude,longitude)
        }
    }
    private fun getLocationWeatherDetails(latitude:Double,longitude:Double){


        if (Constants.isNetworkAvailable(this@MainActivity)) {
            //1
            val retrofit : Retrofit =Retrofit.Builder().baseUrl(Constants.BASE_URL).
            addConverterFactory(GsonConverterFactory.create()).build()
            //2
            val service:WeatherService=retrofit.
            create<WeatherService>(WeatherService::class.java)
            //3
            val listCall:Call<WeatherResponse> =service.getWeather(
                latitude,longitude,Constants.METRIC_UNIT,Constants.APP_ID
            )

            showCustomProgressDialog()

            listCall.enqueue(object :Callback<WeatherResponse>{
                @RequiresApi(Build.VERSION_CODES.N)
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    if (response!!.isSuccess){

                        hideCustomProgressDialog()

                        val weatherList:WeatherResponse=response.body()
                        //setupUI(weatherList) <- before
                        //shared preferences

                        val weatherResponseJsonStrinng=Gson().toJson(weatherList)

                        val editor=mSharedPReferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonStrinng)
                        editor.apply()

                        setupUI()

                        Log.i("Response Result","$weatherList")


                    }else{
                        val rc=response.code()
                        when(rc){
                            400 -> {
                                Log.e("Error 400", "Bad Connection")
                            }
                            404 -> {
                                Log.e("Error 404","Not found")
                            }
                            else -> {
                                Log.e("Error X","Error X and Lol")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable?) {
                    Log.e("Error",t!!.message.toString())
                    hideCustomProgressDialog()
                }

            })



           /* Toast.makeText(
                this@MainActivity,
                "You have connected to the internet. Now you can make an api call.",
                Toast.LENGTH_SHORT
            ).show()

            */
        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    ///
    private fun showCustomProgressDialog(){
        mProgressDialog= Dialog(this)

        mProgressDialog!!.setContentView(R.layout.custom_dialog)
        mProgressDialog!!.show()
    }
    private fun hideCustomProgressDialog(){
        if(mProgressDialog!=null){
            mProgressDialog!!.dismiss()
        }
    }
    ///
    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupUI(){
        val weatherResponseJsonString=mSharedPReferences.getString(Constants.WEATHER_RESPONSE_DATA,"")
        if ( !weatherResponseJsonString.isNullOrEmpty()){
            //converting
            val weatherList=Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)

            for(z in weatherList.weather.indices){
                Log.i("Weather Name",weatherList.weather.toString())
                binding?.tvMain?.text = weatherList.weather[z].main
                binding?.tvMainDescription?.text = weatherList.weather[z].description
                binding?.tvTemp?.text =
                    weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())

                binding?.tvHumidity?.text = weatherList.main.humidity.toString() + " per cent"
                binding?.tvMin?.text = weatherList.main.tempMin.toString() + " min"
                binding?.tvMax?.text = weatherList.main.tempMax.toString() + " max"
                binding?.tvSpeed?.text = weatherList.wind.speed.toString()
                binding?.tvName?.text = weatherList.name
                binding?.tvCountry?.text = weatherList.sys.country
                binding?.tvSunriseTime?.text = unixTime(weatherList.sys.sunrise.toLong())
                binding?.tvSunsetTime?.text = unixTime(weatherList.sys.sunset.toLong())

                // Here we update the main icon
                when (weatherList.weather[z].icon) {
                    "01d" ->  binding?.ivMain?.setImageResource(R.drawable.sunny)
                    "02d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "03d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "04d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "04n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "10d" -> binding?.ivMain?.setImageResource(R.drawable.rain)
                    "11d" -> binding?.ivMain?.setImageResource(R.drawable.storm)
                    "13d" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
                    "01n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "02n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "03n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "10n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "11n" -> binding?.ivMain?.setImageResource(R.drawable.rain)
                    "13n" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
                }

            }
        }

    }
    private fun getUnit(value: String): String? {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }
    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh ->{
                requestLocationData()
                true
            }else -> return super.onOptionsItemSelected(item)
        }

    }

}