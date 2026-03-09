package com.goccorp.finance.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goccorp.finance.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class PresupuestosFragment : Fragment() {

    private lateinit var userEmail: String
    private val db = FirebaseFirestore.getInstance()
    private val budgetList = mutableListOf<Map<String, Any>>()
    private val currentExpenses = mutableMapOf<String, Double>() // Categoría -> Suma de Gastos
    private lateinit var adapter: BudgetAdapter
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_presupuestos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userEmail = arguments?.getString("email") ?: ""

        val rv = view.findViewById<RecyclerView>(R.id.rvPresupuestos)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddBudget)

        adapter = BudgetAdapter(budgetList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        fab.setOnClickListener { showBudgetDialog() }

        loadCurrentExpensesAndBudgets()
    }

    private fun loadCurrentExpensesAndBudgets() {
        // Primero cargamos los gastos actuales para comparar
        db.collection("Users").document(userEmail).collection("transactions")
            .whereEqualTo("type", "expense")
            .get()
            .addOnSuccessListener { docs ->
                currentExpenses.clear()
                for (doc in docs) {
                    val cat = doc.getString("category") ?: "Otros"
                    val amount = doc.getDouble("amount") ?: 0.0
                    currentExpenses[cat] = currentExpenses.getOrDefault(cat, 0.0) + amount
                }
                loadBudgets()
            }
    }

    private fun loadBudgets() {
        db.collection("Users").document(userEmail).collection("budgets")
            .addSnapshotListener { value, _ ->
                budgetList.clear()
                value?.forEach { doc ->
                    val data = doc.data.toMutableMap()
                    data["id"] = doc.id
                    budgetList.add(data)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showBudgetDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Establecer Presupuesto")

        val view = layoutInflater.inflate(R.layout.dialog_add_gasto, null)
        val etAmount = view.findViewById<EditText>(R.id.etGastoAmount)
        val spCategory = view.findViewById<Spinner>(R.id.spCategory)
        view.findViewById<EditText>(R.id.etGastoTitle).visibility = View.GONE // No necesitamos título aquí

        val categorias = arrayOf("Comida", "Transporte", "Renta", "Ocio", "Salud")
        spCategory.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categorias
        )

        builder.setView(view)
        builder.setPositiveButton("Guardar") { _, _ ->
            val category = spCategory.selectedItem.toString()
            val limit = etAmount.text.toString().toDoubleOrNull() ?: 0.0

            // VALIDACIÓN DE DUPLICADOS
            if (budgetList.any { it["category"] == category }) {
                Toast.makeText(requireContext(), "Ya existe un presupuesto para $category", Toast.LENGTH_LONG).show()
                return@setPositiveButton
            }

            if (limit > 0) {
                val budgetData = hashMapOf("category" to category, "limit" to limit)
                db.collection("Users").document(userEmail).collection("budgets").add(budgetData)
            }
        }
        builder.show()
    }

    inner class BudgetAdapter(private val list: List<Map<String, Any>>) : RecyclerView.Adapter<BudgetAdapter.BViewHolder>() {
        inner class BViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvBudgetName)
            val status: TextView = v.findViewById(R.id.tvBudgetStatus)
            val pb: ProgressBar = v.findViewById(R.id.pbBudget)
            val warning: TextView = v.findViewById(R.id.tvBudgetWarning)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = BViewHolder(
            LayoutInflater.from(p.context).inflate(R.layout.item_presupuesto, p, false)
        )

        override fun onBindViewHolder(h: BViewHolder, p: Int) {
            val item = list[p]
            val cat = item["category"].toString()
            val limit = item["limit"] as Double
            val spent = currentExpenses[cat] ?: 0.0

            h.name.text = cat
            h.status.text = "${currencyFormatter.format(spent)} / ${currencyFormatter.format(limit)}"

            h.pb.max = limit.toInt()
            h.pb.progress = spent.toInt()

            if (spent > limit) {
                h.warning.visibility = View.VISIBLE
                h.warning.text = "¡Exceso de ${currencyFormatter.format(spent - limit)}!"
            } else {
                h.warning.visibility = View.GONE
            }

            h.itemView.setOnLongClickListener {
                db.collection("Users").document(userEmail).collection("budgets").document(item["id"].toString()).delete()
                true
            }
        }
        override fun getItemCount() = list.size
    }
}