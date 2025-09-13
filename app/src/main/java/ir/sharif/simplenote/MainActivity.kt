package ir.sharif.simplenote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.sharif.simplenote.notes.NotesHomeActivity
import ir.sharif.simplenote.ui.theme.SimpleNoteTheme

class MainActivity : ComponentActivity() {

    // Keep these identical to AuthRepository constants
    companion object {
        private const val PREFS_NAME = "simplenote_prefs"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read the persisted refresh token (same storage used by AuthRepository)
        val savedRefresh = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REFRESH_TOKEN, null)

        // If there is a refresh token, load it into SimpleNoteApi and go to NotesHomeActivity
        if (!savedRefresh.isNullOrBlank()) {
            SimpleNoteApi.tokens.refresh = savedRefresh

            // Navigate to notes home and finish this activity so user won't come back to welcome
            startActivity(Intent(this, NotesHomeActivity::class.java))
            finish()
            return
        }

        // Otherwise keep the normal welcome UI
        enableEdgeToEdge()
        setContent {
            SimpleNoteTheme {
                Surface(
                    color = Color(0xFF504EC3),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.welcome),
                                contentDescription = null,
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = "Jot Down anything you want to achieve, today or in the future",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val context = LocalContext.current
                        Button(
                            onClick = {
                                val intent = Intent(context, LoginActivity::class.java).apply {
                                    putExtra("message", "Hello from MainActivity")
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF504EC3)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Let’s Get Started")
                        }
                    }
                }
            }
        }
    }
}
