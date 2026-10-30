package io.github.natnael_dev.dailyread.data.ingest

data class ScannedDocument(
    val documentId: String,
    val documentUri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long
)
