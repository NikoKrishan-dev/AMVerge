package com.amverge.android.core.result

sealed class AppError(
    val code: String,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    class SourcePermissionRevoked(message: String = "Permission to read source video was revoked", cause: Throwable? = null) :
        AppError("SOURCE_PERMISSION_REVOKED", message, cause)

    class SourceNotFound(message: String = "Source video file not found", cause: Throwable? = null) :
        AppError("SOURCE_NOT_FOUND", message, cause)

    class UnsupportedContainerOrCodec(message: String, cause: Throwable? = null) :
        AppError("UNSUPPORTED_CONTAINER_OR_CODEC", message, cause)

    class NoVideoStream(message: String = "Video stream not found in media file", cause: Throwable? = null) :
        AppError("NO_VIDEO_STREAM", message, cause)

    class InvalidMediaDuration(message: String = "Invalid or missing media duration", cause: Throwable? = null) :
        AppError("INVALID_MEDIA_DURATION", message, cause)

    class NoUsableKeyframes(message: String = "No usable keyframes found in video", cause: Throwable? = null) :
        AppError("NO_USABLE_KEYFRAMES", message, cause)

    class DetectionFailed(message: String, cause: Throwable? = null) :
        AppError("DETECTION_FAILED", message, cause)

    class ThumbnailFailed(message: String, cause: Throwable? = null) :
        AppError("THUMBNAIL_FAILED", message, cause)

    class PreviewDecoderUnavailable(message: String = "Decoder for this media format is unavailable", cause: Throwable? = null) :
        AppError("PREVIEW_DECODER_UNAVAILABLE", message, cause)

    class ExportDestinationUnavailable(message: String = "Export destination is not accessible", cause: Throwable? = null) :
        AppError("EXPORT_DESTINATION_UNAVAILABLE", message, cause)

    class StreamCopyUnsupported(message: String = "Stream copy is not supported for this stream combination", cause: Throwable? = null) :
        AppError("STREAM_COPY_UNSUPPORTED", message, cause)

    class MergeFailed(message: String, cause: Throwable? = null) :
        AppError("MERGE_FAILED", message, cause)

    class OutputVerificationFailed(message: String = "Exported file is empty or corrupted", cause: Throwable? = null) :
        AppError("OUTPUT_VERIFICATION_FAILED", message, cause)

    class ProcessCanceled(message: String = "Operation was canceled", cause: Throwable? = null) :
        AppError("PROCESS_CANCELED", message, cause)

    class InsufficientStorage(message: String = "Insufficient storage space for export", cause: Throwable? = null) :
        AppError("INSUFFICIENT_STORAGE", message, cause)

    class ScenepackNotFound(message: String = "Scenepack not found", cause: Throwable? = null) :
        AppError("SCENEPACK_NOT_FOUND", message, cause)

    class NoScenesSelected(message: String = "No scenes selected", cause: Throwable? = null) :
        AppError("NO_SCENES_SELECTED", message, cause)

    class ExportExecutionFailed(message: String = "Export execution failed", cause: Throwable? = null) :
        AppError("EXPORT_EXECUTION_FAILED", message, cause)

    class SceneNotFound(message: String = "Scene not found", cause: Throwable? = null) :
        AppError("SCENE_NOT_FOUND", message, cause)

    class InvalidSceneRange(message: String = "Invalid scene range", cause: Throwable? = null) :
        AppError("INVALID_SCENE_RANGE", message, cause)

    class Unknown(message: String, cause: Throwable? = null) :
        AppError("UNKNOWN_ERROR", message, cause)
}
