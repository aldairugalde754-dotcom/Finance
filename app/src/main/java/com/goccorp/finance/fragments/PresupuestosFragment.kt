package com.goccorp.finance.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goccorp.finance.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.*

class PresupuestosFragment : Fragment() {

    private lateinit var userEmail: String
    private val db = FirebaseFirestore.getInstance()

    private val budgetList = mutableListOf<Map<String, Any>>()
    private val categoryList = mutableListOf<String>()
    private val currentExpenses = mutableMapOf<String, Double>()

    private lateinit var adapter: BudgetAdapter
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    // Categorías por defecto (si Firestore no tiene)
    private val defaultCategories = listOf("Comida", "Transporte", "Renta", "Ocio", "Salud")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

        // Cargamos categorías y datos
        loadCategories()
        loadCurrentExpensesAndBudgets()
    }

    // -----------------------------
    // CARGAR CATEGORÍAS (y usar defaults si está vacío)
    // -----------------------------
    private fun loadCategories() {
        db.collection("Users").document(userEmail)
            .collection("categories")
            .addSnapshotListener { value, _ ->
                // Siempre empezamos por los defaults para asegurar que haya opciones
                categoryList.clear()
                categoryList.addAll(defaultCategories)

                // Añadimos las que haya en Firestore evitando duplicados
                value?.forEach { doc ->
                    val name = doc.getString("name") ?: return@forEach
                    if (!categoryList.contains(name)) categoryList.add(name)
                }
            }
    }

    // -----------------------------
    // Crear categoría en Firestore
    // -----------------------------
    private fun createCategory(name: String, onDone: (() -> Unit)? = null) {
        // Guardar en Firestore
        val data = hashMapOf("name" to name)
        db.collection("Users").document(userEmail)
            .collection("categories")
            .add(data)
            .addOnSuccessListener {
                // actualizar lista local inmediatamente para UX instantánea
                if (!categoryList.contains(name)) categoryList.add(name)
                onDone?.invoke()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error guardando categoría", Toast.LENGTH_SHORT).show()
            }
    }

    // -----------------------------
    // CARGAR GASTOS Y PRESUPUESTOS
    // -----------------------------
    private fun loadCurrentExpensesAndBudgets() {
        db.collection("Users").document(userEmail)
            .collection("transactions")
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
        db.collection("Users").document(userEmail)
            .collection("budgets")
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

    // -----------------------------
    // DIALOGO AGREGAR PRESUPUESTO (con manejo de nueva categoría inline)
    // -----------------------------
    private fun showBudgetDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_gasto, null)

        // ocultar la parte de fecha (según pediste)
        dialogView.findViewById<View>(R.id.tvDate)?.visibility = View.GONE
        dialogView.findViewById<View>(R.id.btnDate)?.visibility = View.GONE

        val etAmount = dialogView.findViewById<EditText>(R.id.etGastoAmount)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spCategory)

        // Si categoryList estuviera vacío (por si acaso), cargamos defaults
        if (categoryList.isEmpty()) {
            categoryList.addAll(defaultCategories)
        }

        // Adapter local para spinner (se actualizará dinámicamente)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categoryList.toMutableList())
        spCategory.adapter = spinnerAdapter

        builder.setTitle("Establecer Presupuesto")
        builder.setView(dialogView)

        // usamos null para los listeners y los asignamos después para controlar el cierre
        builder.setPositiveButton("Guardar", null)
        builder.setNeutralButton("Nueva Categoría", null)
        builder.setNegativeButton("Cancelar", null)

        val dialog = builder.create()
        dialog.show()

        // BOTÓN NUEVA CATEGORÍA: no cerrará el diálogo principal; abre un pequeño input
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            // Mostrar otro diálogo para crear la categoría
            val input = EditText(requireContext())
            input.hint = "Nombre de categoría"

            AlertDialog.Builder(requireContext())
                .setTitle("Nueva Categoría")
                .setView(input)
                .setPositiveButton("Guardar") { dd, _ ->
                    val name = input.text.toString().trim()
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Ingresa un nombre válido", Toast.LENGTH_SHORT).show()
                    } else {
                        // Crear categoría en Firestore y actualizar spinner cuando termine
                        createCategory(name) {
                            // actualizar adapter del spinner en el hilo UI
                            if (!categoryList.contains(name)) categoryList.add(name)
                            // recrear adapter (más simple y seguro)
                            val newAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categoryList.toMutableList())
                            spCategory.adapter = newAdapter
                            // seleccionar la nueva categoría
                            spCategory.setSelection(newAdapter.getPosition(name))
                            Toast.makeText(requireContext(), "Categoría '$name' agregada", Toast.LENGTH_SHORT).show()
                        }
                    }
                    dd.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // BOTÓN GUARDAR: validaciones y guardado; si falla, NO cierra el diálogo
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selected = spCategory.selectedItem
            val limit = etAmount.text.toString().toDoubleOrNull() ?: 0.0

            if (selected == null) {
                Toast.makeText(requireContext(), "Debes agregar y seleccionar una categoría", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val category = selected.toString()

            if (limit <= 0.0) {
                Toast.makeText(requireContext(), "Ingresa un monto válido mayor a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar duplicados
            if (budgetList.any { it["category"] == category }) {
                Toast.makeText(requireContext(), "Ya existe un presupuesto para $category", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Guardar presupuesto
            val budgetData = hashMapOf("category" to category, "limit" to limit)
            db.collection("Users").document(userEmail).collection("budgets").add(budgetData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Presupuesto guardado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error guardando presupuesto", Toast.LENGTH_SHORT).show()
                }
        }

        // CANCEL simplemente cierra (comportamiento por defecto)
    }

    // -----------------------------
    // ADAPTER DE PRESUPUESTOS
    // -----------------------------
    inner class BudgetAdapter(private val list: List<Map<String, Any>>) : RecyclerView.Adapter<BudgetAdapter.BViewHolder>() {

        inner class BViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvBudgetName)
            val status: TextView = v.findViewById(R.id.tvBudgetStatus)
            val pb: ProgressBar = v.findViewById(R.id.pbBudget)
            val warning: TextView = v.findViewById(R.id.tvBudgetWarning)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): BViewHolder {
            return BViewHolder(LayoutInflater.from(p.context).inflate(R.layout.item_presupuesto, p, false))
        }

        override fun onBindViewHolder(h: BViewHolder, p: Int) {
            val item = list[p]
            val cat = item["category"].toString()
            val limit = (item["limit"] as? Number)?.toDouble() ?: 0.0
            val spent = currentExpenses[cat] ?: 0.0

            h.name.text = cat
            h.status.text = "${currencyFormatter.format(spent)} / ${currencyFormatter.format(limit)}"

            h.pb.max = if (limit.toInt() > 0) limit.toInt() else 1
            h.pb.progress = spent.toInt()

            if (spent > limit) {
                h.warning.visibility = View.VISIBLE
                h.warning.text = "¡Exceso de ${currencyFormatter.format(spent - limit)}!"
            } else {
                h.warning.visibility = View.GONE
            }

            // Eliminar presupuesto (long press)
            h.itemView.setOnLongClickListener {
                val id = item["id"]?.toString() ?: return@setOnLongClickListener true
                db.collection("Users").document(userEmail).collection("budgets").document(id).delete()
                true
            }
        }

        override fun getItemCount() = list.size
    }
}