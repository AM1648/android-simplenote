package ir.sharif.simplenote

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import ir.sharif.simplenote.auth.AuthUiState
import ir.sharif.simplenote.auth.AuthViewModel
import ir.sharif.simplenote.auth.AuthViewModelFactory
import kotlin.math.min

/**
 * SettingsActivity that uses AuthViewModel (via AuthViewModelFactory) to perform logout.
 *
 * Requires:
 *  - AuthViewModel with logout() and flows: loginState: StateFlow<AuthUiState>, loggedOut: StateFlow<Boolean>
 *  - AuthViewModelFactory() available for the viewModels delegate
 *  - LoginActivity exists (adjust class if named differently)
 */
class SettingsActivity : ComponentActivity() {

    // Use the provided factory (the user asked for this exact delegate)
    private val viewModel: AuthViewModel by viewModels { AuthViewModelFactory(applicationContext) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                var showLogout by remember { mutableStateOf(false) }

                // Observe loggedOut signal from ViewModel; navigate when it becomes true
                val loggedOut by viewModel.loggedOut.collectAsState()

                val context = LocalContext.current
                // When loggedOut is true, navigate to LoginActivity and finish this activity
                LaunchedEffect(loggedOut) {
                    if (loggedOut) {
                        // Clear activity/task so user cannot return with Back
                        val intent = Intent(context, LoginActivity::class.java).apply {
                            putExtra("message", "Hello from MainActivity")
                        }
                        context.startActivity(intent)
                        startActivity(intent)
                        Toast.makeText(this@SettingsActivity, "Logged out", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Settings") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        onChangePassword = {
                            val intent = Intent(context, ChangePasswordActivity::class.java).apply {
                                putExtra("message", "Hello from MainActivity")
                            }
                            context.startActivity(intent)
                            finish()
                        },
                        onLogoutClick = { showLogout = true },
                        viewModel = viewModel
                    )
                }

                if (showLogout) {
                    LogoutDialog(
                        viewModel = viewModel,
                        onDismiss = { showLogout = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    onChangePassword: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: AuthViewModel
) {
    val userInfoLoadState by viewModel.userInfoLoadState.collectAsState(initial = AuthUiState.Idle)
    val userInfoState by viewModel.userInfoState.collectAsState()

    LaunchedEffect (userInfoState) {
        viewModel.getUserInfo()
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // --- Profile card (hard-coded for now) ---
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simple initial-based avatar (no network dependency)
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    userInfoState.username.substring(0, min(userInfoState.username.length, 2)).uppercase(), // initials
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    userInfoState.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    userInfoState.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        if (userInfoLoadState is AuthUiState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("APP SETTINGS", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        // --- Change Password item ---
        SettingRow(
            leading = { Icon(Icons.Default.Lock, contentDescription = null) },
            title = "Change Password",
            trailing = { Icon(Icons.Default.ArrowForward, contentDescription = null) },
            onClick = onChangePassword
        )

        Spacer(Modifier.height(8.dp))

        // --- Log Out (red) ---
        SettingRow(
            leading = {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = "Log Out",
            titleColor = MaterialTheme.colorScheme.error,
            onClick = onLogoutClick
        )

        Spacer(modifier = Modifier.weight(1f))
        Text(
            "Taha Notes v1.1",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SettingRow(
    leading: @Composable () -> Unit,
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading()
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                modifier = Modifier.weight(1f)
            )
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

/**
 * LogoutDialog: observes the ViewModel for loading/error and triggers viewModel.logout()
 *
 * - Shows a circular progress while AuthUiState.Loading is active.
 * - Displays error text if AuthUiState.Error appears.
 * - Calls viewModel.logout() when the user confirms.
 */
@Composable
private fun LogoutDialog(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val authState by viewModel.loginState.collectAsState(initial = AuthUiState.Idle)
    val isProcessing = authState is AuthUiState.Loading
    val errorMessage = (authState as? AuthUiState.Error)?.message

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Text(
                "Log Out",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Are you sure you want to log out from the application?")
                Spacer(modifier = Modifier.height(12.dp))

                if (isProcessing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isProcessing) return@TextButton
                    viewModel.logout()
                },
                enabled = !isProcessing
            ) {
                Text("Yes")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { if (!isProcessing) onDismiss() },
                enabled = !isProcessing
            ) {
                Text("Cancel")
            }
        }
    )
}
