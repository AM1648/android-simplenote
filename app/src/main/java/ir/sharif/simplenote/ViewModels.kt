import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.sharif.simplenote.Note
import ir.sharif.simplenote.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository(
        ApiClient.getApiService(),
        ApiClient.getAuthTokenDataSource()
    )
    
    var uiState by mutableStateOf(AuthUiState())
        private set
    
    private val _loginState = MutableStateFlow<Result<Unit>?>(null)
    val loginState = _loginState.asStateFlow()
    
    fun register(user: RegisterRequest) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            authRepository.register(user).onEach { result ->
                uiState = uiState.copy(isLoading = false)
                when {
                    result.isSuccess -> {
                        uiState = uiState.copy(
                            registrationSuccess = true,
                            errorMessage = null
                        )
                    }
                    result.isFailure -> {
                        uiState = uiState.copy(
                            errorMessage = result.exceptionOrNull()?.message,
                            registrationSuccess = false
                        )
                    }
                }
            }.launchIn(viewModelScope)
        }
    }
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            authRepository.login(username, password).onEach { result ->
                uiState = uiState.copy(isLoading = false)
                when {
                    result.isSuccess -> {
                        _loginState.value = Result.success(Unit)
                        uiState = uiState.copy(errorMessage = null)
                    }
                    result.isFailure -> {
                        _loginState.value = Result.failure(result.exceptionOrNull()!!)
                        uiState = uiState.copy(
                            errorMessage = result.exceptionOrNull()?.message
                        )
                    }
                }
            }.launchIn(viewModelScope)
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loginState.value = null
        }
    }
    
    fun clearErrorMessage() {
        uiState = uiState.copy(errorMessage = null)
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val registrationSuccess: Boolean = false,
    val errorMessage: String? = null
)

class NotesViewModel : ViewModel() {
    private val notesRepository = NotesRepository(
        ApiClient.getApiService(),
        ApiClient.getAuthTokenDataSource()
    )
    
    var uiState by mutableStateOf(NotesUiState())
        private set
    
    fun getNotes(page: Int? = null, pageSize: Int? = null) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            notesRepository.getNotes(page, pageSize).onEach { result ->
                uiState = uiState.copy(isLoading = false)
                when {
                    result.isSuccess -> {
                        uiState = uiState.copy(
                            notes = result.getOrNull()?.results ?: emptyList(),
                            errorMessage = null
                        )
                    }
                    result.isFailure -> {
                        uiState = uiState.copy(
                            errorMessage = result.exceptionOrNull()?.message
                        )
                    }
                }
            }.launchIn(viewModelScope)
        }
    }
    
    fun createNote(title: String, description: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            notesRepository.createNote(title, description).onEach { result ->
                uiState = uiState.copy(isLoading = false)
                when {
                    result.isSuccess -> {
                        // Refresh the notes list
                        getNotes()
                        uiState = uiState.copy(errorMessage = null)
                    }
                    result.isFailure -> {
                        uiState = uiState.copy(
                            errorMessage = result.exceptionOrNull()?.message
                        )
                    }
                }
            }.launchIn(viewModelScope)
        }
    }
    
    fun clearErrorMessage() {
        uiState = uiState.copy(errorMessage = null)
    }
}

data class NotesUiState(
    val isLoading: Boolean = false,
    val notes: List<Note> = emptyList(),
    val errorMessage: String? = null
)