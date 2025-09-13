@file:Suppress("unused")

package ir.sharif.simplenote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * SimpleNote — Single-file Retrofit client & models (NO KSP)
 * Base URL: https://simple.darkube.app/
 *
 * Quick start:
 *   val auth  = SimpleNoteApi.create(SimpleNoteApi.AuthApi::class.java)
 *   val notes = SimpleNoteApi.create(SimpleNoteApi.NotesApi::class.java)
 *
 *   // Login for JWTs
 *   val pair = auth.obtainToken(SimpleNoteApi.TokenObtainPairRequest("alice","secret"))
 *   SimpleNoteApi.tokens.access = pair.access
 *   SimpleNoteApi.tokens.refresh = pair.refresh
 *
 *   // Fetch userinfo
 *   val me = auth.userInfo()
 *
 *   // Create a note
 *   val created = notes.createNote(SimpleNoteApi.NoteRequest("Title","Body"))
 */
object SimpleNoteApi {

    // ────────────────────────────────────────────────────────────────────────────
    // Config
    // ────────────────────────────────────────────────────────────────────────────
    private const val BASE_URL = "https://simple.darkube.app/"

    // ────────────────────────────────────────────────────────────────────────────
    // Token storage (access + refresh)
    // ────────────────────────────────────────────────────────────────────────────
    data class Tokens(var access: String? = null, var refresh: String? = null)
    val tokens = Tokens(refresh = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsImV4cCI6MTc1NzgzNTA4NywianRpIjoiYjhlMWRiYjMwYWNkNDNlMjliNGVmZjllOGRkNjNkZjAiLCJ1c2VyX2lkIjo2Nn0.tyaqM-BGrfnIyyE4ClUcZ-G16yyEEbkYfrAodnkeADo",
        access = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzU3NzUwNDg3LCJqdGkiOiIwYzk3NmZiMDUyMTM0M2M3YjY1ZWM0NjMzMjgxNmI0NSIsInVzZXJfaWQiOjY2fQ.blYoQpxm1qo3KVoWjX9IXOlWFLox6hrGCi2i0vtpqkw")

    // ────────────────────────────────────────────────────────────────────────────
    // OkHttp Interceptors / Authenticator
    // ────────────────────────────────────────────────────────────────────────────
    private class BearerInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val req = chain.request()
            val access = tokens.access
            val newReq = if (!access.isNullOrBlank()) {
                req.newBuilder().header("Authorization", "Bearer $access").build()
            } else req
            return chain.proceed(newReq)
        }
    }

    /**
     * Auto-refresh access token on 401 using /api/auth/token/refresh/.
     * Will attempt once per request chain to avoid infinite loops.
     */
    private class JwtRefreshAuthenticator(
        private val authApiFactory: () -> AuthApi
    ) : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // If no Authorization was sent, nothing to do
            if (response.request.header("Authorization").isNullOrBlank()) return null

            // Avoid loops: if we've already tried and still get 401, give up
            if (responseCount(response) >= 2) return null

            val refresh = tokens.refresh ?: return null

            return try {
                val newAccess = runBlocking {
                    authApiFactory()
                        .refreshToken(TokenRefreshRequest(refresh))
                        .access
                }
                // Save & retry with new access token
                tokens.access = newAccess
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccess")
                    .build()
            } catch (_: HttpException) {
                null
            } catch (_: Exception) {
                null
            }
        }

        private fun responseCount(response: Response): Int {
            var result = 1
            var prior = response.priorResponse
            while (prior != null) {
                result++
                prior = prior.priorResponse
            }
            return result
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Retrofit core (Moshi reflection — no KSP needed)
    // ────────────────────────────────────────────────────────────────────────────
    private val logging by lazy {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory()) // reflection-based adapters
            .build()
    }

    private fun okHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(BearerInterceptor())
            .addInterceptor(logging)
            .authenticator(JwtRefreshAuthenticator { create(AuthApi::class.java) })
            .build()

    private fun retrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    fun <T> create(service: Class<T>): T = retrofit().create(service)

    // ────────────────────────────────────────────────────────────────────────────
    // API interfaces (exactly per SimpleNote.yaml)
    // ────────────────────────────────────────────────────────────────────────────

    interface AuthApi {
        /** POST /api/auth/token/ */
        @POST("/api/auth/token/")
        suspend fun obtainToken(@Body body: TokenObtainPairRequest): TokenObtainPair

        /** POST /api/auth/token/refresh/ */
        @POST("/api/auth/token/refresh/")
        suspend fun refreshToken(@Body body: TokenRefreshRequest): TokenRefresh

        /** GET /api/auth/userinfo/ (JWT required) */
        @GET("/api/auth/userinfo/")
        suspend fun userInfo(): UserInfo

        /** POST /api/auth/register/ */
        @POST("/api/auth/register/")
        suspend fun register(@Body body: RegisterRequest): Register

        /** POST /api/auth/change-password/ (JWT required) */
        @POST("/api/auth/change-password/")
        suspend fun changePassword(@Body body: ChangePasswordRequest): Message
    }

    interface NotesApi {
        /** GET /api/notes/ (JWT required) */
        @GET("/api/notes/")
        suspend fun listNotes(
            @Query("page") page: Int? = null,
            @Query("page_size") pageSize: Int? = null
        ): PaginatedNoteList

        /** POST /api/notes/ (JWT required) */
        @POST("/api/notes/")
        suspend fun createNote(@Body body: NoteRequest): Note

        /** GET /api/notes/{id}/ (JWT required) */
        @GET("/api/notes/{id}/")
        suspend fun getNote(@Path("id") id: Int): Note

        /** PUT /api/notes/{id}/ (JWT required) */
        @PUT("/api/notes/{id}/")
        suspend fun updateNote(@Path("id") id: Int, @Body body: NoteRequest): Note

        /** PATCH /api/notes/{id}/ (JWT required) */
        @PATCH("/api/notes/{id}/")
        suspend fun patchNote(@Path("id") id: Int, @Body body: PatchedNoteRequest): Note

        /** DELETE /api/notes/{id}/ (JWT required) */
        @DELETE("/api/notes/{id}/")
        suspend fun deleteNote(@Path("id") id: Int): Unit

        /** POST /api/notes/bulk (JWT required) */
        @POST("/api/notes/bulk")
        suspend fun bulkCreate(@Body body: List<NoteRequest>): List<Note>

        /** GET /api/notes/filter (JWT required) */
        @GET("/api/notes/filter")
        suspend fun filterNotes(
            @Query("title") title: String? = null,
            @Query("description") description: String? = null,
            @Query("updated__gte") updatedGte: String? = null, // ISO datetime
            @Query("updated__lte") updatedLte: String? = null, // ISO datetime
            @Query("page") page: Int? = null,
            @Query("page_size") pageSize: Int? = null
        ): PaginatedNoteList
    }

    // (Optional) You can expose /api/schema/ as needed
    interface SchemaApi {
        @GET("/api/schema/")
        suspend fun schema(
            @Query("format") format: String? = null, // json|yaml
            @Query("lang") lang: String? = null
        ): Map<String, Any?>
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Models — EXACT field names/types from SimpleNote.yaml
    // Using reflection adapters (no @JsonClass / codegen)
    // ────────────────────────────────────────────────────────────────────────────

    // ---- Auth ----
    data class TokenObtainPairRequest(
        val username: String,
        val password: String
    )
    data class TokenObtainPair(
        val access: String,
        val refresh: String
    )
    data class TokenRefreshRequest(
        val refresh: String
    )
    data class TokenRefresh(
        val access: String
    )

    // ---- Register / UserInfo / Password change ----
    data class RegisterRequest(
        val username: String,
        val password: String,
        val email: String,
        val first_name: String? = null,
        val last_name: String? = null
    )
    data class Register(
        val username: String,
        val email: String,
        val first_name: String? = null,
        val last_name: String? = null
    )
    data class UserInfo(
        val id: Int,
        val username: String,
        val email: String,
        val first_name: String? = null,
        val last_name: String? = null
    )
    data class ChangePasswordRequest(
        val old_password: String,
        val new_password: String
    )
    data class Message(
        val detail: String
    )

    // ---- Notes domain ----
    data class NoteRequest(
        val title: String,
        val description: String
    )
    data class PatchedNoteRequest(
        val title: String? = null,
        val description: String? = null
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

    // ---- Error envelopes (handy for Retrofit error-body parsing) ----
    data class Error401Item(val code: String, val detail: String, val attr: String?)
    data class Error403Item(val code: String, val detail: String, val attr: String?)
    data class Error404Item(val code: String, val detail: String, val attr: String?)
    data class Error405Item(val code: String, val detail: String, val attr: String?)
    data class Error406Item(val code: String, val detail: String, val attr: String?)
    data class Error415Item(val code: String, val detail: String, val attr: String?)
    data class Error500Item(val code: String, val detail: String, val attr: String?)

    data class ErrorResponse401(val type: String, val errors: List<Error401Item>)
    data class ErrorResponse403(val type: String, val errors: List<Error403Item>)
    data class ErrorResponse404(val type: String, val errors: List<Error404Item>)
    data class ErrorResponse405(val type: String, val errors: List<Error405Item>)
    data class ErrorResponse406(val type: String, val errors: List<Error406Item>)
    data class ErrorResponse415(val type: String, val errors: List<Error415Item>)
    data class ErrorResponse500(val type: String, val errors: List<Error500Item>)

    // (Optional) ParseErrors from schema
    data class ParseErrorItem(val code: String, val detail: String, val attr: String?)
    data class ParseErrorResponse(val type: String, val errors: List<ParseErrorItem>)
}
