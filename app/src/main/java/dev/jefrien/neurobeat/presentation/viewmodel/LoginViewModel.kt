package dev.jefrien.neurobeat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jefrien.neurobeat.data.local.datastore.SettingsDataStore
import dev.jefrien.neurobeat.data.remote.api.SubsonicAuthInterceptor
import dev.jefrien.neurobeat.domain.repository.SubsonicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val repository: SubsonicRepository
) : ViewModel() {

    sealed class LoginState {
        data object Idle : LoginState()
        data object Loading : LoginState()
        data class Error(val message: String) : LoginState()
        data object Success : LoginState()
    }

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun checkLoginStatus() {
        viewModelScope.launch {
            val isLoggedIn = settingsDataStore.isLoggedIn.first()
            if (isLoggedIn) {
                // Verify credentials still work with a ping
                val result = repository.ping()
                result.fold(
                    onSuccess = { isOk ->
                        if (isOk) {
                            _state.value = LoginState.Success
                        } else {
                            settingsDataStore.clearCredentials()
                            _state.value = LoginState.Idle
                        }
                    },
                    onFailure = {
                        // Offline or server unreachable, but we have credentials
                        // Still allow access (offline mode)
                        _state.value = LoginState.Success
                    }
                )
            }
        }
    }

    fun login(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading

            val normalizedUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
            val salt = SubsonicAuthInterceptor.generateSalt()
            val token = SubsonicAuthInterceptor.generateToken(password, salt)

            // Save credentials first so the interceptor can use them
            settingsDataStore.saveCredentials(normalizedUrl, username, token, salt)

            // Test connection
            val result = repository.ping()
            result.fold(
                onSuccess = { isOk ->
                    if (isOk) {
                        _state.value = LoginState.Success
                    } else {
                        settingsDataStore.clearCredentials()
                        _state.value = LoginState.Error("Invalid credentials or server URL")
                    }
                },
                onFailure = { error ->
                    settingsDataStore.clearCredentials()
                    _state.value = LoginState.Error(error.message ?: "Connection failed")
                }
            )
        }
    }
}
