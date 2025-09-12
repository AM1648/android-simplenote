import ir.sharif.simplenote.ChangePasswordRequest
import ir.sharif.simplenote.LoginRequest
import ir.sharif.simplenote.MessageResponse
import ir.sharif.simplenote.Note
import ir.sharif.simplenote.NoteRequest
import ir.sharif.simplenote.PaginatedNoteList
import ir.sharif.simplenote.RegisterRequest
import ir.sharif.simplenote.RegisterResponse
import ir.sharif.simplenote.TokenResponse
import ir.sharif.simplenote.UserInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    // Auth endpoints
    @POST("api/auth/register/")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<RegisterResponse>

    @POST("api/auth/token/")
    suspend fun login(@Body loginRequest: LoginRequest): Response<TokenResponse>

    @POST("api/auth/token/refresh/")
    suspend fun refreshToken(@Body refreshRequest: Map<String, String>): Response<TokenResponse>

    @GET("api/auth/userinfo/")
    suspend fun getUserInfo(@Header("Authorization") token: String): Response<UserInfo>

    @POST("api/auth/change-password/")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body changePasswordRequest: ChangePasswordRequest
    ): Response<MessageResponse>

    // Notes endpoints
    @GET("api/notes/")
    suspend fun getNotes(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null
    ): Response<PaginatedNoteList>

    @POST("api/notes/")
    suspend fun createNote(
        @Header("Authorization") token: String,
        @Body noteRequest: NoteRequest
    ): Response<Note>

    @GET("api/notes/{id}/")
    suspend fun getNote(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Note>

    @PUT("api/notes/{id}/")
    suspend fun updateNote(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body noteRequest: NoteRequest
    ): Response<Note>

    @POST("api/notes/bulk")
    suspend fun createNotesBulk(
        @Header("Authorization") token: String,
        @Body notes: List<NoteRequest>
    ): Response<List<Note>>

    @GET("api/notes/filter")
    suspend fun filterNotes(
        @Header("Authorization") token: String,
        @Query("title") title: String? = null,
        @Query("description") description: String? = null,
        @Query("updated__gte") updatedAfter: String? = null,
        @Query("updated__lte") updatedBefore: String? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null
    ): Response<PaginatedNoteList>
}