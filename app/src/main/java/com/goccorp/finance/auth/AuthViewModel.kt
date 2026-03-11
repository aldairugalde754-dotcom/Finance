package com.goccorp.finance.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

sealed class AuthState {
    data object Loading : AuthState()
    data class Success(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
    data object Idle : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun login(email: String, passwordInput: String) {
        _authState.value = AuthState.Loading

        val hashedInput = repository.hashPassword(passwordInput)

        repository.getUser(email) { data ->
            if (data != null) {
                val storedHash = data["Password"] as? String
                if (hashedInput == storedHash) {
                    _authState.value = AuthState.Success(email)
                } else {
                    _authState.value = AuthState.Error("Contraseña incorrecta")
                }
            } else {
                _authState.value = AuthState.Error("El usuario no existe")
            }
        }
    }

    fun register(name: String, email: String, phone: String, passwordInput: String) {
        _authState.value = AuthState.Loading

        repository.getUser(email) { data ->
            if (data != null) {
                _authState.value = AuthState.Error("El usuario ya existe")
            } else {
                val encryptedPass = repository.hashPassword(passwordInput)
                val userMap = hashMapOf(
                    "Name" to name,
                    "Phone" to phone,
                    "Password" to encryptedPass,
                    "Balance" to 0.0
                )

                repository.registerUser(
                    email,
                    userMap,
                    onSuccess = { _authState.value = AuthState.Success(email) },
                    onFailure = { _authState.value = AuthState.Error(it) }
                )
            }
        }
    }

    fun recoverPassword(email: String, phoneInput: String, onStep1Success: () -> Unit) {
        repository.getUser(email) { data ->
            if (data != null && phoneInput == data["Phone"] as? String) {
                onStep1Success()
            } else {
                _authState.value = AuthState.Error("Datos de recuperación incorrectos")
            }
        }
    }

    fun updatePassword(email: String, newPass: String) {
        val hashedNewPass = repository.hashPassword(newPass)
        repository.updatePassword(email, hashedNewPass) { success ->
            if (success) {
                _authState.value = AuthState.Error("Contraseña actualizada con éxito")
            } else {
                _authState.value = AuthState.Error("Error al actualizar la contraseña")
            }
        }
    }
}
