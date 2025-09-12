package ir.sharif.simplenote

//import com.example.simplenote.ui.screens.LoginScreen
//import com.example.simplenote.ui.screens.NotesScreen
//import com.example.simplenote.ui.screens.RegisterScreen
//import com.example.simplenote.viewmodels.AuthViewModel
//import dagger.hilt.android.AndroidEntryPoint


import ApiClient
import AuthViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.sharif.simplenote.ui.theme.SimpleNoteTheme

//@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SimpleNoteTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SimpleNoteApp()
                }
            }
        }
    }
}

@Composable
fun SimpleNoteApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Initialize API client
    LaunchedEffect(Unit) {
        ApiClient.initialize(context)
    }
    
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginDestination(navController)
        }
//        composable("register") {
//            RegisterDestination(navController)
//        }
//        composable("notes") {
//            NotesDestination(navController)
//        }
    }
}

@Composable
fun LoginDestination(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    var isLoading by remember { mutableStateOf(false) }
    
    LoginScreen {

    }//        onLoginClick = { username, password ->
//            isLoading = true
//            authViewModel.login(username, password) { success ->
//                isLoading = false
//                if (success) {
//                    navController.navigate("notes") {
//                        popUpTo("login") { inclusive = true }
//                    }
//                }
//            }
//        },
//        onRegisterClick = {
//            navController.navigate("register")
//        },
//        isLoading = isLoading
}

//@Composable
//fun RegisterDestination(navController: NavHostController) {
//    val authViewModel: AuthViewModel = hiltViewModel()
//    var isLoading by remember { mutableStateOf(false) }
//
//    RegisterScreen(
//        onRegisterClick = { username, password, email, firstName, lastName ->
//            isLoading = true
//            authViewModel.register(username, password, email, firstName, lastName) { success ->
//                isLoading = false
//                if (success) {
//                    navController.navigate("login") {
//                        popUpTo("register") { inclusive = false }
//                    }
//                }
//            }
//        },
//        onBackToLoginClick = {
//            navController.popBackStack()
//        },
//        isLoading = isLoading
//    )
//}
//
//@Composable
//fun NotesDestination(navController: NavHostController) {
//    NotesScreen(
//        onLogoutClick = {
//            // Clear tokens and navigate back to login
//            val authViewModel: AuthViewModel = hiltViewModel()
//            authViewModel.logout()
//            navController.navigate("login") {
//                popUpTo("notes") { inclusive = true }
//            }
//        }
//    )
//}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState?.isSuccess == true) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(username, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        if (viewModel.uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }

        viewModel.uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}