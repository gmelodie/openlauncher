package com.openlauncher.app.util

const val MPS_TO_KMH = 3.6f
const val MPS_TO_MPH = 2.236936f
const val METERS_TO_FEET = 3.28084
const val METERS_TO_MILES = 1609.344

fun Float.speedIn(metric: Boolean): Float = this * if (metric) MPS_TO_KMH else MPS_TO_MPH

fun Double.speedIn(metric: Boolean): Double = this * if (metric) MPS_TO_KMH.toDouble() else MPS_TO_MPH.toDouble()
