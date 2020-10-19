package com.robinsingh.weatherapp.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem

import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.robinsingh.weatherapp.R
import com.robinsingh.weatherapp.models.WeatherResponse
import com.robinsingh.weatherapp.networks.WeatherService
import com.robinsingh.weatherapp.utils.Constants
import kotlinx.android.synthetic.main.activity_main.*
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    //making a variable for permission request
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mCustomDialog:Dialog?=null

    private lateinit var mSharedPreferences:SharedPreferences
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSharedPreferences=getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE)
        //Initialize the fusedLocationProviderClient variable
        // START
        // Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // END
        setupUI()
/** IF THE LOCATION IS NOT ENABLED THE REDIRECT USER TO SETTING SO HE OR SHE CAN DIRECTLY TURN THE LOCATION ON*/
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            // This will redirect you to settings from where you need to turn on the location provider.
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            /**
             * IN THIS BLOCK OF CODE WE WILL USE AN EXTERNAL LIBRARY DEXTOR TO ASK THE MULTIPLE PERMISSION
             * THAT ARE REQUIRED IN THE RUN TIME. IN THE METHOD WE CALL BELOW THE ANONYMOUS OBJECT
             * NEED TO OVER RIDE TWO FUNCTION
             * 1->  IF THE PERMISSION GRANTED OR DENIED
             * 2-> FROM NEXT TIME WHAT IT SHOULD DO IF THE PERMISSION DENIED
             */
            //start
            Dexter.withActivity(this).withPermissions(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION).withListener(
                //inside listener make an object: MultiplePermissionsListener which need to over ride two methods as anonymous
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if(report!!.areAllPermissionsGranted()){

                            requestLocationData()

                        }
                        if(report!!.isAnyPermissionPermanentlyDenied){
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }

                }
            ).onSameThread().check()//end


        }

    }
    /**
     * the following finction is or a dialog that will appear if the user once denied the
     * request of the permission and then try to use the app
     */
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS"){ _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)//this will take user to our application context settings
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    /**
     * the given function below are compulsory to get the location
     * which itself need an object of LocationCallBack class and we need to over ride method as anonymous
     *
     * A function to request the current location. Using the fused location provider client.
     *
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }
    /**
     * A location callback object of fused location provider client where we will get the current location details.
     *
     */
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
    // END

    /**
     * Function is used to get the weather details of the current location based on the latitude longitude
     */
    private fun getLocationWeatherDetails(latitude:Double,longitude:Double) {
        // Here we will check whether the internet
        // connection is available or not using the method which
        // we have created in the Constants object
        // START
        if (Constants.isNetworkAvailable(this@MainActivity)) {

            //here we are building a retrofit request to our url
            val retrofit:Retrofit=Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            //preparing service for calling the api
            val service:WeatherService=retrofit
                        .create<WeatherService>(WeatherService::class.java)//using our interface and retrofit object

            val listCall: Call<WeatherResponse> = service.getWeather(        //calling getWeather method from interface that we made
                latitude,longitude,Constants.METRIC_UNIT,Constants.API_KEY
            )

            showCustomDialog()

            listCall.enqueue(object:Callback<WeatherResponse>{
                @RequiresApi(Build.VERSION_CODES.N)
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    if(response!!.isSuccess){
                        hideCustomDialog()
                        val weatherList:WeatherResponse=response.body()   //this body is the actual json file that wwe need

                        val weatherResponseJsonString= Gson().toJson(weatherList)
                        val editor=mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                        editor.apply()
                        setupUI()
                        Log.i("response result","$weatherList")
                    }
                    else{
                    val rc=response.code()
                        when(rc){
                        400->{
                            Log.i("Error 400","Bad Connection")
                        }
                        404->{
                            Log.i("Error 404","Not Found")
                        }

                         else->{
                             Log.i("Error","unknown")
                         }
                        }
                    }
                }

                override fun onFailure(t: Throwable?) {
                    hideCustomDialog()
                    Log.i("Error","unknown error")
                }

            })

        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
        // END

    /**
     * check permission of location function
     * this is generally the first step in this kind of apps where location need to be enabled
     * enabling location does not mean that the user has given the permission to access the location
     * that we need to call explicity.
     */

    private fun isLocationEnabled(): Boolean {
        // This provides access to the system location services.
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun showCustomDialog(){

        mCustomDialog= Dialog(this)
        mCustomDialog!!.setContentView(R.layout.dialog_custom_progress)
        mCustomDialog!!.show()
    }
    private fun hideCustomDialog(){
        mCustomDialog?.dismiss()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupUI() {

        // For loop to get the required data. And all are populated in the UI.
        val weatherResponseJsonString=mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA,"")
        if(!weatherResponseJsonString.isNullOrEmpty()){
            val weatherList=Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)
            for (z in weatherList.weather.indices) {
                Log.i("NAME", weatherList.weather[z].main)

                tv_main.text = weatherList.weather[z].main
                tv_main_description.text = weatherList.weather[z].description
                tv_temp.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
                tv_humidity.text = weatherList.main.humidity.toString() + " per cent"
                tv_min.text = weatherList.main.temp_min.toString() + " min"
                tv_max.text = weatherList.main.temp_max.toString() + " max"
                tv_speed.text = weatherList.wind.speed.toString()
                tv_name.text = weatherList.name
                tv_country.text = weatherList.sys.country
                tv_sunrise_time.text = unixTime(weatherList.sys.sunrise.toLong())
                tv_sunset_time.text = unixTime(weatherList.sys.sunset.toLong())

                // Here we update the main icon
                when (weatherList.weather[z].icon) {
                    "01d" -> iv_main.setImageResource(R.drawable.sunny)
                    "02d" -> iv_main.setImageResource(R.drawable.cloud)
                    "03d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10d" -> iv_main.setImageResource(R.drawable.rain)
                    "11d" -> iv_main.setImageResource(R.drawable.storm)
                    "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                    "01n" -> iv_main.setImageResource(R.drawable.cloud)
                    "02n" -> iv_main.setImageResource(R.drawable.cloud)
                    "03n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10n" -> iv_main.setImageResource(R.drawable.cloud)
                    "11n" -> iv_main.setImageResource(R.drawable.rain)
                    "13n" -> iv_main.setImageResource(R.drawable.snowflake)
                }
            }



        }


    }
    /**
     * Function is used to get the temperature unit value.
     */
    private fun getUnit(value: String): String? {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }
    /**
     * The function is used to get the formatted time based on the Format and the LOCALE we pass to it.
     */
    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm",Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
    //to inflate the menu icon that we have created for refresh button
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            // START
            R.id.action_refresh -> {
                requestLocationData()

                true
            }
            else -> super.onOptionsItemSelected(item)
            // END
        }
    }


}