package com.goccorp.finance.view

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.goccorp.finance.R
import com.goccorp.finance.auth.Login
import com.goccorp.finance.fragments.GastosFragment
import com.goccorp.finance.fragments.IngresosFragment
import com.goccorp.finance.fragments.PresupuestosFragment
import com.goccorp.finance.fragments.ReportesFragment
import com.goccorp.finance.fragments.ResumenFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView

class MainView : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilita el modo de pantalla completa (Edge-to-Edge)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_view)

        // --- SOLUCIÓN DE INSETS (ZONA SEGURA) ---
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        drawerLayout = findViewById(R.id.drawerLayout)

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())

            // 1. Empuja el contenido del Toolbar hacia abajo para que no lo tape la muesca
            topAppBar.setPadding(0, systemBars.top + displayCutout.top, 0, 0)

            // 2. Ajusta el menú lateral para que los ítems no queden bajo las barras del sistema
            navigationView.setPadding(0, systemBars.top, 0, systemBars.bottom)

            insets
        }

        // --- CONFIGURACIÓN DE DATOS Y UI ---
        userEmail = intent.getStringExtra("email") ?: "Usuario"

        val headerView = navigationView.getHeaderView(0)
        val tvEmailHeader = headerView.findViewById<TextView>(R.id.tvUserEmailHeader)
        tvEmailHeader.text = userEmail

        // Abrir panel lateral desde el botón del Toolbar
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.open_drawer -> {
                    drawerLayout.openDrawer(GravityCompat.END)
                    true
                }
                else -> false
            }
        }

        // Manejar clics en el menú de navegación
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    cerrarSesion()
                }
                else -> {
                    val fragment: Fragment = when (menuItem.itemId) {
                        R.id.nav_resumen -> ResumenFragment()
                        R.id.nav_gastos -> GastosFragment()
                        R.id.nav_ingresos -> IngresosFragment()
                        R.id.nav_presupuestos -> PresupuestosFragment()
                        R.id.nav_reportes -> ReportesFragment()
                        else -> ResumenFragment()
                    }
                    cargarFragment(fragment, menuItem.title.toString())
                }
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        // Control del botón "Atrás"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        // Estado inicial
        if (savedInstanceState == null) {
            cargarFragment(ResumenFragment(), "Resumen Mensual")
            navigationView.setCheckedItem(R.id.nav_resumen)
        }
    }

    private fun cargarFragment(fragment: Fragment, title: String) {
        val bundle = Bundle()
        bundle.putString("email", userEmail)
        fragment.arguments = bundle

        findViewById<MaterialToolbar>(R.id.topAppBar).title = title

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun cerrarSesion() {
        val intent = Intent(this, Login::class.java)
        // Limpia el stack: el usuario no podrá volver al MainView presionando "Atrás"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}