package com.example.jardnzen_app.Fragmentos

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.jardnzen_app.Adaptadores.HistorialAdapter
import com.example.jardnzen_app.Modelos.RegistroHistorial
import com.locochones.jardnzen_app.databinding.FragmentEstadisticasBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FragmentEstadisticas : Fragment() {

    private lateinit var binding: FragmentEstadisticasBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val historialList = mutableListOf<RegistroHistorial>()
    private lateinit var historialAdapter: HistorialAdapter

    // Datos para gráficas
    private val humedadEntries = ArrayList<Entry>()
    private val temperaturaEntries = ArrayList<Entry>()
    private val luzEntries = ArrayList<Entry>()

    // Control de datos
    private var dataCount = 0
    private val maxDataPoints = 10 // Solo 10 puntos para mejor visualización

    private val deviceId = "JardinZenESP32"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEstadisticasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Usuarios")

        // INICIALIZAR GRÁFICAS CON DATOS VACÍOS
        inicializarGraficas()
        setupRecyclerView()

        val user = auth.currentUser
        if (user != null) {
            cargarDatosEstadisticas(user.uid)
            cargarHistorial(user.uid)
        }
    }

    private fun inicializarGraficas() {
        setupCharts()

        // Inicializar con datos vacíos para que se vean las líneas
        inicializarDataSet(humedadEntries, binding.chartHumedad, "Humedad", Color.parseColor("#2E7D32"))
        inicializarDataSet(temperaturaEntries, binding.chartTemperatura, "Temperatura", Color.parseColor("#D32F2F"))
        inicializarDataSet(luzEntries, binding.chartLuz, "Luz", Color.parseColor("#FFA000"))
    }

    private fun inicializarDataSet(entries: ArrayList<Entry>,
                                   chart: com.github.mikephil.charting.charts.LineChart,
                                   label: String, color: Int) {
        // Agregar punto inicial en 0 para que se dibuje la línea
        entries.add(Entry(0f, 0f))

        val dataSet = LineDataSet(entries, label)
        configurarDataSet(dataSet, color)

        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.invalidate()
    }

    private fun setupCharts() {
        configurarChart(binding.chartHumedad, "Humedad")
        configurarChart(binding.chartTemperatura, "Temperatura")
        configurarChart(binding.chartLuz, "Luz")
    }

    private fun configurarChart(chart: com.github.mikephil.charting.charts.LineChart, tipo: String) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setDragEnabled(true)
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.setDrawGridBackground(false)

        // Configurar eje X
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(true)
        xAxis.setDrawAxisLine(true)
        xAxis.labelCount = 5

        // Configurar eje Y según el tipo
        configurarEjeY(chart, tipo)

        // Configurar eje Y derecho
        val yAxisRight = chart.axisRight
        yAxisRight.isEnabled = false

        chart.legend.isEnabled = false

        // IMPORTANTE: Habilitar el dibujo de valores
        chart.setDrawMarkers(true)
        chart.isHighlightPerDragEnabled = true

        chart.animateXY(1000, 1000)
    }

    private fun configurarEjeY(chart: com.github.mikephil.charting.charts.LineChart, tipo: String) {
        val yAxis = chart.axisLeft
        yAxis.setDrawGridLines(true)
        yAxis.granularity = 1f
        yAxis.setDrawAxisLine(true)

        when (tipo) {
            "Humedad" -> {
                yAxis.axisMinimum = 0f
                yAxis.axisMaximum = 100f
                yAxis.labelCount = 6
            }
            "Luz" -> {
                yAxis.axisMinimum = 0f
                yAxis.axisMaximum = 100f
                yAxis.labelCount = 6
            }
            "Temperatura" -> {
                yAxis.axisMinimum = 0f
                yAxis.axisMaximum = 40f
                yAxis.labelCount = 5
            }
        }
    }

    private fun configurarDataSet(dataSet: LineDataSet, color: Int) {
        dataSet.color = color
        dataSet.valueTextColor = Color.BLACK
        dataSet.lineWidth = 3f // Línea más gruesa
        dataSet.setCircleColor(color)
        dataSet.circleRadius = 5f // Puntos más grandes
        dataSet.setDrawCircleHole(false)
        dataSet.mode = LineDataSet.Mode.LINEAR // Líneas rectas entre puntos
        dataSet.setDrawValues(true) // MOSTRAR VALORES NUMÉRICOS
        dataSet.valueTextSize = 10f
        dataSet.setDrawFilled(false) // No relleno

        // IMPORTANTE: Asegurar que se dibujen las líneas
        dataSet.setDrawCircles(true)
        dataSet.setDrawHorizontalHighlightIndicator(true)
        dataSet.setDrawVerticalHighlightIndicator(true)
    }

    private fun setupRecyclerView() {
        historialAdapter = HistorialAdapter(historialList)
        binding.recyclerHistorial.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historialAdapter
        }
    }

    private fun cargarDatosEstadisticas(uid: String) {
        val sensoresRef = database.child(uid).child("dispositivos").child(deviceId).child("sensores")

        // Usar ChildEventListener para actualizaciones en tiempo real
        sensoresRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                procesarDatosSensores(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                procesarDatosSensores(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun procesarDatosSensores(snapshot: DataSnapshot) {
        when (snapshot.key) {
            "humedad_suelo" -> {
                val humedad = snapshot.getValue(Int::class.java) ?: 0
                actualizarGrafica(humedadEntries, humedad.toFloat(), binding.chartHumedad, "Humedad", Color.parseColor("#2E7D32"))
            }
            "temperatura" -> {
                val temperatura = snapshot.getValue(Double::class.java) ?: 0.0
                actualizarGrafica(temperaturaEntries, temperatura.toFloat(), binding.chartTemperatura, "Temperatura", Color.parseColor("#D32F2F"))
            }
            "luz" -> {
                val luz = snapshot.getValue(Int::class.java) ?: 0
                actualizarGrafica(luzEntries, luz.toFloat(), binding.chartLuz, "Luz", Color.parseColor("#FFA000"))
            }
        }
    }

    private fun actualizarGrafica(entries: ArrayList<Entry>, valor: Float,
                                  chart: com.github.mikephil.charting.charts.LineChart,
                                  label: String, color: Int) {
        // Incrementar contador
        dataCount++

        // Limitar a maxDataPoints puntos
        if (entries.size >= maxDataPoints) {
            entries.removeAt(0)
            // Reindexar los puntos restantes
            for (i in entries.indices) {
                entries[i] = Entry(i.toFloat(), entries[i].y)
            }
        }

        // Agregar nuevo dato
        entries.add(Entry(entries.size.toFloat(), valor))

        // Crear y configurar dataset
        val dataSet = LineDataSet(entries, label)
        configurarDataSet(dataSet, color)

        // Actualizar gráfica
        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.invalidate() // FORZAR ACTUALIZACIÓN

        // Agregar al historial cada 5 datos para no saturar
        if (dataCount % 5 == 0) {
            agregarRegistroHistorial()
        }
    }

    private fun agregarRegistroHistorial() {
        val fechaActual = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())

        val registro = RegistroHistorial(
            fecha = fechaActual,
            temperatura = temperaturaEntries.lastOrNull()?.y?.toDouble() ?: 0.0,
            humedad = humedadEntries.lastOrNull()?.y?.toInt() ?: 0,
            luz = luzEntries.lastOrNull()?.y?.toInt() ?: 0
        )

        // Agregar al inicio de la lista
        historialList.add(0, registro)

        // Mantener solo los últimos 10 registros
        if (historialList.size > 10) {
            historialList.removeAt(historialList.size - 1)
        }

        historialAdapter.notifyDataSetChanged()
    }

    private fun cargarHistorial(uid: String) {
        val historialRef = database.child(uid).child("historial")

        historialRef.limitToLast(10).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historialList.clear()

                for (historialSnapshot in snapshot.children) {
                    val registro = historialSnapshot.getValue(RegistroHistorial::class.java)
                    registro?.let {
                        it.id = historialSnapshot.key ?: ""
                        historialList.add(it)
                    }
                }

                historialList.sortByDescending { it.fecha }
                historialAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar error
            }
        })
    }

    // Formateador para el eje X
    inner class HourAxisValueFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return "${value.toInt()}"
        }
    }
}