package com.th3s7r4ng3r.spen_to_pc

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper;
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.jaredrummler.android.device.DeviceName
import com.th3s7r4ng3r.spen_to_pc.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.OutputStream
import java.net.Socket


@OptIn(DelicateCoroutinesApi::class)
class MainActivity : AppCompatActivity() {

    // variables related to networking
    private lateinit var binding: ActivityMainBinding
    private lateinit var socket: Socket
    private lateinit var outputStream: OutputStream
    private var isConnected = false // Track connection status
    private var appVersion = "1.1"
    private var handler: Handler? = null
    private val apiData = GetFromAPI()

    //things to do when the app is opened
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge() //display the app below the notification bar

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateMainUI()
        handler = Handler(Looper.getMainLooper());

        // actions for connect button
        binding.connectButton.setOnClickListener {
            if(binding.desktopIpEditText.text.toString() != "")
                connectToDesktop()
            else
                Toast.makeText(this, "Enter IP address from Desktop server", Toast.LENGTH_SHORT).show()
        }
        //following buttons are hidden in the activity_main.xml | Used only for testing
        binding.nextBtn.setOnClickListener {
            sendData("Single Click")
        }
        binding.prevBtn.setOnClickListener {
            sendData("Double Click")
        }

        //handling footer button clicks
        binding.githubBtn.setOnClickListener {
            openBrowser("github")
        }
        binding.contactmeBtn.setOnClickListener {
            openBrowser("contact")
        }
        binding.donateBtn.setOnClickListener {
            openBrowser("donate")
        }

        //check the device compatibility
        checkCompatibility()
        DeviceName.init(this)

        // Run checkForUpdate after a delay
        handler!!.postDelayed({
            checkForUpdate()
        }, 1000)
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun connectToDesktop() {
        if (isConnected) {
            // Display error message if already connected
            Toast.makeText(this, "Already connected to desktop", Toast.LENGTH_SHORT).show()
            return
        }

        // Display "Connecting..."
        binding.connectionStatusLabel.text = "Connecting..."
        binding.connectionStatusLabel.setTextColor(Color.parseColor("#EDEDED"))
        binding.connectionStatusLabel.background = resources.getDrawable(R.drawable.rounded)

        //getting the ip address from the input
        val desktopIp = binding.desktopIpEditText.text.trim().toString()
        //opening a socket to the server
        GlobalScope.launch(Dispatchers.IO) {
            var connectSuccess = false
            try {
                    socket = Socket(desktopIp, 51515) // Create persistent socket
                    outputStream = socket.getOutputStream()
                    isConnected = true
                    connectSuccess = true
                    updateMainUI()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateMainUI()
                    binding.connectionStatusLabel.text = "Failed"
                    if(e.toString().contains("No address")){
                        Toast.makeText(this@MainActivity, "Incorrect IP address", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    updateMainUI()
                    binding.connectionStatusLabel.text = "Disconnected"
                }
            }
            //similar to pinging, check whether the server or the client disconnected
            if (connectSuccess) {
                sendHeartbeat()
            }
        }
    }

    // sending data stream to the server
    private fun sendData(data: String) {
        // check whether the connection status
        if (!isConnected) {
            Toast.makeText(this, "Not connected to the desktop server", Toast.LENGTH_SHORT).show()
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                //sending data to the server
                outputStream.write(data.toByteArray())

                // validating connection to the server
                if (data == "ping") {
                    withContext(Dispatchers.IO) { // Wait for pong response
                        try {
                            withTimeout(10000) { // Timeout after 10 seconds
                                val response = ByteArray(1024)
                                if (String(response) == "pong") {
                                    isConnected = true
                                } else {
                                    isConnected = false
                                    updateMainUI()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@MainActivity, "Error connecting to the server", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            isConnected = false
                            updateMainUI()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Error connecting to the server", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                isConnected = false
                updateMainUI()
                withContext(Dispatchers.Main) {
                   Toast.makeText(this@MainActivity, "Error sending data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // update the ui elements based on the server inputs
    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun updateMainUI(){
            GlobalScope.launch(Dispatchers.IO) {
                binding.versionID.text = binding.versionID.text.toString() + " " + appVersion
                if(isConnected) {
                    withContext(Dispatchers.Main) {
                        // only for testing
                        binding.nextBtn.isEnabled = true
                        binding.prevBtn.isEnabled = true

                        // styling based on the action
                        binding.connectionStatusLabel.text = "Connected"
                        binding.connectionStatusLabel.setTextColor(Color.parseColor("#FFD9FFC4"))
                        binding.connectionStatusLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF24471E"))
                        binding.connectionStatusLabel.background = resources.getDrawable(R.drawable.rounded)

                        // disabling the connect button
                        binding.connectButton.isEnabled = false
                        binding.connectButton.setTextColor(Color.parseColor("#292929"))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        // only for testing
                        binding.nextBtn.isEnabled = false
                        binding.prevBtn.isEnabled = false

                        // styling based on the action
                        binding.connectionStatusLabel.text = "Disconnected"
                        binding.connectionStatusLabel.setTextColor(Color.parseColor("#FFC4C4"))
                        binding.connectionStatusLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#AE6A0000"))
                        binding.connectionStatusLabel.background = resources.getDrawable(R.drawable.rounded)

                        // enabling the connect button
                        binding.connectButton.isEnabled = true
                        binding.connectButton.setTextColor(Color.parseColor("#E1E1E1"))
                    }
                }
            }
    }

    // function to ping the server and client
    private fun sendHeartbeat() {
        GlobalScope.launch(Dispatchers.IO) {
            while (isConnected) {
                try {
                    outputStream.write("ping".toByteArray())
                    delay(100) // Delay for 0.1 seconds
                } catch (e: Exception) {
                    // Handle heartbeat sending errors
                    isConnected = false
                    updateMainUI()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Disconnected from the server", Toast.LENGTH_SHORT).show()
                    }
                    break
                }
            }
        }
    }


    // Spen connection related functions
    // handling the spen actions
    @SuppressLint("SetTextI18n")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_NUMPAD_6 -> {
                binding.SpenData.text = "SPen Data: Single Click"
                sendData("single_clk")
            }
            KeyEvent.KEYCODE_NUMPAD_4 -> {
                binding.SpenData.text = "SPen Data: Double Click"
                sendData("double_clk")
            }
            KeyEvent.KEYCODE_NUMPAD_1 -> {
                binding.SpenData.text = "SPen Data: Swipe Left"
                sendData("sw_left")
            }
            KeyEvent.KEYCODE_NUMPAD_3 -> {
                binding.SpenData.text = "SPen Data: Swipe Right"
                sendData("sw_right")
            }
            KeyEvent.KEYCODE_NUMPAD_8 -> {
                binding.SpenData.text = "SPen Data: Swipe Up"
                sendData("sw_up")
            }
            KeyEvent.KEYCODE_NUMPAD_2 -> {
                binding.SpenData.text = "SPen Data: Swipe Down"
                sendData("sw_down")
            }
            KeyEvent.KEYCODE_NUMPAD_7 -> {
                binding.SpenData.text = "SPen Data: Counter Clock Wise"
                sendData("cl_counterClockWise")
            }
            KeyEvent.KEYCODE_NUMPAD_9 -> {
                binding.SpenData.text = "SPen Data: Clock Wise"
                sendData("cl_clockWise")
            }
            else -> {
                binding.SpenData.text = "SPen Data:"
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    //handling the footer links clicks
    private fun openBrowser(action: String) {
        val intent = if (action == "github") {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/th3-s7r4ng3r/SPEN-To-PC-AndroidApp/releases"))
        } else if (action == "contact") {
            Intent(Intent.ACTION_VIEW, Uri.parse("mailto:th3.s7r4ng3r@gmail.com"))
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/th3.s7r4ng3r"))
        }
        startActivity(intent)
    }


    // warning popup for nun compatible devices
    @SuppressLint("SetTextI18n")
    private fun checkCompatibility(){
        val modeNolList = arrayOf("SM-S908", "SC-52C", "SCG14", "SM-S918", "SM-N960", "SM-N97", "SM-N770", "SM-N98", "SM-T86", "SM-T87", "SM-T97", "SM-X70", "SM-X80", "SM-X90", "SM-X71", "SM-X81", "SM-X91")
        val modelNameList = arrayOf("S22 Ultra","S23 Ultra", "Note9", "Note10", "Note20", "Tab S7", "Tab S8", "Tab S9")
        //val limitedCompatibleList = arrayOf("S21 Ultra", "Fold3", "Fold4", "Fold5","Tab S7 FE","Tab S9 FE")
        val limitedCompatibleList = arrayOf("SM-G998", "SC-52B","SM-F926","SC-55B","SCG11","SM-F936","SCG16","SC-55C","SM-F946","SC-55D","SCG22", "SM-T73", "SM-X51", "SM-X61", "SCT22")
        val currentDeviceModel = Build.MODEL // Get device model from Android system
        val currentDeviceName = DeviceName.getDeviceName() // Get the device name

        var isCompatible = "none"

        // check whether device is a Samsung device
        if (currentDeviceName.contains("Galaxy", true) || currentDeviceModel.contains("SM-", true) || currentDeviceModel.contains("SC", true)) {
            // check whether the device supports only SPen Pro
            for (model in limitedCompatibleList) {
                if (currentDeviceModel.contains(model, true)) {
                    isCompatible = "limited"
                    break
                }
            }
            // check whether the device is in the Model No list
            if (isCompatible == "none") {
                for (model in modeNolList) {
                    if (currentDeviceModel.contains(model, true)) {
                        isCompatible = "fully"
                        break
                    }
                }
            }
            // check whether the device is in the model name list
            if (isCompatible == "none") {
                for (model in modelNameList) {
                    if (currentDeviceName.contains(model, true)) {
                        isCompatible = "fully"
                        break
                    }
                }
            }
        }

        if(isCompatible == "none"){
            //display the incompatibility message
            showPopup("incompatible")
            binding.incompatibilityLbl.visibility =  View.VISIBLE
        } else if(isCompatible=="limited"){
            //display limited compatibility message
            Toast.makeText(this, "An SPEN Pro is required to use Air Actions", Toast.LENGTH_LONG).show()
            binding.incompatibilityLbl.text = "An SPEN Pro is Required!"
            binding.incompatibilityLbl.visibility =  View.VISIBLE
        }
    }
    private fun showPopup(type:String){
        val builder = AlertDialog.Builder(this)
        if (type == "update") {
            builder.setTitle("Update Available!")
            builder.setMessage("New version of the SPEN To PC has been released with following changes:\n\n" + apiData.appChangedLog + "\n\n Press \"Check for Updates\" to download")
            builder.setPositiveButton("OK") { _, _ ->
            }
        }
        if(type=="incompatible"){
            builder.setTitle("Warning!")
            builder.setMessage("Your Device may not compatible with the SPen Air Actions. App functionalities may not work as intended. Proceed with caution!")
            builder.setPositiveButton("OK"){ _, _ ->
            }
        }

        val dialog = builder.create()
        dialog.show()
    }

    // check for app update
    private fun checkForUpdate(){
        val serverVersion = apiData.appVersion
        val status = apiData.dataRetrieved
        if(status == "true") {
            if (!appVersion.equals(serverVersion, true)) {
                showPopup("update")
            }
        }
    }


    // disconnect and destroy resources when the app is closed
    override fun onDestroy() {
        super.onDestroy()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                outputStream.write("connection closed".toByteArray())
                socket.close() // Close connection on app close
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error closing the connection", Toast.LENGTH_SHORT).show()
            }
        }
    }
}