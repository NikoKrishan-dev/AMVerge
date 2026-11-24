package com.amverge.android.backend.ai

import com.amverge.android.backend.domain.model.ai.AiToolRequest
import com.amverge.android.backend.domain.model.ai.AiToolResult
import com.amverge.android.core.result.OperationResult
import java.io.File

interface AiProcessingEngine {
    /**
     * Executes the requested AI pass (Interpolation, Upscaling, or Depth Map)
     * on the specified input video clip and returns the processed video result.
     */
    suspend fun executeAiTool(
        request: AiToolRequest,
        inputVideoFile: File,
        outputVideoFile: File,
        onProgress: suspend (progress: Float, message: String) -> Unit
    ): OperationResult<AiToolResult>
}
