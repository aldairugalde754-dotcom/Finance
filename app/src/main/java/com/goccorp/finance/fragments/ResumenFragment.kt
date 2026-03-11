package com.goccorp.finance.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.goccorp.finance.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class Transaction(
    val title: String,
    val amount: Double,
    val type: String,
    val date: Timestamp?
)

class ResumenFragment : Fragment() {

    private val transactions = mutableListOf<Transaction>()
    private lateinit var adapter: TransactionAdapter

    private lateinit var tvTotalBalance: TextView
    private lateinit var lineChart: LineChart

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val db = Firebase.firestore
    private var userEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_resumen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        userEmail = arguments?.getString("email") ?: ""

        tvTotalBalance = view.findViewById(R.id.tvTotalBalance)
        lineChart = view.findViewById(R.id.lineChart)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val btnIncome = view.findViewById<MaterialButton>(R.id.btnIncome)
        val btnExpense = view.findViewById<MaterialButton>(R.id.btnExpense)

        adapter = TransactionAdapter(transactions)
        recyclerView.adapter = adapter

        setupChart()

        btnIncome.setOnClickListener { openIngresoFragment() }
        btnExpense.setOnClickListener { openGastoFragment() }

        loadBalance()
        loadTransactions()
    }

    private fun openIngresoFragment() {
        val fragment = IngresosFragment()
        fragment.arguments = Bundle().apply {
            putString("email", userEmail)
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openGastoFragment() {
        val fragment = GastosFragment()
        fragment.arguments = Bundle().apply {
            putString("email", userEmail)
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadTransactions() {
        db.collection("Users").document(userEmail)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { value, _ ->
                if (value == null) return@addSnapshotListener

                transactions.clear()
                for (doc in value.documents) {
                    val title = doc.getString("title") ?: ""
                    val amount = doc.getDouble("amount") ?: 0.0
                    
                    // Normalización: Si es 'income' lo convertimos a 'Ingreso' para la lógica interna
                    val rawType = doc.getString("type") ?: "Gasto"
                    val type = if (rawType == "income" || rawType == "Ingreso") "Ingreso" else "Gasto"

                    val date = doc.getTimestamp("date")

                    transactions.add(Transaction(title, amount, type, date))
                }

                adapter.notifyDataSetChanged()
                updateDashboard()
            }
    }

    private fun loadBalance() {
        db.collection("Users").document(userEmail).get()
            .addOnSuccessListener {
                val balance = it.getDouble("Balance") ?: 0.0
                tvTotalBalance.text = currencyFormatter.format(balance)
            }
    }

    private fun updateDashboard() {
        val entries = ArrayList<Entry>()
        var currentBalance = 0.0f
        val chronologicalTx = transactions.reversed()

        entries.add(Entry(0f, 0f))

        chronologicalTx.forEachIndexed { index, tx ->
            val value = if (tx.type == "Ingreso") {
                tx.amount.toFloat()
            } else {
                -tx.amount.toFloat()
            }
            currentBalance += value
            entries.add(Entry((index + 1).toFloat(), currentBalance))
        }

        val dataSet = LineDataSet(entries, "Balance Histórico").apply {
            color = Color.parseColor("#003087")
            valueTextColor = Color.BLACK
            lineWidth = 3f
            setDrawCircles(true)
            circleColors = listOf(Color.parseColor("#0079C1"))
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#800079C1")
        }

        lineChart.data = LineData(dataSet)
        lineChart.invalidate()
    }

    private fun setupChart() {
        lineChart.apply {
            description = Description().apply { text = "" }
            setDrawGridBackground(false)
            xAxis.isEnabled = false
            axisRight.isEnabled = false
            legend.isEnabled = false
            animateX(1000)
        }
    }

    inner class TransactionAdapter(private val list: List<Transaction>) :
        RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvIcon: TextView = view.findViewById(R.id.tvIcon)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tx = list[position]
            holder.tvTitle.text = tx.title
            holder.tvCategory.text = tx.type

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            tx.date?.let {
                holder.tvDate.text = sdf.format(it.toDate())
            }

            if (tx.type == "Ingreso") {
                holder.tvIcon.text = "💰"
                holder.tvAmount.text = "+${currencyFormatter.format(tx.amount)}"
                holder.tvAmount.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                holder.tvIcon.text = "💸"
                holder.tvAmount.text = "-${currencyFormatter.format(tx.amount)}"
                holder.tvAmount.setTextColor(Color.parseColor("#E53935"))
            }
        }

        override fun getItemCount() = list.size
    }
}
