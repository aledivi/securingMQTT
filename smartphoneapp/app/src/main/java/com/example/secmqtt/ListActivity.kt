package com.example.secmqtt

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

private lateinit var psk : SecretKey
private lateinit var iv: ByteArray

class ListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        psk = SecretKeySpec(readByteFile("psk"), "AES")
        //iv = readByteFile("iv")

        val rv = findViewById<RecyclerView>(R.id.reciclerView)
        rv.layoutManager = LinearLayoutManager(this)
        val devices = (1..4).map{
            IoTDevice("$it", "camera#$it")
        }.toMutableList()
        val ioTDeviceAdapter = IoTDeviceAdapter(devices, applicationContext)
        rv.adapter = ioTDeviceAdapter
    }

    fun readByteFile(filename: String) : ByteArray{
        val path = getExternalFilesDir(null)
        val file = File(path, filename)
        val length = file.length().toInt()
        val bytes = ByteArray(length)
        val fin = FileInputStream(file)
        try {
            fin.read(bytes)
        } finally {
            fin.close()
        }
        return bytes
    }

}

data class IoTDevice(val number:String, val name:String)

class IoTDeviceAdapter(val devices: MutableList<IoTDevice>, val context: Context): RecyclerView.Adapter<IoTDeviceAdapter.IoTDeviceViewHolder>(){

    class IoTDeviceViewHolder(v: View): RecyclerView.ViewHolder(v){
        private lateinit var context: Context
        val number = v.findViewById<TextView>(R.id.device_number)
        val name = v.findViewById<TextView>(R.id.device_name)
        val deviceRL = v.findViewById<RelativeLayout>(R.id.device_rl)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(device: IoTDevice, context: Context){
            this.context = context
            number.text = "IoT device #" + device.number
            name.text = device.name
            deviceRL.setOnClickListener {
                val current_time = LocalDateTime.now(ZoneOffset.UTC)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss.SSS")
                val ct_formatted = current_time.format(formatter)
                val postUrl = "http://165.22.119.197/" + device.number
                val postBody = "{\n" +
                        "\"timestamp\": \"${ct_formatted}\"\n" +
                        "}"

                val postBodyEncrypted = encrypt(postBody.replace("\n", ""))
                Log.d("length", postBodyEncrypted.length.toString())
                val JSON: MediaType? = "application/json; charset=utf-8".toMediaTypeOrNull()
                val client = OkHttpClient()
                val body = postBodyEncrypted.toRequestBody(JSON)
                val request: Request = Request.Builder()
                    .url(postUrl)
                    .post(body)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: java.io.IOException) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                it.context,
                                "Internal server error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        call.cancel()
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        val knowledge = response.body!!.bytes()
                        if (response.code >= HttpURLConnection.HTTP_OK &&
                            response.code < HttpURLConnection.HTTP_MULT_CHOICE && response.body != null
                        ) {
                            /*
                            val path = it.context.getExternalFilesDir(null)
                            val file = File(path, "C2_" + device.number)
                            FileOutputStream(file).use {
                                val decrypted = decrypt(knowledge)
                                Log.d("knowledge", decrypted.toString())
                                it.write(decrypted)
                            }

                            val inputAsString =
                                FileInputStream(file).bufferedReader().use { it.readText() }
                            Log.d("from file", inputAsString)
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    it.context,
                                    "C2_" + device.number + " saved in " + path,
                                    Toast.LENGTH_LONG
                                ).show()
                            } */
                            val payload = decrypt(knowledge)
                            val intent = Intent(it.context, NFCActivity::class.java)
                            intent.also {
                                it.putExtra("payload", payload)
                            }
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            Log.d("payload", payload.toString())
                            context.startActivity(intent)
                        } else if (response.code === HttpURLConnection.HTTP_CLIENT_TIMEOUT) {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    it.context,
                                    "Request time too old",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else if (response.code === HttpURLConnection.HTTP_UNAUTHORIZED) {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    it.context,
                                    "Invalid client",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    fun decrypt(content: ByteArray): ByteArray{
                        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                        cipher.init(Cipher.DECRYPT_MODE, psk)
                        val plainText = cipher.doFinal(Base64.getDecoder().decode(content))
                        return plainText
                    }
                })
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun encrypt(strToEncrypt: String) : String{
            val input = strToEncrypt.toByteArray(charset("UTF8"))
            synchronized(Cipher::class.java) {
                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                Log.d("input size", input.size.toString())
                cipher.init(Cipher.ENCRYPT_MODE, psk)
                //Log.d("iv", iv.toString())
                //Log.d("cipher iv", cipher.iv.toString())
                val cipherText = ByteArray(cipher.getOutputSize(input.size))
                Log.d("outputsize", cipherText.size.toString())
                var ctLength = cipher.update(
                    input, 0, input.size,
                    cipherText, 0
                )
                ctLength += cipher.doFinal(cipherText, ctLength)
                return Base64.getEncoder().encodeToString(cipherText)
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
        holder.bind(devices[position], context)
    }

    override fun getItemCount(): Int {
        return devices.size
    }

}