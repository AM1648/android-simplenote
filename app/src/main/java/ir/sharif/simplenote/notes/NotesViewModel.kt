package ir.sharif.simplenote.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.sharif.simplenote.SimpleNoteApi.Note
import ir.sharif.simplenote.SimpleNoteApi.NoteRequest
import ir.sharif.simplenote.SimpleNoteApi.PaginatedNoteList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/* ---------- UI State models ---------- */

sealed interface NotesListUiState {
    data object Idle : NotesListUiState
    data object Loading : NotesListUiState
    data object Loaded : NotesListUiState
    data class Success(val page: PaginatedNoteList) : NotesListUiState
    data class Error(val message: String) : NotesListUiState
}

sealed interface NoteUiState {
    data object Idle : NoteUiState
    data object Loading : NoteUiState
    data class Success(val note: Note) : NoteUiState
    data class Error(val message: String) : NoteUiState
}

sealed interface WriteUiState {
    data object Idle : WriteUiState
    data object Loading : WriteUiState
    data class Success(val note: Note) : WriteUiState
    data class Error(val message: String) : WriteUiState
}

sealed interface DeleteUiState {
    data object Idle : DeleteUiState
    data object Loading : DeleteUiState
    data class Success(val deletedId: Int) : DeleteUiState
    data class Error(val message: String) : DeleteUiState
}

sealed interface BulkUiState {
    data object Idle : BulkUiState
    data object Loading : BulkUiState
    data class Success(val notes: List<Note>) : BulkUiState
    data class Error(val message: String) : BulkUiState
}

/* ---------- ViewModel ---------- */

class NotesViewModel(
    private val repo: NotesRepository
) : ViewModel() {

    // Listing / filtering state
    private val _notesState = MutableStateFlow<NotesListUiState>(NotesListUiState.Idle)
    val notesState: StateFlow<NotesListUiState> = _notesState.asStateFlow()

    // Single-note state
    private val _noteState = MutableStateFlow<NoteUiState>(NoteUiState.Idle)
    val noteState: StateFlow<NoteUiState> = _noteState.asStateFlow()

    // Create/Update/Patch state
    private val _writeState = MutableStateFlow<WriteUiState>(WriteUiState.Idle)
    val writeState: StateFlow<WriteUiState> = _writeState.asStateFlow()

    // Delete state
    private val _deleteState = MutableStateFlow<DeleteUiState>(DeleteUiState.Idle)
    val deleteState: StateFlow<DeleteUiState> = _deleteState.asStateFlow()

    // Bulk create state
    private val _bulkState = MutableStateFlow<BulkUiState>(BulkUiState.Idle)
    val bulkState: StateFlow<BulkUiState> = _bulkState.asStateFlow()

    // Remember last successful list/filter query for "refresh"
    private var lastListCall: (suspend () -> PaginatedNoteList)? = null

    /* ---------- List & Filter ---------- */

    fun list(page: Int? = null, pageSize: Int? = null) {
        _notesState.value = NotesListUiState.Loading
        viewModelScope.launch {
            lastListCall = { repo.list(page, pageSize) }
            runCatching { lastListCall!!.invoke() }
                .onSuccess { _notesState.value = NotesListUiState.Success(it) }
                .onFailure { _notesState.value = NotesListUiState.Error(it.message ?: "Load failed") }
        }
    }

    fun filter(
        title: String? = null,
        description: String? = null,
        updatedGte: String? = null,
        updatedLte: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ) {
        _notesState.value = NotesListUiState.Loading
        viewModelScope.launch {
            lastListCall = {
                repo.filter(
                    title = title,
                    description = description,
                    updatedGte = updatedGte,
                    updatedLte = updatedLte,
                    page = page,
                    pageSize = pageSize
                )
            }
            runCatching { lastListCall!!.invoke() }
                .onSuccess { _notesState.value = NotesListUiState.Success(it) }
                .onFailure { _notesState.value = NotesListUiState.Error(it.message ?: "Filter failed") }
        }
    }

    fun refreshList() {
        val call = lastListCall ?: run {
            _notesState.value = NotesListUiState.Error("No previous list/filter to refresh")
            return
        }
        _notesState.value = NotesListUiState.Loading
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { _notesState.value = NotesListUiState.Success(it) }
                .onFailure { _notesState.value = NotesListUiState.Error(it.message ?: "Refresh failed") }
        }
    }

    /* ---------- Single note ---------- */

    fun getNote(id: Int) {
        _noteState.value = NoteUiState.Loading
        viewModelScope.launch {
            runCatching { repo.get(id) }
                .onSuccess { _noteState.value = NoteUiState.Success(it) }
                .onFailure { _noteState.value = NoteUiState.Error(it.message ?: "Load note failed") }
        }
    }

    /* ---------- Create / Update / Patch ---------- */

    fun createNote(title: String, description: String) {
        if (title.isBlank() || description.isBlank()) {
            _writeState.value = WriteUiState.Error("Title and description are required")
            return
        }
        _writeState.value = WriteUiState.Loading
        viewModelScope.launch {
            runCatching { repo.create(title, description) }
                .onSuccess { _writeState.value = WriteUiState.Success(it) }
                .onFailure { _writeState.value = WriteUiState.Error(it.message ?: "Create failed") }
        }
    }

    fun updateNote(id: Int, title: String, description: String) {
        if (title.isBlank() || description.isBlank()) {
            _writeState.value = WriteUiState.Error("Title and description are required")
            return
        }
        _writeState.value = WriteUiState.Loading
        viewModelScope.launch {
            runCatching { repo.update(id, title, description) }
                .onSuccess { _writeState.value = WriteUiState.Success(it) }
                .onFailure { _writeState.value = WriteUiState.Error(it.message ?: "Update failed") }
        }
    }

    fun patchNote(id: Int, title: String? = null, description: String? = null) {
        if (title.isNullOrBlank() && description.isNullOrBlank()) {
            _writeState.value = WriteUiState.Error("Provide at least one field to patch")
            return
        }
        _writeState.value = WriteUiState.Loading
        viewModelScope.launch {
            runCatching { repo.patch(id, title, description) }
                .onSuccess { _writeState.value = WriteUiState.Success(it) }
                .onFailure { _writeState.value = WriteUiState.Error(it.message ?: "Patch failed") }
        }
    }

    /* ---------- Delete ---------- */

    fun deleteNote(id: Int) {
        _deleteState.value = DeleteUiState.Loading
        viewModelScope.launch {
            runCatching { repo.delete(id) }
                .onSuccess { _deleteState.value = DeleteUiState.Success(id) }
                .onFailure { _deleteState.value = DeleteUiState.Error(it.message ?: "Delete failed") }
        }
    }

    /* ---------- Bulk create ---------- */

    fun bulkCreate(pairs: List<Pair<String, String>>) {
        if (pairs.isEmpty()) {
            _bulkState.value = BulkUiState.Error("Provide at least one note")
            return
        }
        _bulkState.value = BulkUiState.Loading
        viewModelScope.launch {
            val bodies = pairs.map { (t, d) -> NoteRequest(title = t, description = d) }
            runCatching { repo.bulkCreate(bodies) }
                .onSuccess { _bulkState.value = BulkUiState.Success(it) }
                .onFailure { _bulkState.value = BulkUiState.Error(it.message ?: "Bulk create failed") }
        }
    }

    /* ---------- Helpers to reset states (optional for UI flows) ---------- */

    fun clearWriteState() { _writeState.value = WriteUiState.Idle }
    fun clearDeleteState() { _deleteState.value = DeleteUiState.Idle }
    fun clearNoteState() { _noteState.value = NoteUiState.Idle }
    fun clearListState() { _notesState.value = NotesListUiState.Idle }
    fun clearBulkState() { _bulkState.value = BulkUiState.Idle }
    fun finalizeListState() {
        _notesState.value = NotesListUiState.Loaded
    }
}
