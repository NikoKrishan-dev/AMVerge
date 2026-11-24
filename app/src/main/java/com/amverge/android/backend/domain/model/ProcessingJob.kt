package com.amverge.android.backend.domain.model

enum class JobType {
    IMPORT,
    THUMBNAIL,
    EXPORT,
    AI_TOOL
}

enum class JobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED
}

enum class ProcessingStage {
    PROBING,
    DETECTING,
    SAVING_SCENES,
    THUMBNAILS,
    EXPORTING,
    VERIFYING,
    AI_PREPARING,
    AI_INFERENCE,
    AI_ENCODING
}

data class ProcessingJob(
    val id: String,
    val projectId: String,
    val type: JobType,
    val status: JobStatus,
    val stage: ProcessingStage,
    val progress: Float,
    val message: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val workName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ExportMode {
    INDIVIDUAL,
    MERGED,
    EDL_TIMELINE
}

enum class AspectRatioCrop(val displayName: String) {
    ORIGINAL("Original (16:9)"),
    VERTICAL_9_16("Shorts/Reels (9:16)"),
    SQUARE_1_1("Square (1:1)"),
    STANDARD_4_3("Retro (4:3)")
}

enum class AudioExportOption(val displayName: String) {
    KEEP_AUDIO("Keep Audio"),
    MUTE_AUDIO("Mute Audio (Clean Cuts)")
}

data class ExportRequest(
    val projectId: String,
    val sceneIds: List<String>,
    val mode: ExportMode,
    val destinationTreeUri: String? = null,
    val destinationDocumentUri: String? = null,
    val outputFileName: String? = null,
    val aspectRatio: AspectRatioCrop = AspectRatioCrop.ORIGINAL,
    val audioOption: AudioExportOption = AudioExportOption.KEEP_AUDIO
)
