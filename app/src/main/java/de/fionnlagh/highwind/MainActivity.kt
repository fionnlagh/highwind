
package de.fionnlagh.highwind

import android.Manifest
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {

//    private lateinit var macField: EditText
//    private lateinit var passwordField: EditText
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var infoButton: Button
    private lateinit var infoText: TextView
    private lateinit var lockButton: Button
    private lateinit var unlockButton: Button
    private lateinit var lightOnButton: Button
    private lateinit var lightOffButton: Button
    private lateinit var locateButton: Button
    private lateinit var sendSpeedButton: Button
    private lateinit var toggleSportMode: Switch
    private lateinit var speedLimitText: TextView
    private lateinit var speedLimit: SeekBar
    private lateinit var logging: TextView
    // private lateinit var lightButton: Button
    private var currentSpeedLimit: Int = 21
    private var isSportMode: Int = 1
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
//    private var btGattMtu = 24
    private var btGattCurrentMtu = 20
    private val SERVICE_UUID = UUID.fromString("00002c00-0000-1000-8000-00805f9b34fb")
    private val CHAR_UUID = UUID.fromString("00002c10-0000-1000-8000-00805f9b34fb")

    private val handler = android.os.Handler()
    private lateinit var statusRunnable: Runnable

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        macField = findViewById(R.id.macField)
//        passwordField = findViewById(R.id.passwordField)
        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        infoButton = findViewById(R.id.infoButton)
        infoText = findViewById(R.id.infoText)
        lockButton = findViewById(R.id.lockButton)
        unlockButton = findViewById(R.id.unlockButton)
        lightOnButton = findViewById(R.id.lightOn)
        lightOffButton = findViewById(R.id.lightOff)
        locateButton = findViewById(R.id.locate)
        sendSpeedButton= findViewById(R.id.defaultSpeed)
        toggleSportMode = findViewById(R.id.toggleSportMode)
        speedLimitText= findViewById(R.id.speedLimitText)
        speedLimit= findViewById(R.id.speedLimit)
        logging= findViewById(R.id.logging)


        connectButton.setOnClickListener { connectToScooter() }
        infoButton.setOnClickListener { sendCommand("AT+BKINF=${getPassword()},0") }
        lockButton.setOnClickListener { sendCommand("AT+BKSCT=${getPassword()},1") }
        unlockButton.setOnClickListener { sendCommand("AT+BKSCT=${getPassword()},0") }
        lightOnButton.setOnClickListener { sendCommand("AT+BKLED=${getPassword()},0,1") }
        lightOffButton.setOnClickListener { sendCommand("AT+BKLED=${getPassword()},0,0") }
        locateButton.setOnClickListener { sendCommand("AT+BKLOC=${getPassword()},0") }
        sendSpeedButton.setOnClickListener {
            sendCommand("AT+BKECP=${getPassword()},1,21,1,")
            isSportMode = 1
            currentSpeedLimit = 21
            speedLimitText.text = "Speed Limit: $currentSpeedLimit km/h"
            speedLimit.progress = currentSpeedLimit
            toggleSportMode.isChecked = true
        }
        logging.setMovementMethod(android.text.method.ScrollingMovementMethod())

        toggleSportMode.setOnCheckedChangeListener {
                _, isChecked ->
            if (isChecked) {
                isSportMode = 1
                sendCommand("AT+BKECP=${getPassword()},1,,1,")
            } else {
                isSportMode = 0
                sendCommand("AT+BKECP=${getPassword()},1,,0,")
            }
        }

        speedLimit.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSpeedLimit = progress
                speedLimitText.text = "Speed Limit: $progress km/h"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                logging.append("Speed Limit: $currentSpeedLimit km/h\n")
                sendCommand("AT+BKECP=${getPassword()},1,$currentSpeedLimit,,")
            }
        })

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
        connectToScooter()
    }

    private fun hasPermissions(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getPassword(): String {
        return "BLE Password goes here"
    }

    private fun connectToScooter() {
 //       val mac = macField.text.toString()
        val mac = "MAC Address in format xx:xx:xx:xx:xx:xx goes here"
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac)
        statusText.text = "\uD83D\uDFE1Connecting..."
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private fun sendCommand(command: String) {
        val formattedCommand = "$command$\r\n"
        val bytes = formattedCommand.toByteArray(Charsets.UTF_8)
        val chunks = bytes.asList().chunked(btGattCurrentMtu)

        for (chunk in chunks) {
            val packet = ByteArray(chunk.size) { i -> chunk[i] }
            writeCharacteristic?.value = packet
            bluetoothGatt?.writeCharacteristic(writeCharacteristic)
            Thread.sleep(100)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                Thread.sleep(100)
 //               bluetoothGatt?.requestMtu(247)
                gatt?.discoverServices()
                runOnUiThread { statusText.text = "\uD83D\uDD35Connected" }
            } else {
                handler.removeCallbacks(statusRunnable)
                runOnUiThread { statusText.text = "\uD83D\uDD34Disconnected" }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val service = gatt?.getService(SERVICE_UUID)
            writeCharacteristic = service?.getCharacteristic(CHAR_UUID)
            gatt?.setCharacteristicNotification(writeCharacteristic, true)

            val descriptor = writeCharacteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt?.writeDescriptor(descriptor)
            runOnUiThread {
                statusText.text = if (writeCharacteristic != null) "\uD83D\uDFE2Ready" else "Characteristic not found"
            }

            statusRunnable = object : Runnable {
                override fun run() {
                    sendCommand("AT+BKINF=${getPassword()},0")
                    handler.postDelayed(this, 500) // alle 3 Sekunden
                }
            }
            handler.post(statusRunnable)

        }

        private val responseBuffer = StringBuilder()

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val responsePart = characteristic.value.toString(Charsets.UTF_8)
            responseBuffer.append(responsePart)

            if (responsePart.contains("\r\n")) { // Ende der Antwort erkannt
                val completeResponse = responseBuffer.toString()
                responseBuffer.clear()
                runOnUiThread {
 //                   logging.append("Antwort: $completeResponse")
                    handleScooterResponse(completeResponse)
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread {
                    logging.append("MTU geändert auf $mtu\n")
                }
            } else {
                runOnUiThread {
                    logging.append("MTU-Änderung fehlgeschlagen\n")
                }
            }
        }
    }

    private fun handleScooterResponse(response: String) {
        if (!response.startsWith("+ACK:")) {
            logging.append("Unbekannte Antwort: $response\n")
            return
        }

        val ackPayload = response.removePrefix("+ACK:").removeSuffix("$\r\n").trim()
        if (ackPayload.length < 5) {
            logging.append("Ungültige Antwortstruktur: $response\n")
            return
        }

        val command = ackPayload.substring(0, 5)
        val paramString = ackPayload.drop(6) // Skipping the colon after 5-char command
        val params = paramString.split(",")

        when (command) {
            "BKSCT" -> {
                val result = params.getOrNull(0)
                when (result) {
                    "0" -> logging.append("Scooter entsperrt.\n")
                    "1" -> logging.append("Scooter gesperrt.\n")
                    else -> logging.append("Unbekannter Rückgabecode bei BKSCT: $result\n")
                }
            }

            "BKLED" -> {
                  logging.append("Licht an/aus.\n")
            }
            "BKECP" -> {
                val result = params.getOrNull(0)
                when (result) {
                    "0" -> logging.append("Sportmode / Geschwindigkeit nicht akzeptiert\n")
                    "1" -> logging.append("Sportmode / Geschwindigkeit akzeptiert\n")
                    else -> logging.append("Unbekannter Rückgabecode bei BKECP: $result\n")
                }
            }
            "BKINF" -> {
                val lockSymbol: String
                val lampSymbol: String

                val lockstatus = params.getOrNull(0)
                val speed = params.getOrNull(1)
                val trip = params.getOrNull(2)
                val totalmileage = params.getOrNull(3)
                val ridetime = params.getOrNull(4)
                val bat = params.getOrNull(5)
                val lamp = params.getOrNull(6)
                if (lockstatus != "0") {
                    lockSymbol = "\uD83D\uDD12"
                }else{
                    lockSymbol = "\uD83D\uDD13"
                }
                if (lamp != "0") {
                    lampSymbol = "\uD83C\uDF1D"
                }else{
                    lampSymbol = "\uD83C\uDF1A"
                }
                //logging.append("Statusdaten erhalten:\n$paramString\n")
                //infoText.text = "$lockSymbol \uFE0F $speed km/h \uD83D\uDEF4 $trip \uD83D\uDD1D $totalmileage ⌚ $ridetime \uD83D\uDD0B$bat% $lampSymbol"
                infoText.text = "$lockSymbol \uFE0F$speed km/h \uD83D\uDEF4$trip km \uD83D\uDD1D$totalmileage km \uD83D\uDD0B$bat% $lampSymbol"
            }
            "BKLOC" -> {
            logging.append("Blinki Blinki\n")
            }

            else -> {
                logging.append("Unbekannter Befehl: $command mit Daten: $paramString\n")
            }
        }
    }
}
