package ir.sharif.simplenote.notes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ir.sharif.simplenote.R
import ir.sharif.simplenote.SettingsActivity
import ir.sharif.simplenote.SimpleNoteApi

class NotesHomeActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels { NotesViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                val context = LocalContext.current

                // Aggregated notes across pages
                val notes = remember { mutableStateListOf<SimpleNoteApi.Note>() }
                var hasNext by remember { mutableStateOf(true) }
                var page by remember { mutableStateOf(1) }

                // Tracks which page we actually requested most recently.
                var requestedPage by remember { mutableStateOf(1) }

                // Prevent parallel duplicate requests
                var isRequesting by remember { mutableStateOf(false) }

                var isFirstLoadEnded by remember { mutableStateOf(false) }

                val listState by viewModel.notesState.collectAsState()
                val writeState by viewModel.writeState.collectAsState()
                val deleteState by viewModel.deleteState.collectAsState()

                val PAGE_SIZE = 8 // page size set to 8

                // Initial load (request page 1)
                LaunchedEffect(Unit) {
                    page = 1
                    requestedPage = 1
                    notes.clear()
                    hasNext = true
                    Log.d("NotesHomeActivity", "Initial load: page=$page")
                    isRequesting = true
                    viewModel.list(page = page, pageSize = PAGE_SIZE)
                }

                // React to viewmodel results (merge / upsert)
                // Keyed only on listState to avoid re-running when `page` changes.
                LaunchedEffect(listState) {
                    when (listState) {
                        is NotesListUiState.Success -> {
                            val p = (listState as NotesListUiState.Success).page
                            Log.d("NotesHomeActivity", "Received page results: requestedPage=$requestedPage count=${p.results.size} next=${p.next}")

                            // If requestedPage == 1, MERGE page1 into existing list
                            if (requestedPage == 1) {
                                // update existing items or prepend new ones from page1 but DO NOT clear subsequent pages
                                // We'll upsert server results: replace matching ids, or insert at front if new
                                p.results.reversed().forEach { serverNote ->
                                    val idx = notes.indexOfFirst { it.id == serverNote.id }
                                    if (idx >= 0) {
                                        notes[idx] = serverNote
                                        Log.d("NotesHomeActivity", "Updated note id=${serverNote.id} (merge page1)")
                                    } else {
                                        // insert at beginning so page1 items remain ordered first
                                        notes.add(0, serverNote)
                                        Log.d("NotesHomeActivity", "Prepended note id=${serverNote.id} (merge page1)")
                                    }
                                }
                            } else {
                                // For subsequent pages, upsert each item (append or replace)
                                p.results.forEach { serverNote ->
                                    val idx = notes.indexOfFirst { it.id == serverNote.id }
                                    if (idx >= 0) {
                                        notes[idx] = serverNote // replace (update)
                                        Log.d("NotesHomeActivity", "Updated note id=${serverNote.id}")
                                    } else {
                                        notes.add(serverNote) // append new
                                        Log.d("NotesHomeActivity", "Appended note id=${serverNote.id}")
                                    }
                                }
                            }

                            hasNext = p.next != null

                            // show popup after successful page merge
                            Toast.makeText(context, "Page $requestedPage loaded", Toast.LENGTH_SHORT).show()
                            isRequesting = false
                            viewModel.finalizeListState()
                            isFirstLoadEnded = true
                        }
                        is NotesListUiState.Error -> {
                            Log.w("NotesHomeActivity", "List load error: ${(listState as NotesListUiState.Error).message}")
                            isRequesting = false
                        }
                        is NotesListUiState.Loading -> {
                            // mark requesting
                            isRequesting = true
                        }
                        else -> Unit
                    }
                }

                // When create succeeds, jump to editor and ensure note is in local list
                LaunchedEffect(writeState) {
                    Log.d("NotesHomeActivity", "LaunchedEffect(writeState): $writeState")
                    if (writeState is WriteUiState.Success) {
                        val created = (writeState as WriteUiState.Success).note
                        Log.d("NotesHomeActivity", "writeState Success — created: id=${created.id}")
                        // upsert created note at top
                        val idx = notes.indexOfFirst { it.id == created.id }
                        if (idx >= 0) notes[idx] = created else notes.add(0, created)
                        // open editor
                        val intent = Intent(context, NoteActivity::class.java).apply {
                            putExtra("note_id", created.id)
                        }
                        context.startActivity(intent)
                        viewModel.clearWriteState()
                    }
                }

                // When delete succeeds, remove deleted id from local list
                LaunchedEffect(deleteState) {
                    if (deleteState is DeleteUiState.Success) {
                        val deletedId = (deleteState as DeleteUiState.Success).deletedId
                        val idx = notes.indexOfFirst { it.id == deletedId }
                        if (idx >= 0) {
                            notes.removeAt(idx)
                            Log.d("NotesHomeActivity", "Removed deleted note id=$deletedId from local list")
                        } else {
                            Log.d("NotesHomeActivity", "Delete reported for id=$deletedId but not found locally")
                        }
                        // refresh page1 merge (do not clear other pages)
                        page = 1
                        requestedPage = 1
                        Log.d("NotesHomeActivity", "Refreshing page1 (merge) after delete")
                        viewModel.list(page = page, pageSize = PAGE_SIZE)
                        viewModel.clearDeleteState()
                    }
                }

                fun refreshAllMergePage1() {
                    // Helpful manual refresh: merge page1 into existing list (no clear)
                    page = 1
                    requestedPage = 1
                    Log.d("NotesHomeActivity", "Manual refreshAll (merge page1) called")
                    viewModel.list(page = page, pageSize = PAGE_SIZE)
                }

                NotesHomeScreen(
                    notes = notes,
                    onBottomReached = {
                        // trigger next page only when we have more and not currently requesting
                        if (hasNext && !isRequesting) {
                            page += 1
                            requestedPage = page // record the page we're requesting
                            Log.d("NotesHomeActivity", "Requesting page $page")
                            viewModel.list(page = page, pageSize = PAGE_SIZE)
                        } else {
                            Log.d("NotesHomeActivity", "onBottomReached ignored: hasNext=$hasNext isRequesting=$isRequesting")
                        }
                    },
                    onAddNote = {
                        Log.d("NotesHomeActivity", "FAB clicked — calling viewModel.createNote()")
                        viewModel.createNote("New Note", "New Description")
                    },
                    onOpenNote = { note ->
                        val i = Intent(context, NoteActivity::class.java)
                            .putExtra("note_id", note.id)
                        context.startActivity(i)
                    },
                    onOpenSettings = {
                        startActivity(
                            Intent(this, SettingsActivity::class.java)
                                .putExtra("message", "Hello from Home")
                        )
                    },
                    // treat only Loading as loading — Success/Loaded are not "loading" for the UI
                    isLoading = listState is NotesListUiState.Loading,
                    firstLoadEnded = isFirstLoadEnded
                )

                // Refresh on resume — MERGE page 1 instead of clearing everything so pages previously loaded stay present.
                DisposableEffect(Unit) {
                    val observer = LifecycleEventObserver { _, e ->
                        if (e == Lifecycle.Event.ON_RESUME) {
                            Log.d("NotesHomeActivity", "ON_RESUME -> merge-refresh page 1 to sync with backend")
                            // merge page1 results into existing notes (don't clear other pages)
                            page = 1
                            requestedPage = 1
                            viewModel.list(page = page, pageSize = PAGE_SIZE)
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }
            }
        }
    }
}


/* ---------- UI for Home ---------- */

/* The rest of your composables (NotesHomeScreen, NoteCard, clickableWithRipple)
   remain unchanged — omitted here for brevity but keep them exactly as you had. */


/* ---------- UI for Home ---------- */

@Composable
private fun NotesHomeScreen(
    notes: List<SimpleNoteApi.Note>,
    onBottomReached: () -> Unit,
    onAddNote: () -> Unit,
    onOpenNote: (SimpleNoteApi.Note) -> Unit,
    onOpenSettings: () -> Unit,
    isLoading: Boolean,
    firstLoadEnded: Boolean
) {
    var query by rememberSaveable { mutableStateOf("") }

    val filtered = remember(notes, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) notes
        else notes.filter {
            it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }

    val purple = Color(0xFF6C5CE7)
    val textPrimary = Color(0xFF180E25)
    val textSecondary = Color(0xFF827D89)
    val gridState = rememberLazyGridState()

    // Infinite scroll: call onBottomReached near end
    LaunchedEffect(filtered, gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisible ->
                if (lastVisible != null && lastVisible >= filtered.size - 4) {
                    onBottomReached()
                }
            }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNote,
                containerColor = purple,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) { Icon(Icons.Rounded.Add, contentDescription = "Add note") }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            Column {
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = purple,
                        trackColor = Color.Transparent
                    )
                }
                NavigationBar(containerColor = Color(0xFFF7F5FA)) {
                    NavigationBarItem(
                        selected = true,
                        onClick = { },
                        icon = { Icon(Icons.Outlined.Home, null) },
                        label = { Text("Home") }
                    )
                    Spacer(Modifier.weight(1f))
                    NavigationBarItem(
                        selected = false,
                        onClick = { onOpenSettings() },
                        icon = { Icon(Icons.Outlined.Settings, null) },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { inner ->
        if (notes.isEmpty() && firstLoadEnded) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home), // <-- add your drawable
                    contentDescription = "Empty state",
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Start Your Journey",
                    style = TextStyle(
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Every big step start with small step.\n" +
                            "Notes your first idea and start\n" +
                            "your journey!",
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    placeholder = { Text("Search...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )

                Spacer(Modifier.height(12.dp))

                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Notes",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF180E25)
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp)
                ) {
                    items(filtered, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            colorAlternate = (note.id % 2 == 0),
                            onClick = { onOpenNote(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: SimpleNoteApi.Note,
    colorAlternate: Boolean,
    onClick: () -> Unit
) {
    val bg = if (colorAlternate) Color(0xFFFFE9A8) else Color(0xFFFFF2C7)
    val corner = 12.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .padding(12.dp)
            .heightIn(min = 180.dp)
            .then(Modifier.clickableWithRipple(onClick)),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = note.title.ifBlank { "Untitled" },
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF180E25),
                lineHeight = 20.sp
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = note.description,
            style = TextStyle(
                fontSize = 12.sp,
                color = Color(0xB3000000),
                lineHeight = 16.sp
            ),
            maxLines = 8,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Modifier.clickableWithRipple(onClick: () -> Unit) =
    this.then(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .clickable(
                onClick = onClick,
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() }
            )
    )
