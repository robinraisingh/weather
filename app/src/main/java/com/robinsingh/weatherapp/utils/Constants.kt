package com.robinsingh.weatherapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {

    const val BASE_URL:String="http://api.openweathermap.org/data/"
    const val API_KEY:String="ad09b608664c42adacfbeb5c79de5278"
    const val METRIC_UNIT:String="metric"
    const val WEATHER_RESPONSE_DATA:String="weather_response_data"
    const val PREFERENCE_NAME:String="shared_preference_name"

    /**
     * This function is used check the weather the device is connected to the Internet or not.
     */

    //this method needs to be called where we need to check the internet connectivity

    fun isNetworkAvailable(context: Context): Boolean {
        // It answers the queries about the state of network connectivity.
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network      = connectivityManager.activeNetwork ?: return false  //return false if not exist
            val activeNetWork = connectivityManager.getNetworkCapabilities(network) ?: return false  //return false if not exist
            return when {
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            //the below written method is to check version for sdk<23
            //it is deprecated in version>23
            // Returns details about the currently active default data network.
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}