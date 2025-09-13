package ir.sharif.simplenote.notes


import android.os.Build
import androidx.annotation.RequiresApi
import ir.sharif.simplenote.SimpleNoteApi
import ir.sharif.simplenote.SimpleNoteApi.NotesApi
import ir.sharif.simplenote.SimpleNoteApi.Note
import ir.sharif.simplenote.SimpleNoteApi.NoteRequest
import ir.sharif.simplenote.SimpleNoteApi.PaginatedNoteList
import ir.sharif.simplenote.SimpleNoteApi.PatchedNoteRequest
import java.time.ZonedDateTime

/**
 * Repository for SimpleNote "notes" endpoints.
 *
 * ⚠️ Requires a valid JWT in SimpleNoteApi.tokens.access; the Bearer interceptor
 * will attach it automatically once set (e.g., after login).
 */
class NotesRepository(
    private val api: NotesApi
) {
    // List
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun list(page: Int? = null, pageSize: Int? = null): PaginatedNoteList =
        api.listNotes(page = page, pageSize = pageSize)
//        PaginatedNoteList(100, "dummy", null,
//            generateSequence((page!! - 1)!! * pageSize!! + 1) { it + 1 }.take(pageSize).map { i ->
//                Note(i, "Note $i", "Description for note $i, auto generated for test", ZonedDateTime.now().toString(), ZonedDateTime.now().toString(), "Me", "MeUser")
//            }.toList()
//        )


    // Filter list
    suspend fun filter(
        title: String? = null,
        description: String? = null,
        updatedGte: String? = null, // ISO datetime
        updatedLte: String? = null, // ISO datetime
        page: Int? = null,
        pageSize: Int? = null
    ): PaginatedNoteList =
        api.filterNotes(
            title = title,
            description = description,
            updatedGte = updatedGte,
            updatedLte = updatedLte,
            page = page,
            pageSize = pageSize
        )

    // Create
    suspend fun create(title: String, description: String): Note =
        api.createNote(NoteRequest(title = title, description = description))

    // Retrieve single note
    suspend fun get(id: Int): Note = api.getNote(id)

    // Update (PUT)
    suspend fun update(id: Int, title: String, description: String): Note =
        api.updateNote(id, NoteRequest(title = title, description = description))

    // Patch (PATCH)
    suspend fun patch(id: Int, title: String? = null, description: String? = null): Note =
        api.patchNote(id, PatchedNoteRequest(title = title, description = description))

    // Delete (returns Unit)
    suspend fun delete(id: Int) = api.deleteNote(id)

    // Bulk create
    suspend fun bulkCreate(items: List<NoteRequest>): List<Note> =
        api.bulkCreate(items)
}
