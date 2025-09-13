package ir.sharif.simplenote.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.sharif.simplenote.SimpleNoteApi

class NotesViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val notesApi = SimpleNoteApi.create(SimpleNoteApi.NotesApi::class.java)
        val repo = NotesRepository(notesApi)
        return NotesViewModel(repo) as T
    }
}
