package ir.sharif.simplenote.auth

import android.content.Context
import android.content.SharedPreferences
import ir.sharif.simplenote.SimpleNoteApi
import ir.sharif.simplenote.SimpleNoteApi.AuthApi
import ir.sharif.simplenote.SimpleNoteApi.Register
import ir.sharif.simplenote.SimpleNoteApi.RegisterRequest
import ir.sharif.simplenote.SimpleNoteApi.TokenObtainPair
import ir.sharif.simplenote.SimpleNoteApi.TokenObtainPairRequest
import ir.sharif.simplenote.SimpleNoteApi.UserInfo
import ir.sharif.simplenote.SimpleNoteApi.ChangePasswordRequest
import ir.sharif.simplenote.SimpleNoteApi.Message

private const val PREFS_NAME = "simplenote_prefs"
private const val KEY_REFRESH_TOKEN = "refresh_token"

class AuthRepository(
    private val api: AuthApi,
    context: Context
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Logs in with username+password (we pass the email field as username).
     * Saves tokens into SimpleNoteApi.tokens, persists refresh token in SharedPreferences,
     * and returns both tokens + current user.
     */
    suspend fun loginWithUsername(
        usernameOrEmail: String,
        password: String
    ): Pair<TokenObtainPair, UserInfo> {
        val pair: TokenObtainPair =
            api.obtainToken(TokenObtainPairRequest(username = usernameOrEmail, password = password))

        // Save tokens globally so subsequent calls include Authorization automatically
        SimpleNoteApi.tokens.access = pair.access
        SimpleNoteApi.tokens.refresh = pair.refresh

        // Persist refresh token to SharedPreferences
        pair.refresh?.let { refresh ->
            prefs.edit().putString(KEY_REFRESH_TOKEN, refresh).apply()
        }

        // Fetch current user with fresh access token
        val me: UserInfo = api.userInfo()
        return pair to me
    }

    suspend fun getUserInfo() = api.userInfo()

    /**
     * Registers a user; you can pass email as both username and email for a simple flow.
     * Does NOT auto-login — call loginWithUsername afterwards if desired.
     */
    suspend fun register(username: String, password: String, email: String): Register {
        val body = RegisterRequest(
            username = username,
            password = password,
            email = email,
            first_name = null,
            last_name = null
        )
        return api.register(body)
    }

    /**
     * Change the currently authenticated user's password.
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): Message {
        val req = ChangePasswordRequest(
            old_password = oldPassword,
            new_password = newPassword
        )
        return api.changePassword(req)
    }

    /**
     * Logout: clears in-memory tokens and removes stored refresh token.
     * If your backend later adds a revoke/blacklist endpoint, call it here first.
     */
    suspend fun logout() {
        // Clear in-memory tokens
        SimpleNoteApi.tokens.access = null
        SimpleNoteApi.tokens.refresh = null

        // Clear persisted refresh token
        prefs.edit().remove(KEY_REFRESH_TOKEN).apply()
    }

    /**
     * Optional helper — read persisted refresh token if needed (e.g., on app start).
     */
    fun getPersistedRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }
}
