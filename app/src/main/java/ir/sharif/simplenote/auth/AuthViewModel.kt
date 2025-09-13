package ir.sharif.simplenote.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.sharif.simplenote.SimpleNoteApi.TokenObtainPair
import ir.sharif.simplenote.SimpleNoteApi.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val tokens: TokenObtainPair, val me: UserInfo) : AuthUiState
    data class Error(val message: String) : AuthUiState

    // New state for password change result
    data class PasswordChanged(val message: String) : AuthUiState
}

class AuthViewModel(
    private val repo: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val registerState: StateFlow<AuthUiState> = _registerState.asStateFlow()

    private val _userInfoLoadState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val userInfoLoadState: StateFlow<AuthUiState> = _userInfoLoadState.asStateFlow()

    private val _userInfoState = MutableStateFlow<UserInfo>(UserInfo(0, "", "", "", ""))
    val userInfoState: StateFlow<UserInfo> = _userInfoState.asStateFlow()

    // New change-password state
    private val _changePasswordState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val changePasswordState: StateFlow<AuthUiState> = _changePasswordState.asStateFlow()

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    public fun login(emailAsUsername: String, password: String) {
        if (emailAsUsername.isBlank() || password.isBlank()) {
            _loginState.value = AuthUiState.Error("Email/Username and password are required")
            return
        }
        _loginState.value = AuthUiState.Loading
        viewModelScope.launch {
            runCatching { repo.loginWithUsername(emailAsUsername, password) }
                .onSuccess { (tokens, me) -> _loginState.value = AuthUiState.Success(tokens, me) }
                .onFailure { _loginState.value = AuthUiState.Error(it.message ?: "Login failed") }
        }
    }

    public fun register(username: String, password: String, email: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _registerState.value = AuthUiState.Error("Username, email and password are required")
            return
        }
        _registerState.value = AuthUiState.Loading
        viewModelScope.launch {
            runCatching { repo.register(username, password, email) }
                .onSuccess { _registerState.value = AuthUiState.Idle /* or show a toast/snackbar */ }
                .onFailure { _registerState.value = AuthUiState.Error(it.message ?: "Register failed") }
        }
    }

    public fun getUserInfo() {
        _userInfoLoadState.value = AuthUiState.Loading
        viewModelScope.launch {
            runCatching { repo.getUserInfo() }
                .onSuccess {
                    _userInfoState.value = it
                    _userInfoLoadState.value = AuthUiState.Idle /* or show a toast/snackbar */
                }
                .onFailure { _userInfoLoadState.value = AuthUiState.Error(it.message ?: "Loading user info failed") }
        }
    }

    /**
     * Change the currently authenticated user's password.
     *
     * Expects the repository to call the API endpoint:
     *   POST /api/auth/change-password/ (returns Message { detail: String })
     *
     * Observers can watch `changePasswordState` to get Loading / PasswordChanged / Error updates.
     */
    public fun changePassword(oldPassword: String, newPassword: String) {
        if (oldPassword.isBlank() || newPassword.isBlank()) {
            _changePasswordState.value = AuthUiState.Error("Old and new password are required")
            return
        }

        _changePasswordState.value = AuthUiState.Loading
        viewModelScope.launch {
            runCatching { repo.changePassword(oldPassword, newPassword) }
                .onSuccess { message ->
                    // message is expected to be an object with `detail: String`
                    // if your repo returns plain String adjust accordingly
                    val detailText = when (message) {
                        is ir.sharif.simplenote.SimpleNoteApi.Message -> message.detail
                        is String -> message
                        else -> (message?.toString() ?: "Password changed")
                    }
                    _changePasswordState.value = AuthUiState.PasswordChanged(detailText)
                }
                .onFailure { _changePasswordState.value = AuthUiState.Error(it.message ?: "Change password failed") }
        }
    }

    public fun logout() {
        viewModelScope.launch {
            runCatching { repo.logout() }
                .onSuccess {
                    // Reset UI states if you want
                    _loginState.value = AuthUiState.Idle
                    _registerState.value = AuthUiState.Idle
                    _changePasswordState.value = AuthUiState.Idle

                    _loggedOut.value = true    // tell UI to navigate to Login
                }
                .onFailure {
                    _loginState.value = AuthUiState.Error(it.message ?: "Logout failed")
                }
        }
    }
}
