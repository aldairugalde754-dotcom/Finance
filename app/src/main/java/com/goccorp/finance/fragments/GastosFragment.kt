package com.goccorp.finance.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goccorp.finance.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class GastosFragment : Fragment() {

    private lateinit var userEmail: String
    private val db = Firebase.firestore
    private val gastosList = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: GastoAdapter
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_gastos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        userEmail = arguments?.let { it["email"]?.toString() } ?: ""

        val rv = view.findViewById<RecyclerView>(R.id.rvGastos)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddExpense)

        adapter = GastoAdapter(gastosList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        fab.setOnClickListener { showGastoDialog(null) }

        loadGastos()
    }

    private fun loadGastos() {
        db.collection("Users")
            .document(userEmail)
            .collection("transactions")
            .whereEqualTo("type", "expense")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { value, _ ->
                if (value == null) return@addSnapshotListener

                gastosList.clear()
                for (doc in value.documents) {
                    val data = doc.data?.toMutableMap() ?: continue
                    data["id"] = doc.id
                    gastosList.add(data)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showGastoDialog(gastoData: Map<String, Any>?) {
        val view = layoutInflater.inflate(R.layout.dialog_add_gasto, null)

        val etTitle = view.findViewById<EditText>(R.id.etGastoTitle)
        val etAmount = view.findViewById<EditText>(R.id.etGastoAmount)
        val spCategory = view.findViewById<Spinner>(R.id.spCategory)
        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val btnDate = view.findViewById<Button>(R.id.btnDate)

        val categorias = arrayOf(
            "Comida",
            "Transporte",
            "Renta",
            "Ocio",
            "Salud",
            "Servicios",
            "Educación",
            "Suscripciones",
            "Ropa",
            "Mascotas",
            "Regalos",
            "Ahorro"
        )

        spCategory.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categorias
        )

        val cal = Calendar.getInstance()
        var selectedTimestamp: Timestamp? = null
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        gastoData?.let { data ->
            etTitle.setText(data["title"].toString())
            etAmount.setText(data["amount"].toString())

            val pos = categorias.indexOf(data["category"].toString())
            if (pos >= 0) spCategory.setSelection(pos)

            (data["date"] as? Timestamp)?.let { ts ->
                cal.time = ts.toDate()
                selectedTimestamp = ts
            }
        }

        tvDate.text = "Fecha: ${sdf.format(cal.time)}"

        btnDate.setOnClickListener {
            val dp = DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    cal.set(y, m, d, 0, 0)
                    selectedTimestamp = Timestamp(cal.time)
                    tvDate.text = "Fecha: ${sdf.format(cal.time)}"
                },
                cal[Calendar.YEAR],
                cal[Calendar.MONTH],
                cal[Calendar.DAY_OF_MONTH]
            )
            dp.show()
        }

        AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val title = etTitle.text.toString()
                val amount = etAmount.text.toString().toDoubleOrNull()
                val category = spCategory.selectedItem.toString()

                if (title.isEmpty() || amount == null) {
                    Toast.makeText(requireContext(), "Datos inválidos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val data = hashMapOf(
                    "title" to title,
                    "amount" to amount,
                    "category" to category,
                    "type" to "expense",
                    "date" to (selectedTimestamp ?: FieldValue.serverTimestamp())
                )

                if (gastoData == null) {
                    db.collection("Users").document(userEmail)
                        .collection("transactions")
                        .add(data)
                        .addOnSuccessListener {
                            updateUserBalance(-amount)
                        }
                } else {
                    val oldAmount = gastoData["amount"] as Double
                    db.collection("Users").document(userEmail)
                        .collection("transactions")
                        .document(gastoData["id"].toString())
                        .set(data)
                        .addOnSuccessListener {
                            updateUserBalance(oldAmount - amount)
                        }
                }
            }
            .apply {
                gastoData?.let { data ->
                    setNeutralButton("Eliminar") { _, _ ->
                        val amount = data["amount"] as Double
                        db.collection("Users").document(userEmail)
                            .collection("transactions")
                            .document(data["id"].toString())
                            .delete()
                            .addOnSuccessListener {
                                updateUserBalance(amount)
                            }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateUserBalance(diff: Double) {
        db.collection("Users")
            .document(userEmail)
            .update("Balance", FieldValue.increment(diff))
    }

    inner class GastoAdapter(private val list: List<Map<String, Any>>) :
        RecyclerView.Adapter<GastoAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.tvGastoTitle)
            val category: TextView = v.findViewById(R.id.tvGastoCategory)
            val amount: TextView = v.findViewById(R.id.tvGastoAmount)
            val date: TextView = v.findViewById(R.id.tvDate)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(p.context).inflate(R.layout.item_gasto, p, false))

        override fun onBindViewHolder(h: ViewHolder, i: Int) {
            val item = list[i]

            h.title.text = item["title"].toString()
            h.category.text = item["category"].toString()

            val amount = item["amount"] as Double
            h.amount.text = "-${currencyFormatter.format(amount)}"

            (item["date"] as? Timestamp)?.let { ts ->
                val date = ts.toDate()
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                h.date.text = format.format(date)
            }

            h.itemView.setOnClickListener {
                showGastoDialog(item)
            }
        }

        override fun getItemCount() = list.size
    }
}
