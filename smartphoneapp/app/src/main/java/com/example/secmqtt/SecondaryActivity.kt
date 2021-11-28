package com.example.secmqtt

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SecondaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secondary)
        val rv = findViewById<RecyclerView>(R.id.reciclerView)
        rv.layoutManager = LinearLayoutManager(this)
        val devices = (1..4).map{
            IoTDevice("$it", "camera#$it")
        }.toMutableList()

        val ioTDeviceAdapter = IoTDeviceAdapter(devices)
        rv.adapter = ioTDeviceAdapter
    }
}

data class IoTDevice(val number:String, val name:String)

class IoTDeviceAdapter(val devices: MutableList<IoTDevice>): RecyclerView.Adapter<IoTDeviceAdapter.IoTDeviceViewHolder>(){

    class IoTDeviceViewHolder(v: View): RecyclerView.ViewHolder(v){
        val number = v.findViewById<TextView>(R.id.device_number)
        val name = v.findViewById<TextView>(R.id.device_name)
        val deviceRL = v.findViewById<RelativeLayout>(R.id.device_rl)

        fun bind(device: IoTDevice){
            number.text = "IoT device #" + device.number
            name.text = device.name
            deviceRL.setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW, Uri.parse("http://165.22.119.197:5000/" + device.number)
                )
                it.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IoTDeviceViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false) //visual tree created from the xml
        return IoTDeviceViewHolder(layout)
    }
    //takes the holder and binds it the element at a given position (invoking bind fun)
    override fun onBindViewHolder(holder: IoTDeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int {
        return devices.size
    }

}