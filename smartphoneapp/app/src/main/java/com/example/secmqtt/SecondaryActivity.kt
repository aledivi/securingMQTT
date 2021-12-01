package com.example.secmqtt

import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection



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

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(device: IoTDevice){
            number.text = "IoT device #" + device.number
            name.text = device.name
            deviceRL.setOnClickListener {
                /*val intent = Intent(
                    Intent.ACTION_VIEW, Uri.parse("http://165.22.119.197:5000/" + device.number)
                )
                it.context.startActivity(intent)*/
                val current_time = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss")
                val ct_formatted = current_time.format(formatter)
                var postUrl = "http://165.22.119.197:5000/" + device.number
                var postBody = "{" +
                        "    \"timestamp\": \"${ct_formatted}\"\n" +
                        "}"

                val JSON: MediaType? = "application/json; charset=utf-8".toMediaTypeOrNull()
                val client = OkHttpClient()

                val body = postBody.toRequestBody(JSON)
                val request: Request = Request.Builder()
                    .url(postUrl)
                    .post(body)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: java.io.IOException) {
                        call.cancel()
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        Log.d("TAG_GG", "all goes well ")
                        val knowledge = response.body!!.bytes()
                        if (response.code >= HttpURLConnection.HTTP_OK &&
                            response.code < HttpURLConnection.HTTP_MULT_CHOICE && response.body != null) {
                            //Log.d("Response body: ", response.body!!.bytes().decodeToString())
                            val path = it.context.getExternalFilesDir(null)
                            val file = File(path, "C2_" + device.number)
                            Log.d("dir", file.path)
                            FileOutputStream(file).use {
                                it.write(knowledge)
                            }
                            val inputAsString = FileInputStream(file).bufferedReader().use { it.readText() }
                            Log.d("from file", inputAsString)
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    it.context,
                                    "C2_" + device.number + " saved in " + path,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                })
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IoTDeviceViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false) //visual tree created from the xml
        return IoTDeviceViewHolder(layout)
    }
    //takes the holder and binds it the element at a given position (invoking bind fun)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: IoTDeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int {
        return devices.size
    }

}