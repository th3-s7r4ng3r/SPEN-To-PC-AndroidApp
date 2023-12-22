package com.th3s7r4ng3r.spen_to_pc

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.th3s7r4ng3r.spen_to_pc.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.OutputStream
import java.net.Socket


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var socket: Socket
    private lateinit var outputStream: OutputStream
    private var isConnected = false // Track connection status

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
                    binding.connectionStatusLabel.text = "Connection Status: Disconnected"
                }
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            socket.close() // Close connection on app close
        } catch (e: Exception) {
            Toast.makeText(this, "Error closing the connection", Toast.LENGTH_SHORT).show()
        }
    }
}