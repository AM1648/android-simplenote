package ir.sharif.simplenote

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import ir.sharif.simplenote.auth.AuthRepository
import ir.sharif.simplenote.auth.AuthViewModel
import ir.sharif.simplenote.auth.AuthUiState
import ir.sharif.simplenote.auth.AuthViewModelFactory

class ChangePasswordActivity : ComponentActivity() {
    private val viewModel: AuthViewModel by viewModels { AuthViewModelFactory(applicationContext) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                var oldPass by rememberSaveable { mutableStateOf("") }
                var newPass by rememberSaveable { mutableStateOf("") }
                var confirmPass by rememberSaveable { mutableStateOf("") }
                val focus = LocalFocusManager.current

                // Collect the change-password state
                val changeState by viewModel.changePasswordState.collectAsState()

                // react to state changes (show snackbars / finish on success)
                LaunchedEffect(changeState) {
                    when (changeState) {
                        is AuthUiState.Loading -> {
                            // optionally show an indefinite snackbar or progress indicator elsewhere
                        }

                        is AuthUiState.PasswordChanged -> {
                            val msg = (changeState as AuthUiState.PasswordChanged).message
                            // show snackbar then finish
                            snackbarHostState.showSnackbar(msg)
                            Toast.makeText(this@ChangePasswordActivity, msg, Toast.LENGTH_SHORT)
                                .show()
                            finish()
                        }

                        is AuthUiState.Error -> {
                            val msg = (changeState as AuthUiState.Error).message
                            snackbarHostState.showSnackbar(msg)
                        }

                        else -> {
                            // Idle / Success (not used here)
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Change Password") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { inner ->
                    Column(
                        Modifier
                            .padding(inner)
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Please input your current password first",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = oldPass,
                            onValueChange = { oldPass = it },
                            label = { Text("Current Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Now, create your new password",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Password should contain a-z, A-Z, 0-9",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it },
                            label = { Text("Retype New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { focus.clearFocus() }
                            )
                        )

                        Spacer(Modifier.weight(1f))

                        // disable submit while loading
                        val isLoading = changeState is AuthUiState.Loading

                        Button(
                            onClick = {
                                focus.clearFocus()
                                when {
                                    oldPass.isBlank() || newPass.isBlank() || confirmPass.isBlank() ->
                                        scope.launch { snackbarHostState.showSnackbar("Please fill all fields.") }

                                    newPass != confirmPass ->
                                        scope.launch { snackbarHostState.showSnackbar("New passwords do not match.") }

                                    else -> {
                                        // call ViewModel -> repository -> API
                                        viewModel.changePassword(oldPass, newPass)
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 8.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isLoading) "Submitting..." else "Submit New Password")
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}
