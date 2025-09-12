package ir.sharif.simplenote

// Auth models
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val first_name: String? = null,
    val last_name: String? = null
)

data class RegisterResponse(
    val username: String,
    val email: String,
    val first_name: String?,
    val last_name: String?
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    val access: String,
    val refresh: String
)

data class UserInfo(
    val id: Int,
    val username: String,
    val email: String,
    val first_name: String?,
    val last_name: String?
)

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)

data class MessageResponse(
    val detail: String
)

// Note models
data class NoteRequest(
    val title: String,
    val description: String
)

data class Note(
    val id: Int,
    val title: String,
    val description: String,
    val created_at: String,
    val updated_at: String,
    val creator_name: String,
    val creator_username: String
)

data class PaginatedNoteList(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<Note>
)

// Error models
data class ErrorResponse(
    val type: String,
    val errors: List<ApiError>
)

data class ApiError(
    val code: String,
    val detail: String,
    val attr: String?
)