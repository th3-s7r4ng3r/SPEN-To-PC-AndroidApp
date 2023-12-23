package com.th3s7r4ng3r.spen_to_pc

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.sdk.penremote.AirMotionEvent
import com.samsung.android.sdk.penremote.ButtonEvent
import com.samsung.android.sdk.penremote.SpenEventListener
import com.samsung.android.sdk.penremote.SpenRemote
import com.samsung.android.sdk.penremote.SpenRemote.ConnectionResultCallback
import com.samsung.android.sdk.penremote.SpenUnit
import com.samsung.android.sdk.penremote.SpenUnitManager
import com.th3s7r4ng3r.spen_to_pc.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.OutputStream
import java.net.Socket


class MainActivity : AppCompatActivity() {

    // variables related to netowrking
    private lateinit var binding: ActivityMainBinding
    private lateinit var socket: Socket
    private lateinit var outputStream: OutputStream
    private var isConnected = false // Track connection status

    // variables related to SPEN connection
    private var mSpenUnitManager: SpenUnitManager? = null
    private var mButtonPressed = false
    private var SpenConnected = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateMainUI()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.connectButton.setOnClickListener {
            connectToDesktop()
        }
        binding.singleClickButton.setOnClickListener {
            sendData("Single Click")
        }
        binding.doubleClickButton.setOnClickListener {
            sendData("Double Click")
        }
        initSpenRemote();
    }

    @SuppressLint("SetTextI18n")
    private fun connectToDesktop() {
        if (isConnected) {
            // Display error message if already connected
            Toast.makeText(this, "Already connected to desktop", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable the Connect button and display "Connecting..."
        binding.connectionStatusLabel.text = "Connection Status: Connecting..."

        val desktopIp = binding.desktopIpEditText.text.toString()
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
                    binding.connectionStatusLabel.text = "Connection Status: Failed"
                    Toast.makeText(this@MainActivity, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    updateMainUI()
                    binding.connectionStatusLabel.text = "Connection Status: Disconnected"
                }
            }
            if (connectSuccess) {
                sendHeartbeat() // Start sending heartbeats
            }
        }
    }

    private fun sendData(data: String) {
        if (!isConnected) {
            Toast.makeText(this, "Not connected to desktop", Toast.LENGTH_SHORT).show()
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write(data.toByteArray())
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
                // Switch to the main thread for UI interactions
                isConnected = false
                updateMainUI()
                withContext(Dispatchers.Main) {
                   Toast.makeText(this@MainActivity, "Error sending data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateMainUI(){
            GlobalScope.launch(Dispatchers.IO) {
                if(isConnected) {
                    withContext(Dispatchers.Main) {
                        binding.connectionStatusLabel.text = "Connection Status: Connected"
                        binding.singleClickButton.isEnabled = true
                        binding.doubleClickButton.isEnabled = true
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.connectionStatusLabel.text = "Connection Status: Disconnected"
                        binding.singleClickButton.isEnabled = false
                        binding.doubleClickButton.isEnabled = false
                    }
                }
            }
    }
    private fun sendHeartbeat() {
        GlobalScope.launch(Dispatchers.IO) {
            while (isConnected) {
                try {
                    outputStream?.write("ping".toByteArray())
                    delay(100) // Delay for 0.1 seconds
                } catch (e: Exception) {
                    // Handle heartbeat sending errors
                    isConnected = false
                    updateMainUI()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Heartbeat error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    break
                }
            }
        }
    }


    // Spen connection related functions
    @SuppressLint("SetTextI18n")
    private fun initSpenRemote() {
        //Check whether S Pen features are supported in the device

            val spenRemote = SpenRemote.getInstance()
             if (!spenRemote.isFeatureEnabled(SpenRemote.FEATURE_TYPE_BUTTON)) {
                 GlobalScope.launch(Dispatchers.IO) {
                     withContext(Dispatchers.Main) {
                         binding.SpenConnection.text = "SPen Version: Only Air Action Supported"
                         SpenConnected = false;
                     }
                 }
                return
            }

            //Check if already connected
            if (spenRemote.isConnected) {
                GlobalScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        binding.SpenConnection.text = "SPen Data: Connected!"
                    }
                }
                return
            }

            //Connect to the S Pen Framework
            mSpenUnitManager = null
            spenRemote.connect(this, object : ConnectionResultCallback {
                override fun onSuccess(spenUnitManager: SpenUnitManager) {
                    //If connection is successful, register SpenEventListener for each units.
                    mSpenUnitManager = spenUnitManager
                    binding.SpenConnection.text = "SPen Data: Connection established!"
                    SpenConnected = true;
                    initSpenEventListener()
                }
                override fun onFailure(e: Int) {
                    SpenConnected = false;
                    Toast.makeText(this@MainActivity, "SPen error: ${e}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun initSpenEventListener() {
        val buttonUnit = mSpenUnitManager!!.getUnit(SpenUnit.TYPE_BUTTON)
        if (buttonUnit != null) {
            mSpenUnitManager!!.registerSpenEventListener(mSpenButtonEventListener, buttonUnit)
        }
    }

    @SuppressLint("SetTextI18n")
    private val mSpenButtonEventListener = SpenEventListener { spenEvent ->
        val buttonEvent = ButtonEvent(spenEvent)
        var pressCount = 0.0;

        when (buttonEvent.action) {
            ButtonEvent.ACTION_DOWN -> {
                mButtonPressed = true
                pressCount = pressCount + 1;
            }
            ButtonEvent.ACTION_UP -> {
                mButtonPressed = false
                pressCount = pressCount + 1;
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                if (pressCount == 1.0) {
                    binding.SpenData.text = "SPen Data: Single Click"
                } else if (pressCount > 1.0) {
                    binding.SpenData.text = "SPen Data: Double Click"
                    pressCount = 0.0;
                } else {
                    delay(200)
                    binding.SpenData.text = "SPen Data:"
                    pressCount = 0.0;
                }
            }
        }
    }

    private fun releaseSpenRemote() {
        val spenRemote = SpenRemote.getInstance()
        if (spenRemote.isConnected) {
            spenRemote.disconnect(this)
        }
        mSpenUnitManager = null
    }


    override fun onDestroy() {
        super.onDestroy()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                outputStream.write("connection closed".toByteArray())
                mButtonPressed = false
                releaseSpenRemote();
                socket.close() // Close connection on app close
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error closing the connection", Toast.LENGTH_SHORT).show()
            }
        }
    }
}