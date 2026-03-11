package com.goccorp.finance.auth

import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class AuthRepository(private val db: FirebaseFirestore) {

    private val salt = "financeApp2026SecureSalt"

    fun hashPassword(password: String): String {
        val saltedPassword = "${password}${salt}"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(saltedPassword.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun getUser(email: String, onResult: (Map<String, Any>?) -> Unit) {
        db.collection("Users").document(email).get()
            .addOnSuccessListener { document ->
                onResult(if (document.exists()) document.data else null)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun registerUser(
        email: String,
        userMap: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("Users").document(email).set(userMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Error desconocido") }
    }

    fun updatePassword(email: String, newHash: String, onComplete: (Boolean) -> Unit) {
        db.collection("Users").document(email)
            .update("Password", newHash)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
