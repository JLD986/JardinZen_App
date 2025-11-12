package com.example.jardnzen_app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.locochones.jardnzen_app.R
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class VincularDispositivoActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var listViewDevices: ListView
    private lateinit var btnBuscar: Button
    private lateinit var btnEnviar: Button
    private lateinit var txtSsid: EditText
    private lateinit var txtPassword: EditText
    private lateinit var txtCorreo: EditText

    private var selectedDevice: BluetoothDevice? = null
    private var socket: BluetoothSocket? = null
    private var userPassword: String? = null // contraseña almacenada en la sesión

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: com.google.firebase.database.DatabaseReference

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val granted = perms.all { it.value == true }
            if (!granted) {
                Toast.makeText(this, "Permisos necesarios no concedidos", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vincular_dispositivo)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Usuarios")

        initBluetooth()
        setupViews()
        pedirPermisos()
        precargarCorreo()  // aquí cargamos correo y contraseña guardada
        setupListeners()
    }

    private fun initBluetooth() {
        val btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btManager.adapter
    }

    private fun setupViews() {
        listViewDevices = findViewById(R.id.listViewDevices)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnEnviar = findViewById(R.id.btnEnviar)
        txtSsid = findViewById(R.id.txtSsid)
        txtPassword = findViewById(R.id.txtPassword)
        txtCorreo = findViewById(R.id.txtCorreo)
    }

    private fun precargarCorreo() {
        val session = SessionManager(this)
        val email = session.getEmail()
        val password = session.getPassword()

        if (email != null) {
            txtCorreo.setText(email)
            txtCorreo.isEnabled = false
        }

        if (password != null) {
            userPassword = password // guardamos contraseña para enviar al ESP32
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupListeners() {
        btnBuscar.setOnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                Toast.makeText(this, "Activa Bluetooth y vuelve a pulsar buscar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            buscarDispositivosEmparejados()
        }

        listViewDevices.setOnItemClickListener { _, _, position, _ ->
            val bonded = getBondedDevicesList()
            if (position in bonded.indices) {
                selectedDevice = bonded[position]
                Toast.makeText(this, "Seleccionado: ${selectedDevice?.name}", Toast.LENGTH_SHORT).show()
            }
        }

        btnEnviar.setOnClickListener {
            enviarDatosBluetooth()
        }
    }

    private fun pedirPermisos() {
        val permisos = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permisos.add(Manifest.permission.BLUETOOTH_CONNECT)
            permisos.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permisos.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permisos.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        requestPermissionsLauncher.launch(permisos.toTypedArray())
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun hasBluetoothScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    @SuppressLint("MissingPermission")
    private fun getBondedDevicesList(): List<BluetoothDevice> {
        if (!hasBluetoothConnectPermission()) {
            Toast.makeText(this, "Permiso BLUETOOTH_CONNECT no concedido", Toast.LENGTH_SHORT).show()
            return emptyList()
        }
        val dispositivos = bluetoothAdapter.bondedDevices
        return dispositivos?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    private fun buscarDispositivosEmparejados() {
        val bonded = getBondedDevicesList()
        if (bonded.isEmpty()) {
            Toast.makeText(this, "No hay dispositivos emparejados", Toast.LENGTH_SHORT).show()
            return
        }
        val nombres = bonded.map { "${it.name}\n${it.address}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nombres)
        listViewDevices.adapter = adapter
    }

    @SuppressLint("MissingPermission")
    private fun enviarDatosBluetooth() {
        val ssid = txtSsid.text.toString().trim()
        val wifiPassword = txtPassword.text.toString().trim()
        val email = txtCorreo.text.toString().trim()

        if (selectedDevice == null) {
            Toast.makeText(this, "Selecciona un dispositivo primero", Toast.LENGTH_SHORT).show()
            return
        }
        if (ssid.isEmpty() || wifiPassword.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val json = JSONObject().apply {
            put("ssid", ssid)
            put("password", wifiPassword)
            put("userEmail", email)
            put("userPassword", userPassword ?: "")
            put("deviceId", selectedDevice?.name ?: "ESP32_01")
        }

        Thread {
            try {
                val device = selectedDevice!!
                val socketLocal = device.createRfcommSocketToServiceRecord(uuid)

                if (bluetoothAdapter.isDiscovering) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (hasBluetoothScanPermission()) bluetoothAdapter.cancelDiscovery()
                    } else {
                        bluetoothAdapter.cancelDiscovery()
                    }
                }

                socketLocal.connect()
                val out = socketLocal.outputStream
                out.write((json.toString() + "\n").toByteArray(Charsets.UTF_8))
                out.flush()

                // 🕐 Esperar a que el ESP32 procese y responda
                val input = socketLocal.inputStream
                if (input.available() > 0) {
                    val buffer = ByteArray(256)
                    val bytes = input.read(buffer)
                    val respuesta = String(buffer, 0, bytes)
                    Log.d("BluetoothRespuesta", "ESP32 respondió: $respuesta")
                }

                Thread.sleep(1000) // espera para asegurar envío completo
                socketLocal.close()

                runOnUiThread {
                    Toast.makeText(this, "Datos enviados correctamente ✅", Toast.LENGTH_LONG).show()
                    // Guardar en Firebase después del envío exitoso
                    guardarDispositivoEnFirebase()
                }
            } catch (e: Exception) {
                Log.e("VincularDispositivo", "Error enviando datos", e)
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun guardarDispositivoEnFirebase() {
        val user = auth.currentUser
        user?.let { uid ->
            val dispositivoNombre = selectedDevice?.name ?: "ESP32_JardnZen"

            // Guardar en la estructura del usuario
            database.child(uid.uid).child("dispositivoIoT").setValue(dispositivoNombre)
                .addOnSuccessListener {
                    Log.d("VincularDispositivo", "Dispositivo guardado en Firebase: $dispositivoNombre")
                    Toast.makeText(this, "Dispositivo vinculado exitosamente", Toast.LENGTH_SHORT).show()

                    // Crear estructura de sensores por defecto
                    crearEstructuraSensores(uid.uid, dispositivoNombre)

                    // Cerrar actividad después de éxito
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("VincularDispositivo", "Error al guardar dispositivo", e)
                    Toast.makeText(this, "Error al guardar en la nube", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun crearEstructuraSensores(uid: String, dispositivoNombre: String) {
        val sensoresRef = database.child(uid).child("dispositivos").child(dispositivoNombre).child("sensores")

        val datosSensores = mapOf(
            "temperatura" to 0.0,
            "humedad_suelo" to 0,
            "luz" to 0,
            "nivel_agua" to 0.0,
            "ultima_actualizacion" to System.currentTimeMillis()
        )

        sensoresRef.setValue(datosSensores)
            .addOnSuccessListener {
                Log.d("VincularDispositivo", "Estructura de sensores creada")
            }
            .addOnFailureListener { e ->
                Log.e("VincularDispositivo", "Error al crear estructura de sensores", e)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            socket?.close()
        } catch (_: Exception) { }
    }
}