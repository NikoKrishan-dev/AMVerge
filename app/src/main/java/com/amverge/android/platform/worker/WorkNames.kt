package com.amverge.android.platform.worker

object WorkNames {
    fun importWorkName(projectId: String): String = "import:$projectId"
    fun exportWorkName(jobId: String): String = "export:$jobId"
}
