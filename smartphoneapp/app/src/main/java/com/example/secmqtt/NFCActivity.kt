package com.example.secmqtt

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.*
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class NFCActivity : Activity() {

    private var mNfcAdapter: NfcAdapter? = null
    private lateinit var payload: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)
        payload = intent.extras?.getByteArray("payload")!!
        /*
        val path = getExternalFilesDir(null)
        val file = File(path, "C2_1")
        val inputAsString =
            FileInputStream(file).bufferedReader().use { it.readText() }
        payload = inputAsString.toByteArray()
        Log.d("inputAsString", inputAsString)
        */
        Log.d("payload", payload.toString())
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    override fun onResume() {
        super.onResume()
        mNfcAdapter?.let {
            enableNFCInForeground(it, this, javaClass)
        }
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter?.let {
            disableNFCInForeground(it, this)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val messageWrittenSuccessfully = createNFCMessage(payload, intent)
        val resultTextView = findViewById<TextView>(R.id.nfc_text)
        resultTextView.text = ifElse(
            messageWrittenSuccessfully,
            "Successful Written to Tag",
            "Something was wrong\nTry Again"
        )
    }


    fun <T> ifElse(condition: Boolean, primaryResult: T, secondaryResult: T) =
        if (condition) primaryResult else secondaryResult

    fun createNFCMessage(payload: ByteArray, intent: Intent?): Boolean {

        val pathPrefix = "com.example.secmqtt.com:nfcapp"
        val nfcRecord = NdefRecord(
            NdefRecord.TNF_EXTERNAL_TYPE,
            pathPrefix.toByteArray(),
            ByteArray(0),
            payload
        )
        Log.d("nfcRecord", nfcRecord.toString())
        val nfcMessage = NdefMessage(arrayOf(nfcRecord))
        Log.d("nfcMessage", nfcMessage.toString())
        intent?.let {
            val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            return writeMessageToTag(nfcMessage, tag)
        }
        return false
    }

    fun <T> enableNFCInForeground(nfcAdapter: NfcAdapter, activity: Activity, classType: Class<T>) {
        val pendingIntent = PendingIntent.getActivity(
            activity, 0,
            Intent(activity, classType).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )
        val nfcIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        val filters = arrayOf(nfcIntentFilter)

        val TechLists =
            arrayOf(arrayOf(Ndef::class.java.name), arrayOf(NdefFormatable::class.java.name))

        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, TechLists)
    }

    fun disableNFCInForeground(nfcAdapter: NfcAdapter, activity: Activity) {
        nfcAdapter.disableForegroundDispatch(activity)
    }

    private fun writeMessageToTag(nfcMessage: NdefMessage, tag: Tag?): Boolean {

        Log.d("Tech type", Arrays.toString(tag!!.techList))
        try {
            val nDefTag = Ndef.get(tag)

            nDefTag?.let {
                it.connect()
                Log.d("tag size", it.maxSize.toString())
                Log.d("message size", nfcMessage.toByteArray().size.toString())
                if (it.maxSize < nfcMessage.toByteArray().size) {
                    //Message to large to write to NFC tag
                    Log.d("ALT", "Message too large to write to NFC tag")
                    return false
                }
                if (it.isWritable) {
                    it.writeNdefMessage(nfcMessage)
                    it.close()
                    Log.d("Success", "Message written to the tag")
                    return true
                } else {
                    Log.d("Fail", "Tag is read only")
                    return false
                }
            }

            val nDefFormatableTag = NdefFormatable.get(tag)

            nDefFormatableTag?.let {
                try {
                    it.connect()
                    it.format(nfcMessage)
                    it.close()
                    Log.d("Completed", "Data written to the tag")
                    return true
                } catch (e: IOException) {
                    Log.d("Fail", "Failed to format tag")
                    return false
                }
            }
            //NDEF is not supported
            Log.d("Fail", "NDEF not supported")
            return false

        } catch (e: Exception) {
            //Write operation has failed
            Log.d("Fail", "Write operation has failed")
        }
        return false
    }
}
