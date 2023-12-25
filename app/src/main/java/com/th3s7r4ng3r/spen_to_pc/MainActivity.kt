package com.th3s7r4ng3r.spen_to_pc

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

    //things to do when the app is opened
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge() //display the app below the notification bar
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateMainUI()

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
                        binding.connectionStatusLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#471E1E"))
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
    // handling the spen clicked
    @SuppressLint("SetTextI18n")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_NUMPAD_6 -> {
                binding.SpenData.text = "SPen Data: Single Click"
                sendData("Single Click")
            }
            KeyEvent.KEYCODE_NUMPAD_4 -> {
                binding.SpenData.text = "SPen Data: Double Click"
                sendData("Double Click")
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
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/th3-s7r4ng3r/SPEN-To-PC-AndroidApp"))
        } else if (action == "contact") {
            Intent(Intent.ACTION_VIEW, Uri.parse("mailto:gvinura@gmail.com"))
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/th3.s7r4ng3r"))
        }
        startActivity(intent)
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