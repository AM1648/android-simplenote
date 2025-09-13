package ir.sharif.simplenote.notes

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import ir.sharif.simplenote.SimpleNoteApi
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class NoteActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels { NotesViewModelFactory() }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val noteId = intent.getIntExtra("note_id", -1)
        require(noteId > 0) { "NoteActivity needs note_id extra" }

        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                val noteState by viewModel.noteState.collectAsState()
                val writeState by viewModel.writeState.collectAsState()
                val deleteState by viewModel.deleteState.collectAsState()

                // local editable fields
                var title by remember { mutableStateOf("") }
                var body by remember { mutableStateOf("") }
                var updatedAt by remember { mutableStateOf<String?>(null) }

                // load once
                LaunchedEffect(Unit) { viewModel.getNote(noteId) }

                // observe note load / write responses to refresh "updatedAt"
                LaunchedEffect(noteState) {
                    if (noteState is NoteUiState.Success) {
                        val n = (noteState as NoteUiState.Success).note
                        title = n.title
                        body = n.description
                        updatedAt = n.updated_at
                    }
                }
                LaunchedEffect(writeState) {
                    if (writeState is WriteUiState.Success) {
                        val n = (writeState as WriteUiState.Success).note
                        title = n.title
                        body = n.description
                        updatedAt = n.updated_at
                    }
                }

                // debounce auto-save on text changes
                var saveJobKey by remember { mutableStateOf(0) }
                fun scheduleSave() {
                    saveJobKey++
                }
                LaunchedEffect(saveJobKey, title, body) {
                    // Only when we already loaded something
                    if (noteState is NoteUiState.Success || writeState is WriteUiState.Success) {
                        delay(600) // debounce
                        viewModel.patchNote(noteId, title = title, description = body)
                    }
                }

                // bottom sheet for delete confirm
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var showSheet by remember { mutableStateOf(false) }

                // handle delete success → finish and return to home
                LaunchedEffect(deleteState) {
                    if (deleteState is DeleteUiState.Success) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {},
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                // right-top delete (as design)
                                IconButton(onClick = { showSheet = true }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        // bottom bar: last edited + small trash button on the right (design hint)
                        val purple = Color(0xFF6C5CE7)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last edited at " + (updatedAt?.let { toShortTime(it) }
                                    ?: "—"),
                                style = TextStyle(fontSize = 12.sp, color = Color(0x99000000))
                            )
                            Spacer(Modifier.weight(1f))
                            FilledTonalButton(
                                onClick = { finish() },
                                modifier = Modifier
                                    .size(height = 40.dp, width = 56.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = purple,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Done, contentDescription = "Delete")
                            }
                        }
                    }
                ) { inner ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(inner)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            value = title,
                            onValueChange = {
                                title = it
                                scheduleSave()
                            },
                            textStyle = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            placeholder = { Text("New Product Ideas") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = body,
                            onValueChange = {
                                body = it
                                scheduleSave()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            placeholder = { Text("Write your note...") },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            ),
                            minLines = 10,
                            maxLines = Int.MAX_VALUE
                        )
                    }
                }

                if (noteState is NoteUiState.Loading) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                }

                if (showSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showSheet = false },
                        sheetState = sheetState,
                        containerColor = Color.White,
                        dragHandle = {}
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .align(Alignment.End)
                            ) {
                                // close pill
                                TextButton(
                                    onClick = { showSheet = false },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text("✕", fontSize = 16.sp, color = Color(0xFF555555))
                                }
                            }
                            Text(
                                "Want to Delete this Note?",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                textAlign = TextAlign.Start
                            )

                            Button(
                                onClick = {
                                    viewModel.deleteNote(noteId)
                                    showSheet = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF4D61),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Delete Note", fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toShortTime(iso: String): String {
        return ZonedDateTime.parse(iso)
            .withZoneSameInstant(ZoneId.of("Asia/Tehran"))
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}
