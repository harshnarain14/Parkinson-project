package com.harsh.parkinson.fragment


import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.harsh.parkinson.R
import com.harsh.parkinson.database.AppDatabase
import com.harsh.parkinson.database.ResultTremor
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


class DashboardFragment(val contextParam: Context) : Fragment() {

    private val DEVICE_ADDRESS = "00:19:10:09:22:26" // Replace with your HC-05 device address
    private val DEVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val REQUEST_ENABLE_BLUETOOTH = 1
    private val MESSAGE_RECEIVED = 2
    private val MAX_ENTRIES = 10
    var ino = 0
    lateinit var textData: List<String> // Replace with your actual dat a
    val delayInMillis: Long = 1000 // Adjust the delay as per your requirements

    var currentIndex = 0

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var connectedThread: ConnectedThread
    private lateinit var lineChart: LineChart
    lateinit var connectbtn:Button
    lateinit var start:Button
    private lateinit var lineDataSet: LineDataSet
    private lateinit var lineData: LineData
    private val entries = ArrayList<Entry>()
    private val xLabels = ArrayList<String>()
    private var xIndex = 0
    private val command = "1"
    val yValue =0.0f


    private lateinit var dataTextView: TextView

    private val receivedData = StringBuilder()

    private val mHandler = Handler(Handler.Callback { msg ->
        if (msg.what == MESSAGE_RECEIVED) {
            val receivedMsg = msg.obj as String
            // Check if the received message is valid
            if (receivedMsg.isNotEmpty() && receivedMsg.isNotBlank()) {
                receivedData.append(receivedMsg)

            // Split the received data into separate lines
            val lines = receivedData.toString().split("\n")
            println(receivedData)
            for (line in lines) {
                if (line.isNotEmpty()) {
                    val yValue = line.toFloatOrNull()
                    dataTextView.text = yValue.toString()
                    if (yValue != null) {
                        val entry = Entry(xIndex.toFloat(), yValue)
                        entries.add(entry)
                        xIndex++
                    }
                }
            }

            // Limit the number of entries to MAX_ENTRIES
            if (entries.size > MAX_ENTRIES) {
                entries.removeAt(0)
            }

            // Update the line chart with the new entries
            updateLineChart()

            // Clear the receivedData StringBuilder to avoid accumulating old data
            receivedData.clear()
            } else {
                // Handle invalid or empty message error
                // For example, show a toast message or log the error
                Toast.makeText(activity as Context, "Invalid data received", Toast.LENGTH_SHORT).show()
            }

        }
        true
    })



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        lineChart = view.findViewById(R.id.chart)
        connectbtn=view.findViewById(R.id.conn)
        start=view.findViewById(R.id.start)
        dataTextView = view.findViewById(R.id.text)


        start.setOnClickListener {
            try {
                val outputStream = connectedThread.getOutputStream()
                val message = command.toByteArray()
                outputStream?.write(message)
                setupLineChart()

                val maxValueEntry = entries.maxByOrNull { it.y }
                val maxValue = maxValueEntry?.y
                Log.d("DashboardFragment", "Max Value: $maxValue")

                val currentDay = getCurrentDay()

                // Create an instance of ResultTremor
                val resultEntity = ResultTremor(maxValue = maxValue ?: 0.0f, currentDay = currentDay)

                // Launch a new coroutine with Dispatchers.IO context
                CoroutineScope(Dispatchers.IO).launch {
                    // Insert the resultEntity into the database using the DAO
                    val resultDao = AppDatabase.getInstance(requireContext()).resultDao()
                    resultDao.insert(resultEntity)

                    // Display a toast message indicating successful storage
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Data stored successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                // Handle write error
            }
        }

        connectbtn.setOnClickListener {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                showEnableBluetoothDialog()
                Toast.makeText(activity as Context, "not found", Toast.LENGTH_SHORT).show()
            } else {
                connectToDevice()
            }
        }


        return view
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is enabled, connect to the device
                connectToDevice()
            } else {
                Toast.makeText(activity as Context, "Bluetooth enabling cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::connectedThread.isInitialized) {
            connectedThread.cancel()
        }
    }

    private fun connectToDevice() {
        println("hello")
        val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS)
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(DEVICE_UUID)
            bluetoothSocket.connect()
            connectedThread = ConnectedThread(bluetoothSocket)
            connectedThread.start()
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle connection error
            Toast.makeText(activity as Context, "not found", Toast.LENGTH_SHORT).show()
            println("no")
        }
    }

    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {


        fun getOutputStream(): OutputStream? {
            return bluetoothSocket.outputStream
        }
        private val inputStream: InputStream = socket.inputStream
        private var receivingData = true

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (receivingData) {
                try {
                    bytes = inputStream.read(buffer)
                    val receivedMessage = String(buffer, 0, bytes)


                    // Check if the received message is valid
                    if (receivedMessage.isNotEmpty() && receivedMessage.isNotBlank()) {
                        mHandler.obtainMessage(MESSAGE_RECEIVED, receivedMessage).sendToTarget()
                    } else {
                        // Handle invalid or empty message error
                        // For example, show a toast message or log the error
                        mHandler.obtainMessage(MESSAGE_RECEIVED, "Invalid data").sendToTarget()
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                    // Handle read error
                    break
                }
            }

        }
        fun stopReceivingData() {
            receivingData = false
        }


        fun cancel() {
            try {
                bluetoothSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun setupLineChart() {
        lineChart.description.isEnabled = true
        lineChart.setTouchEnabled(false)
        lineChart.setPinchZoom(false)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)

        val yAxis = lineChart.axisLeft
        yAxis.setDrawGridLines(true)

        lineChart.axisRight.isEnabled = true

        lineChart.legend.isEnabled = true
    }

    private fun updateLineChart() {
        val lineDataSet = LineDataSet(entries, "Data Set")
        lineDataSet.color = Color.RED
        lineDataSet.valueTextColor = Color.BLACK
        lineDataSet.setDrawCircles(false)
        lineDataSet.lineWidth = 2f
        lineDataSet.setDrawValues(false)
        lineDataSet.setDrawFilled(false)
        lineDataSet.mode = LineDataSet.Mode.LINEAR

        lineData = LineData(lineDataSet)
        lineChart.data = lineData

        lineChart.notifyDataSetChanged()
        lineChart.moveViewToX(xIndex.toFloat())
        lineChart.invalidate()
    }
    private fun showEnableBluetoothDialog() {
        val alertDialog = AlertDialog.Builder(contextParam)
            .setTitle("Enable Bluetooth")
            .setMessage("Bluetooth is disabled. Do you want to enable it?")
            .setPositiveButton("Open Settings") { _, _ ->
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        alertDialog.show()
    }
    private fun getCurrentDay(): String {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE")
        return currentDate.format(formatter)
    }

    private fun getCurrentTimestamp(): String {
        val calendar = Calendar.getInstance()
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}













