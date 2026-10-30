package io.github.natnael_dev.dailyread.data.ingest

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class AndroidTreeDocumentProvider(
    private val contentResolver: ContentResolver
) : TreeDocumentProvider {

    override suspend fun queryTreeDocuments(treeUri: String): List<ScannedDocument> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedDocument>()
        val uri = Uri.parse(treeUri)
        val treeDocumentId = DocumentsContract.getTreeDocumentId(uri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, treeDocumentId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        try {
            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex) ?: "Unknown"
                    val mime = cursor.getString(mimeIndex) ?: ""
                    val size = if (cursor.isNull(sizeIndex)) 0L else cursor.getLong(sizeIndex)
                    val mod = if (cursor.isNull(modIndex)) 0L else cursor.getLong(modIndex)

                    val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId).toString()

                    results.add(
                        ScannedDocument(
                            documentId = docId,
                            documentUri = docUri,
                            displayName = name,
                            mimeType = mime,
                            sizeBytes = size,
                            lastModified = mod
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Return collected results or empty list on query failure
        }
        results
    }

    override suspend fun openInputStream(documentUri: String): InputStream? = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(Uri.parse(documentUri))
        } catch (e: Exception) {
            null
        }
    }
}
