package com.karl.kiosk

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.circularreveal.CircularRevealWidget
import android.support.v7.widget.RecyclerView
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.karl.kiosk.shared.preferences.session
import com.valdesekamdem.library.mdtoast.MDToast

class SettingsActivity : AppCompatActivity() {

    private lateinit var session: session

    private lateinit var capture_percentage: TextView
    private lateinit var percentage_seekBar: SeekBar
    private lateinit var location_switch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        session = session(this)

        capture_percentage = findViewById(R.id.capture_percentage)
        percentage_seekBar = findViewById(R.id.percentage_seekBar)
        location_switch = findViewById(R.id.location_switch)

        //getActionBar().title = "Settings"
        supportActionBar!!.title = "Settings"

    }

    override fun onStart() {
        super.onStart()

        val current_percentage = session.getCapturePercentage().toInt()
        val checkLocation = session.CheckLocation()

        percentage_seekBar.progress = current_percentage
        capture_percentage.text = "$current_percentage%"
        if(checkLocation == "true") location_switch.setChecked(true) else location_switch.setChecked(false)
        location_switch.isEnabled = false

        percentage_seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    // Display the current progress of SeekBar
                    capture_percentage.text = "$i%"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // Do something
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // Do something
                    setPercentage(percentage_seekBar.progress)
                }

            })


        location_switch.setOnCheckedChangeListener {
                p0, p1 ->

                session.setCheckLocation(p1)
        }
    }

    private fun setPercentage(p :Int){

        if(p < 0 || p > 100){
            MDToast.makeText(this,"Percentage should be greater than 0 and less than 100.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
        }else{
            session.setCapturePercentage(p)
            MDToast.makeText(this,"Percentage set!", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()
        }
    }
}
