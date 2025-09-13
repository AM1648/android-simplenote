package ir.sharif.simplenote

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import ir.sharif.simplenote.auth.AuthUiState
import ir.sharif.simplenote.auth.AuthViewModel
import ir.sharif.simplenote.auth.AuthViewModelFactory

class RegisterActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels { AuthViewModelFactory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val regState by viewModel.registerState.collectAsState()
                val context = LocalContext.current

                Box(Modifier.fillMaxSize()) {
                    RegisterScreen(
                        onBack = { finish() },
                        onLoginClick = {
                            val intent = Intent(context, LoginActivity::class.java).apply {
                                putExtra("message", "Hello from MainActivity")
                            }
                            context.startActivity(intent)
                            finish()
                        },
                        onSubmit = { form ->
                            // quick client-side check
                            if (form.password != form.retype) {
                                // show something in your UI instead if you prefer
                                // e.g., a Snackbar; keeping it minimal here:
                                return@RegisterScreen
                            }
                            // Uses the provided ViewModel as requested
                            viewModel.register(
                                username = form.username,
                                email = form.email,
                                password = form.password
                            )
                        }
                    )

                    // super lightweight feedback (optional)
                    when (val s = regState) {
                        AuthUiState.Loading -> {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }

                        is AuthUiState.Error -> {
                            Text(
                                text = s.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onSubmit: (RegisterForm) -> Unit
) {
    // --- Form state ---
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var retype by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showRetype by rememberSaveable { mutableStateOf(false) }

    val focus = LocalFocusManager.current

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)  // page side padding
                .imePadding(),                 // moves content above keyboard
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBack() }
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF6C5CE7)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Back to Login",
                        color = Color(0xFF6C5CE7),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Title + subtitle
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = "Register",
                        style = TextStyle(
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF180E25)
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "And start taking notes",
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            color = Color(0xFF827D89)
                        )
                    )
                }
            }

            // Fields
            item {
                LabeledField(
                    label = "First Name",
                    value = firstName,
                    onValueChange = { firstName = it },
                    placeholder = "Example: Taha",
                    imeAction = ImeAction.Next,
                    onImeAction = { }
                )
            }
            item {
                LabeledField(
                    label = "Last Name",
                    value = lastName,
                    onValueChange = { lastName = it },
                    placeholder = "Example: Hamifar",
                    imeAction = ImeAction.Next
                )
            }
            item {
                LabeledField(
                    label = "Username",
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "Example: @HamifarTaha",
                    imeAction = ImeAction.Next
                )
            }
            item {
                LabeledField(
                    label = "Email Address",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Example: hamifar.taha@gmail.com",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            }
            item {
                LabeledPasswordField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it },
                    showPassword = showPassword,
                    onToggleVisibility = { showPassword = !showPassword },
                    imeAction = ImeAction.Next
                )
            }
            item {
                LabeledPasswordField(
                    label = "Retype Password",
                    value = retype,
                    onValueChange = { retype = it },
                    showPassword = showRetype,
                    onToggleVisibility = { showRetype = !showRetype },
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focus.clearFocus()
                        onSubmit(
                            RegisterForm(firstName, lastName, username, email, password, retype)
                        )
                    }
                )
            }

            // Register button
            item {
                Button(
                    onClick = {
                        focus.clearFocus()
                        onSubmit(
                            RegisterForm(
                                firstName,
                                lastName,
                                username,
                                email,
                                password,
                                retype
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Register")
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                        )
                    }
                }
            }

            // Login link
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = Color(0xFF6D6A75)
                    )
                    Text(
                        text = "Login here",
                        color = Color(0xFF6C5CE7),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onLoginClick() }
                    )
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
    val focus = LocalFocusManager.current
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

data class RegisterForm(
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val password: String,
    val retype: String
)
