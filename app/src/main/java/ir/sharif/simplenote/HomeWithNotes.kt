//package ir.sharif.simplenote
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.LocalIndication
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.foundation.lazy.grid.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.outlined.Home
//import androidx.compose.material.icons.outlined.Search
//import androidx.compose.material.icons.outlined.Settings
//import androidx.compose.material.icons.rounded.Add
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//class NotesHomeActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            MaterialTheme {
//                // Sample data
//                val sampleNotes = remember {
//                    listOf(
//                        Note(
//                            id = "1",
//                            title = "💡 New Product Idea Design",
//                            body = "Create a mobile app UI Kit that provide a basic notes functionality but with some improvement.\n\nThere will be a choice to select what kind of notes that user needed, so the experience while taking notes can be unique based on the needs.",
//                            color = NoteColor.YellowA
//                        ),
//                        Note(
//                            id = "2",
//                            title = "💡 New Product Idea Design",
//                            body = "Create a mobile app UI Kit that provide a basic notes functionality but with some improvement.\n\nThere will be a choice to select what kind of notes that user needed, so the experience while taking notes can be unique based on the needs.",
//                            color = NoteColor.YellowB
//                        ),
//                    )
//                }
//
//                NotesHomeScreen(
//                    notes = sampleNotes,
//                    onAddNote = { /* TODO */ },
//                    onOpenNote = { /* TODO: open details by id */ },
//                    onOpenSettings = { /* TODO */ }
//                )
//            }
//        }
//    }
//}
//
///* ---------- UI ---------- */
//
//@Composable
//fun NotesHomeScreen(
//    notes: List<Note>,
//    onAddNote: () -> Unit,
//    onOpenNote: (Note) -> Unit,
//    onOpenSettings: () -> Unit
//) {
//    var selectedTab by remember { mutableStateOf(0) } // 0 Home, 1 Settings
//    var query by rememberSaveable { mutableStateOf("") }
//
//    val filtered = remember(notes, query) {
//        val q = query.trim().lowercase()
//        if (q.isEmpty()) notes
//        else notes.filter {
//            it.title.lowercase().contains(q) || it.body.lowercase().contains(q)
//        }
//    }
//
//    val purple = Color(0xFF6C5CE7)
//
//    Scaffold(
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = onAddNote,
//                containerColor = purple,
//                contentColor = Color.White,
//                elevation = FloatingActionButtonDefaults.elevation(6.dp)
//            ) { Icon(Icons.Rounded.Add, contentDescription = "Add note") }
//        },
//        floatingActionButtonPosition = FabPosition.Center,
//        bottomBar = {
//            NavigationBar(containerColor = Color(0xFFF7F5FA)) {
//                NavigationBarItem(
//                    selected = selectedTab == 0,
//                    onClick = { selectedTab = 0 },
//                    icon = { Icon(Icons.Outlined.Home, null) },
//                    label = { Text("Home") }
//                )
//                Spacer(Modifier.weight(1f))
//                NavigationBarItem(
//                    selected = selectedTab == 1,
//                    onClick = {
//                        selectedTab = 1
//                        onOpenSettings()
//                    },
//                    icon = { Icon(Icons.Outlined.Settings, null) },
//                    label = { Text("Settings") }
//                )
//            }
//        }
//    ) { inner ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(inner)
//                .padding(horizontal = 16.dp)
//        ) {
//            Spacer(Modifier.height(12.dp))
//
//            // Search
//            OutlinedTextField(
//                value = query,
//                onValueChange = { query = it },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(48.dp),
//                placeholder = { Text("Search...") },
//                singleLine = true,
//                shape = RoundedCornerShape(12.dp),
//                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
//                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
//            )
//
//            Spacer(Modifier.height(12.dp))
//
//            // Section title
//            Box(
//                Modifier.fillMaxWidth(),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = "Notes",
//                    style = TextStyle(
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color(0xFF180E25)
//                    )
//                )
//            }
//
//            Spacer(Modifier.height(8.dp))
//
//            // Grid of notes (2 columns)
//            LazyVerticalGrid(
//                columns = GridCells.Fixed(2),
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(bottom = 96.dp), // room for FAB over bottom bar
//                verticalArrangement = Arrangement.spacedBy(12.dp),
//                horizontalArrangement = Arrangement.spacedBy(12.dp),
//                contentPadding = PaddingValues(bottom = 12.dp, top = 4.dp)
//            ) {
//                items(filtered, key = { it.id }) { note ->
//                    NoteCard(note = note, onClick = { onOpenNote(note) })
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun NoteCard(note: Note, onClick: () -> Unit) {
//    val bg = when (note.color) {
//        NoteColor.YellowA -> Color(0xFFFFF2C7) // warm pale yellow
//        NoteColor.YellowB -> Color(0xFFFFE9A8) // slightly stronger yellow
//    }
//    val corner = 12.dp
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(corner))
//            .background(bg)
//            .padding(12.dp)
//            .heightIn(min = 180.dp)
//            .then(Modifier.clickableWithRipple(onClick)), // helper below
//        verticalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        Text(
//            text = note.title,
//            style = TextStyle(
//                fontSize = 16.sp,
//                fontWeight = FontWeight.SemiBold,
//                color = Color(0xFF180E25),
//                lineHeight = 20.sp
//            ),
//            maxLines = 3,
//            overflow = TextOverflow.Ellipsis
//        )
//        Text(
//            text = note.body,
//            style = TextStyle(
//                fontSize = 12.sp,
//                color = Color(0xB3000000), // ~70% black
//                lineHeight = 16.sp
//            ),
//            maxLines = 8,
//            overflow = TextOverflow.Ellipsis
//        )
//    }
//}
//
///* ---------- Model & small helpers ---------- */
//
//data class Note(
//    val id: String,
//    val title: String,
//    val body: String,
//    val color: NoteColor = NoteColor.YellowA
//)
//
//enum class NoteColor { YellowA, YellowB }
//
//@Composable
//private fun Modifier.clickableWithRipple(onClick: () -> Unit) =
//    this.then(
//        Modifier
//            .clip(RoundedCornerShape(12.dp))
//            .background(Color.Transparent)
//            .clickable(
//                onClick = onClick,
//                indication = LocalIndication.current,
//                interactionSource = remember { MutableInteractionSource() })
//    )
//
