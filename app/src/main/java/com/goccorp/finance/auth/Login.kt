package com.goccorp.finance.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.goccorp.finance.databinding.ActivityLoginBinding
import com.goccorp.finance.view.MainView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = AuthRepository(Firebase.firestore)
        viewModel = AuthViewModel(repository)

        setupWindowInsets()
        observeViewModel()
        startListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> {
                    // Show progress if needed
                }
                is AuthState.Success -> {
                    Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainView::class.java).apply {
                        putExtra("email", state.email)
                    }
                    startActivity(intent)
                    finish()
                }
                is AuthState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is AuthState.Idle -> {
                    // Do nothing
                }
            }
        }
    }

    private fun startListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        binding.btnSignUp.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        binding.tvForgot.setOnClickListener {
            showRecoveryStep1()
        }
    }

    private fun showRecoveryStep1() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Ingresa tu correo primero", Toast.LENGTH_LONG).show()
            return
        }

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(64, 24, 64, 0)
        }

        val inputPhone = EditText(this).apply {
            hint = "Teléfono registrado"
            inputType = InputType.TYPE_CLASS_PHONE
            layoutParams = params
        }

        container.addView(inputPhone)

        MaterialAlertDialogBuilder(this)
            .setTitle("Verificación")
            .setMessage("Ingresa tu teléfono registrado")
            .setView(container)
            .setPositiveButton("Verificar") { _, _ ->
                val phoneInput = inputPhone.text.toString().trim()
                viewModel.recoverPassword(email, phoneInput) {
                    showRecoveryStep2(email)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showRecoveryStep2(email: String) {
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(64, 24, 64, 0)
        }

        val inputPass = EditText(this).apply {
            hint = "Nueva contraseña"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = params
        }

        container.addView(inputPass)

        MaterialAlertDialogBuilder(this)
            .setTitle("Nueva contraseña")
            .setView(container)
            .setPositiveButton("Actualizar") { _, _ ->
                val newPass = inputPass.text.toString()
                if (newPass.length < 6) {
                    Toast.makeText(this, "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.updatePassword(email, newPass)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
