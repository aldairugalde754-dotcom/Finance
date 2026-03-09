package com.goccorp.finance.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.goccorp.finance.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.security.MessageDigest

class Register : AppCompatActivity() {

    private lateinit var tName: EditText
    private lateinit var tEmail: EditText
    private lateinit var tPhone: EditText
    private lateinit var tPassword: EditText
    private lateinit var tConfirmPassword: EditText
    private lateinit var bRegister: MaterialButton
    private lateinit var tvLogin: TextView

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startComponents()
        startListeners()
    }

    private fun startComponents() {
        tName = findViewById(R.id.etName)
        tEmail = findViewById(R.id.etEmail)
        tPhone = findViewById(R.id.etPhone)
        tPassword = findViewById(R.id.etPassword)
        tConfirmPassword = findViewById(R.id.etConfirmPassword)
        bRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
    }

    // HASH + SALT
    private fun hashPassword(password: String): String {
        val salt = "financeApp2026SecureSalt"
        val saltedPassword = password + salt
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(saltedPassword.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun startListeners() {

        bRegister.setOnClickListener {

            val name = tName.text.toString().trim()
            val email = tEmail.text.toString().trim()
            val phone = tPhone.text.toString().trim()
            val password = tPassword.text.toString()
            val confirmPassword = tConfirmPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Debes llenar todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                tPassword.text.clear()
                tConfirmPassword.text.clear()
                return@setOnClickListener
            }

            val encryptedPass = hashPassword(password)

            db.collection("Users").document(email).get()
                .addOnSuccessListener { document ->

                    if (document.exists()) {
                        Toast.makeText(this, "El usuario ya existe", Toast.LENGTH_SHORT).show()
                    } else {

                        val userMap = hashMapOf(
                            "Name" to name,
                            "Phone" to phone,
                            "Password" to encryptedPass,
                            "Balance" to 0.0
                        )

                        db.collection("Users")
                            .document(email)
                            .set(userMap)
                            .addOnSuccessListener {

                                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()

                                val intent = Intent(this, Login::class.java)
                                startActivity(intent)
                                finish()

                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }
}