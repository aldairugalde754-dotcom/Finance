package com.goccorp.finance.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goccorp.finance.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class IngresosFragment : Fragment() {

    private lateinit var userEmail: String
    private val db = FirebaseFirestore.getInstance()
    private val ingresosList = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: IngresoAdapter
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_ingresos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        userEmail = arguments?.getString("email") ?: ""

        val rv = view.findViewById<RecyclerView>(R.id.rvIngresos)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddIncome)

        adapter = IngresoAdapter(ingresosList)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        fab.setOnClickListener { showIngresoDialog(null) }

        loadIngresos()
    }

    private fun loadIngresos() {

        db.collection("Users")
            .document(userEmail)
            .collection("transactions")
            .whereEqualTo("type","income")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { value, _ ->

                if (value == null) return@addSnapshotListener

                ingresosList.clear()

                for (doc in value.documents) {
                    val data = doc.data?.toMutableMap() ?: continue
                    data["id"] = doc.id
                    ingresosList.add(data)
                }

                adapter.notifyDataSetChanged()
            }
    }

    private fun showIngresoDialog(data: Map<String, Any>?) {

        val builder = AlertDialog.Builder(requireContext())

        val view = layoutInflater.inflate(R.layout.dialog_add_gasto,null)

        val etTitle = view.findViewById<EditText>(R.id.etGastoTitle)
        val etAmount = view.findViewById<EditText>(R.id.etGastoAmount)
        val spSource = view.findViewById<Spinner>(R.id.spCategory)
        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val btnDate = view.findViewById<Button>(R.id.btnDate)

        val fuentes = arrayOf("Sueldo","Venta","Regalo","Inversión","Otros")

        spSource.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            fuentes
        )

        val cal = Calendar.getInstance()
        var selectedTimestamp: Timestamp? = null

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        if (data != null) {

            etTitle.setText(data["title"].toString())
            etAmount.setText(data["amount"].toString())

            val pos = fuentes.indexOf(data["category"].toString())
            if (pos >= 0) spSource.setSelection(pos)

            val ts = data["date"]

            if (ts is Timestamp) {
                cal.time = ts.toDate()
                selectedTimestamp = ts
            }
        }

        tvDate.text = "Fecha: ${sdf.format(cal.time)}"

        btnDate.setOnClickListener {

            val dp = DatePickerDialog(
                requireContext(),
                { _, y, m, d ->

                    cal.set(y,m,d)
                    selectedTimestamp = Timestamp(cal.time)

                    tvDate.text = "Fecha: ${sdf.format(cal.time)}"
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )

            dp.show()
        }

        builder.setView(view)

        builder.setPositiveButton("Guardar") { _, _ ->

            val title = etTitle.text.toString()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val source = spSource.selectedItem.toString()

            if (title.isEmpty() || amount == null) {
                Toast.makeText(requireContext(),"Datos inválidos",Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val map = hashMapOf(
                "title" to title,
                "amount" to amount,
                "category" to source,
                "type" to "income",
                "date" to (selectedTimestamp ?: FieldValue.serverTimestamp())
            )

            if (data == null) {

                db.collection("Users").document(userEmail)
                    .collection("transactions")
                    .add(map)
                    .addOnSuccessListener {
                        updateBalance(amount)
                    }

            } else {

                val old = data["amount"] as Double

                db.collection("Users").document(userEmail)
                    .collection("transactions")
                    .document(data["id"].toString())
                    .set(map)
                    .addOnSuccessListener {
                        updateBalance(amount - old)
                    }
            }
        }

        if (data != null) {

            builder.setNeutralButton("Eliminar") { _, _ ->

                val amount = data["amount"] as Double

                db.collection("Users").document(userEmail)
                    .collection("transactions")
                    .document(data["id"].toString())
                    .delete()
                    .addOnSuccessListener {
                        updateBalance(-amount)
                    }
            }
        }

        builder.setNegativeButton("Cancelar",null)

        builder.show()
    }

    private fun updateBalance(diff: Double) {

        db.collection("Users")
            .document(userEmail)
            .update("Balance", FieldValue.increment(diff))
    }

    inner class IngresoAdapter(private val list: List<Map<String,Any>>) :
        RecyclerView.Adapter<IngresoAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.tvGastoTitle)
            val category: TextView = v.findViewById(R.id.tvGastoCategory)
            val amount: TextView = v.findViewById(R.id.tvGastoAmount)
            val date: TextView = v.findViewById(R.id.tvDate)
        }

        override fun onCreateViewHolder(p:ViewGroup,t:Int):ViewHolder{

            val v = LayoutInflater.from(p.context)
                .inflate(R.layout.item_ingreso,p,false)

            return ViewHolder(v)
        }

        override fun onBindViewHolder(h: ViewHolder, i: Int) {

            val item = list[i]

            h.title.text = item["title"].toString()
            h.category.text = item["category"].toString()

            val amount = item["amount"] as Double
            h.amount.text = "+${currencyFormatter.format(amount)}"
            val timestamp = item["date"]

            if (timestamp is Timestamp) {

                val date = timestamp.toDate()
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                h.date.text = format.format(date)
            }

            h.itemView.setOnClickListener {
                showIngresoDialog(item)
            }
        }

        override fun getItemCount() = list.size
    }
}