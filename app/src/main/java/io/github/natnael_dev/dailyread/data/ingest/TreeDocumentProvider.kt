package io.github.natnael_dev.dailyread.data.ingest

import java.io.InputStream

interface TreeDocumentProvider {
    suspend fun queryTreeDocuments(treeUri: String): List<ScannedDocument>
    suspend fun openInputStream(documentUri: String): InputStream?
}
