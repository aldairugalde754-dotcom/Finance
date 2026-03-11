package com.goccorp.finance.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.goccorp.finance.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ReportesFragment : Fragment() {

    private lateinit var userEmail: String
    private val db = FirebaseFirestore.getInstance()

    private lateinit var pieChart: PieChart
    private lateinit var tvEmptyMessage: TextView

    private lateinit var tvMonth: TextView
    private lateinit var btnPrevMonth: MaterialButton
    private lateinit var btnNextMonth: MaterialButton

    private var selectedMonth = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_reportes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userEmail = arguments?.getString("email") ?: ""

        pieChart = view.findViewById(R.id.pieChart)
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage)

        tvMonth = view.findViewById(R.id.tvMonth)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)

        setupPieChart()
        updateMonthText()
        loadReportData()

        btnPrevMonth.setOnClickListener {

            selectedMonth.add(Calendar.MONTH, -1)
            updateMonthText()
            loadReportData()
        }

        btnNextMonth.setOnClickListener {

            selectedMonth.add(Calendar.MONTH, 1)
            updateMonthText()
            loadReportData()
        }
    }

    private fun setupPieChart() {

        pieChart.apply {

            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)

            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)

            animateY(1200)
        }
    }

    private fun loadReportData() {

        db.collection("Users")
            .document(userEmail)
            .collection("transactions")
            .whereEqualTo("type", "expense")
            .get()
            .addOnSuccessListener { docs ->

                val categoryMap = mutableMapOf<String, Float>()

                for (doc in docs) {

                    val timestamp = doc.getTimestamp("date") ?: continue

                    val cal = Calendar.getInstance()
                    cal.time = timestamp.toDate()

                    val month = cal.get(Calendar.MONTH)
                    val year = cal.get(Calendar.YEAR)

                    val selectedM = selectedMonth.get(Calendar.MONTH)
                    val selectedY = selectedMonth.get(Calendar.YEAR)

                    if (month == selectedM && year == selectedY) {

                        val cat = doc.getString("category") ?: "Otros"
                        val amount = doc.getDouble("amount")?.toFloat() ?: 0f

                        categoryMap[cat] =
                            categoryMap.getOrDefault(cat, 0f) + amount
                    }
                }

                if (categoryMap.isEmpty()) {
                    showEmpty(true)
                } else {
                    showEmpty(false)
                    updateChart(categoryMap)
                }
            }
    }

    private fun showEmpty(isEmpty: Boolean) {

        if (isEmpty) {
            pieChart.visibility = View.GONE
            tvEmptyMessage.visibility = View.VISIBLE
        } else {
            pieChart.visibility = View.VISIBLE
            tvEmptyMessage.visibility = View.GONE
        }
    }

    private fun updateChart(data: Map<String, Float>) {

        val entries = ArrayList<PieEntry>()

        for ((category, total) in data) {
            entries.add(PieEntry(total, category))
        }

        val dataSet = PieDataSet(entries, "Gastos por Categoría").apply {

            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 14f
            valueTextColor = Color.BLACK
        }

        val pieData = PieData(dataSet)

        pieChart.data = pieData
        pieChart.invalidate()
    }

    private fun updateMonthText() {

        val months = arrayOf(
            "Enero","Febrero","Marzo","Abril","Mayo","Junio",
            "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
        )

        val month = months[selectedMonth.get(Calendar.MONTH)]
        val year = selectedMonth.get(Calendar.YEAR)

        tvMonth.text = "$month $year"
    }
}