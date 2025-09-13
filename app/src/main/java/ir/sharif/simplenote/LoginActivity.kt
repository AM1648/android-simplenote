package ir.sharif.simplenote

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import ir.sharif.simplenote.auth.AuthUiState
import ir.sharif.simplenote.auth.AuthViewModel
import ir.sharif.simplenote.auth.AuthViewModelFactory
import ir.sharif.simplenote.notes.NotesHomeActivity

// imports unchanged…

class LoginActivity : ComponentActivity() {
    private val viewModel: AuthViewModel by viewModels { AuthViewModelFactory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val loginState by viewModel.loginState.collectAsState()
                val registerState by viewModel.registerState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                val context = LocalContext.current
                // Handle login results
                LaunchedEffect(loginState) {
                    when (val s = loginState) {
                        is AuthUiState.Error -> snackbarHostState.showSnackbar(s.message)
                        is AuthUiState.Success -> {
                            // You now have s.tokens (access/refresh) and s.me (UserInfo).
                            // TODO: navigate to Home
                            // startActivity(Intent(this@LoginActivity, HomeActivity::class.java)); finish()
                            val intent = Intent(context, NotesHomeActivity::class.java).apply {
                                putExtra("message", "Hello from MainActivity")
                            }
                            context.startActivity(intent)
                        }

                        else -> Unit
                    }
                }

                // Handle register results
                LaunchedEffect(registerState) {
                    when (val s = registerState) {
                        is AuthUiState.Error -> snackbarHostState.showSnackbar(s.message)
                        AuthUiState.Idle -> { /* noop */
                        }

                        AuthUiState.Loading -> { /* noop */
                        }

                        is AuthUiState.PasswordChanged -> { /* noop */
                        }

                        is AuthUiState.Success -> {
                            // In our ViewModel we don't set Success for register (we set Idle on success).
                            // If you later decide to emit Success with user data, handle it here.
                        }
                    }
                }

                // One loader for either flow
                val isBusy =
                    loginState is AuthUiState.Loading || registerState is AuthUiState.Loading

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets(0)
                ) { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        val context = LocalContext.current
                        LoginScreen(
                            onLogin = { form ->
                                viewModel.login(form.email, form.password)
                            },
                            onRegister = { form ->
                                val intent = Intent(context, RegisterActivity::class.java).apply {
                                    putExtra("message", "Hello from MainActivity")
                                }
                                context.startActivity(intent)
                            }
                        )

                        if (isBusy) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: (LoginForm) -> Unit,
    onRegister: (LoginForm) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .imePadding(),
                contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                // Title
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = "Let’s Login",
                            style = TextStyle(
                                fontSize = 32.sp,
                                lineHeight = 38.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF180E25)
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "And notes your idea",
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                color = Color(0xFF827D89)
                            )
                        )
                    }
                }

                // Email
                item {
                    LabeledField(
                        label = "Email Address",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Example: johndoe@gmail.com",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                }

                // Password
                item {
                    LabeledPasswordField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        showPassword = showPassword,
                        onToggleVisibility = { showPassword = !showPassword },
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            focus.clearFocus()
                            onLogin(LoginForm(email, password))
                        }
                    )
                }

                // Login button
                item {
                    Button(
                        onClick = {
                            focus.clearFocus()
                            onLogin(LoginForm(email, password))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C5CE7),
                            contentColor = Color.White
                        )
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Login")
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }

                // Divider
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = Color(0xFFE6E3EA))
                        Text(
                            "  Or  ",
                            color = Color(0xFF827D89),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Divider(modifier = Modifier.weight(1f), color = Color(0xFFE6E3EA))
                    }
                }

                // Register link
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Don’t have any account? ", color = Color(0xFF6D6A75))
                        Text(
                            text = "Register here",
                            color = Color(0xFF6C5CE7),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                focus.clearFocus()
                                onRegister(LoginForm(email, password))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = Color(0xFF180E25), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = { Text(placeholder, color = Color(0x33827D89)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            )
        )
    }
}

@Composable
private fun LabeledPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = Color(0xFF180E25), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = { Text("********") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val text = if (showPassword) "Hide" else "Show"
                Text(
                    text,
                    color = Color(0xFF6C5CE7),
                    modifier = Modifier.clickable { onToggleVisibility() }
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            )
        )
    }
}

data class LoginForm(
    val email: String,
    val password: String
)
