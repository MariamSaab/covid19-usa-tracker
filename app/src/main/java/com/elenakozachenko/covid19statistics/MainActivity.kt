package com.elenakozachenko.covid19statistics

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://covidtracking.com/api/v1/"
private const val ALL_STATES = "All (Nationwide)"

class MainActivity : AppCompatActivity() {

    private lateinit var  currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)
        covidService.getNationalData().enqueue(object: Callback<List<CovidData>>{
            override fun onResponse(call: Call<List<CovidData>>, response: Response<List<CovidData>>) {
                Log.i(TAG, "onResponse $response")
                val nationalData = response.body()
                if (nationalData == null){
                    Log.w(TAG, "Did not receive a valid response")
                    return
                }
                setupEventListeners()
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, "Update graph with national data")
                updateDisplayWithData(nationalDailyData)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
               Log.e(TAG, "onFailer $t")
            }
        })

        covidService.getNationalData().enqueue(object: Callback<List<CovidData>>{

            override fun onResponse(call: Call<List<CovidData>>, response: Response<List<CovidData>>) {
                Log.i(TAG, "onResponse $response")
                val statesData = response.body()
                if (statesData == null){
                    Log.w(TAG, "Did not receive a valid response")
                    return
                }
                perStateDailyData = statesData.reversed().groupBy { it.state }
                Log.i(TAG, "Update spinner with state names")
                //updateSpinnerWithStateData(perStateDailyData.keys)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun setupEventListeners() {
     sparkView.isScrubEnabled  = true
        sparkView.setScrubListener {  itemData ->
            if(itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }
        radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when ( checkedId){

                R.id.radioButtonWeek -> TimeScale.WEEK
                R.id.radioButtonMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }
        radioGroupMetricSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonDeath -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        //update the color of the chart
        val colorRes = when (metric) {
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.POSITIVE -> R.color.colorPositive
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        sparkView.lineColor = colorInt
        tvMetricValue.setTextColor(colorInt)

        //updating metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true

        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        tvMetricValue.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }

}