import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore("tokens.pref")

object ApiClient {
    private const val BASE_URL = "https://simple.darkube.app/" // Replace with your backend URL

    private lateinit var apiService: ApiService
    private lateinit var authTokenDataSource: AuthTokenDataSource

    fun initialize(context: Context) {
        authTokenDataSource = AuthTokenDataSource(context)

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            // Try to get the token from data store
            val token = runBlocking {
                authTokenDataSource.getAccessToken()?.first()
            }

            val newRequest: Request = if (token != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }

            chain.proceed(newRequest)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    fun getApiService(): ApiService = apiService
    fun getAuthTokenDataSource(): AuthTokenDataSource = authTokenDataSource
}

// Token data source for secure storage
class AuthTokenDataSource(context: Context) {
//    private val dataStore = context.createDataStore(name = "tokens.pref")

   private var dataStore = context.applicationContext.dataStore

    private val accessToken = stringPreferencesKey("access_token")
    private val refreshToken = stringPreferencesKey("refresh_token")

    suspend fun saveTokens(access: String, refresh: String) {
        dataStore.edit { preferences ->
            preferences[accessToken] = access
            preferences[refreshToken] = refresh
        }
    }

    suspend fun getAccessToken(): String? {
        return dataStore.data.map { it[accessToken] }.first()
    }

    suspend fun getRefreshToken(): String? {
        return dataStore.data.map { it[refreshToken] }.first()
    }

    suspend fun clearTokens() {
        dataStore.edit { preferences ->
            preferences.remove(accessToken)
            preferences.remove(refreshToken)
        }
    }
}