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
import com.goccorp.finance.view.MainView
import com.goccorp.finance.auth.Register
import com.goccorp.finance.databinding.ActivityLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.security.MessageDigest

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startListeners()
    }

    private fun hashPassword(password: String): String {

        val salt = "financeApp2026SecureSalt"
        val saltedPassword = password + salt

        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(saltedPassword.toByteArray())

        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun startListeners() {

        // LOGIN
        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val passwordInput = binding.etPassword.text.toString()

            if (email.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hashedInput = hashPassword(passwordInput)

            db.collection("Users").document(email).get()
                .addOnSuccessListener { document ->

                    if (document.exists()) {

                        val storedHash = document.getString("Password")

                        if (hashedInput == storedHash) {

                            Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this, MainView::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)

                            finish()

                        } else {
                            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(this, "El usuario no existe", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // IR A REGISTRO
        binding.btnSignUp.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        // RECUPERAR CONTRASEÑA
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
        )

        params.setMargins(64, 24, 64, 0)

        val inputPhone = EditText(this)
        inputPhone.hint = "Teléfono registrado"
        inputPhone.inputType = InputType.TYPE_CLASS_PHONE
        inputPhone.layoutParams = params

        container.addView(inputPhone)

        MaterialAlertDialogBuilder(this)
            .setTitle("Verificación")
            .setMessage("Ingresa tu teléfono registrado")
            .setView(container)
            .setPositiveButton("Verificar") { _, _ ->

                val phoneInput = inputPhone.text.toString().trim()

                db.collection("Users").document(email).get()
                    .addOnSuccessListener { doc ->

                        if (doc.exists() && phoneInput == doc.getString("Phone")) {
                            showRecoveryStep2(email)
                        } else {
                            Toast.makeText(this, "Datos incorrectos", Toast.LENGTH_SHORT).show()
                        }
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
        )

        params.setMargins(64, 24, 64, 0)

        val inputPass = EditText(this)
        inputPass.hint = "Nueva contraseña"
        inputPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        inputPass.layoutParams = params

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

                val hashedNewPass = hashPassword(newPass)

                db.collection("Users")
                    .document(email)
                    .update("Password", hashedNewPass)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Contraseña actualizada",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}