package com.th3s7r4ng3r.spen_to_pc

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.th3s7r4ng3r.spen_to_pc.ui.theme.SPENToPCTheme

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import com.th3s7r4ng3r.spen_to_pc.databinding.ActivityMainBinding
import java.io.OutputStream
import java.net.Socket
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var socket: Socket
    private lateinit var outputStream: OutputStream

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.connectButton.setOnClickListener {
            val desktopIp = binding.desktopIpEditText.text.toString()
            ConnectTask().execute(desktopIp)
        }

        binding.singleClickButton.setOnClickListener {
            SendDataTask().execute("Single Click")
        }

        binding.doubleClickButton.setOnClickListener {
            SendDataTask().execute("Double Click")
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class ConnectTask : AsyncTask<String, Void, Boolean>() {
        @Deprecated("Deprecated in Java")
        @SuppressLint("SetTextI18n")
        override fun doInBackground(vararg params: String): Boolean {
            return try {
                socket = Socket(params[0], 51515)
                outputStream = socket.getOutputStream()
                true
            } catch (e: Exception) {
                runOnUiThread {
                    binding.connectionStatusLabel.text = "Connection Status: Failed"
                }
                false
            }
        }

        @SuppressLint("SetTextI18n")
        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: Boolean) {
            if (result) {
                binding.connectionStatusLabel.text = "Connection Status: Connected"
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class SendDataTask : AsyncTask<String, Void, Boolean>() {
        @SuppressLint("SetTextI18n")
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: String): Boolean {
            return try {
                val data = params[0]
                outputStream.write(data.toByteArray())
                true
            } catch (e: Exception) {
                runOnUiThread {
                    binding.connectionStatusLabel.text = "Connection Status: Failed"
                }
                false
            }
        }
    }
}