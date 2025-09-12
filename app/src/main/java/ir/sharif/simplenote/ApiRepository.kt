import ir.sharif.simplenote.LoginRequest
import ir.sharif.simplenote.Note
import ir.sharif.simplenote.NoteRequest
import ir.sharif.simplenote.PaginatedNoteList
import ir.sharif.simplenote.RegisterRequest
import ir.sharif.simplenote.RegisterResponse
import ir.sharif.simplenote.TokenResponse
import ir.sharif.simplenote.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response

class AuthRepository(private val apiService: ApiService, private val tokenDataSource: AuthTokenDataSource) {
    suspend fun register(user: RegisterRequest): Flow<Result<RegisterResponse>> = flow {
        try {
            val response = apiService.register(user)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(Result.success(it))
                } ?: emit(Result.failure(Exception("Empty response body")))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                emit(Result.failure(Exception("Registration failed: $errorBody")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun login(username: String, password: String): Flow<Result<TokenResponse>> = flow {
        try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                response.body()?.let {
                    // Save tokens securely
                    tokenDataSource.saveTokens(it.access, it.refresh)
                    emit(Result.success(it))
                } ?: emit(Result.failure(Exception("Empty response body")))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                emit(Result.failure(Exception("Login failed: $errorBody")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun getUserInfo(): Flow<Result<UserInfo>> = flow {
        try {
            val token = tokenDataSource.getAccessToken()
            if (token != null) {
                val response = apiService.getUserInfo("Bearer $token")
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(Result.success(it))
                    } ?: emit(Result.failure(Exception("Empty response body")))
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    emit(Result.failure(Exception("Failed to get user info: $errorBody")))
                }
            } else {
                emit(Result.failure(Exception("No authentication token found")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun logout() {
        tokenDataSource.clearTokens()
    }
}

class NotesRepository(private val apiService: ApiService, private val tokenDataSource: AuthTokenDataSource) {
    suspend fun getNotes(page: Int? = null, pageSize: Int? = null): Flow<Result<PaginatedNoteList>> = flow {
        try {
            val token = tokenDataSource.getAccessToken()
            if (token != null) {
                val response = apiService.getNotes("Bearer $token", page, pageSize)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(Result.success(it))
                    } ?: emit(Result.failure(Exception("Empty response body")))
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    emit(Result.failure(Exception("Failed to get notes: $errorBody")))
                }
            } else {
                emit(Result.failure(Exception("No authentication token found")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun createNote(title: String, description: String): Flow<Result<Note>> = flow {
        try {
            val token = tokenDataSource.getAccessToken()
            if (token != null) {
                val response = apiService.createNote("Bearer $token",
                    NoteRequest(title, description)
                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(Result.success(it))
                    } ?: emit(Result.failure(Exception("Empty response body")))
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    emit(Result.failure(Exception("Failed to create note: $errorBody")))
                }
            } else {
                emit(Result.failure(Exception("No authentication token found")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // Add similar methods for other note operations
}