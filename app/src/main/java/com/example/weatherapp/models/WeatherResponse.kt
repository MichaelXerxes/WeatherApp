package com.example.weatherapp.models

import android.media.MicrophoneInfo

data class WeatherResponse (
    val coordinate: Coordinate,
    val weather:List<Weather>,
    val base:String,
    val main: Main,
    val visibility:Int,
    val wind: Wind,
    val clouds:Clouds,
    val dt:Int,
    val sys:Sys,
    val id :Int,
    val name:String,
    val cod:Int
        )