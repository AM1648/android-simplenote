// ir/sharif/simplenote/auth/AuthViewModelFactory.kt
package ir.sharif.simplenote.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.sharif.simplenote.SimpleNoteApi

class AuthViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authApi = SimpleNoteApi.create(SimpleNoteApi.AuthApi::class.java)
        val repo = AuthRepository(authApi, context)
        return AuthViewModel(repo) as T
    }
}
