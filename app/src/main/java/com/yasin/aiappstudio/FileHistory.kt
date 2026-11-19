package com.yasin.aiappstudio

import java.io.File

data class FileSnapshot(val file: File, val timestamp: Long)

/** Reads the timestamped snapshots already produced by WorkspaceManager. */
object FileHistory {
    fun list(workspace: File, projectName: String, relativePath: String): List<FileSnapshot> {
        require(projectName.isNotBlank() && !projectName.startsWith(".") && !projectName.contains('/') && !projectName.contains('\\'))
        require(relativePath.isNotBlank() && !relativePath.startsWith('/') && !relativePath.contains('\\'))
        val project = File(workspace, projectName).canonicalFile
        require(project.isDirectory && project.parentFile == workspace.canonicalFile)
        val target = File(project, relativePath).canonicalFile
        require(target.path.startsWith(project.path + File.separator))
        val safe = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(relativePath.toByteArray(Charsets.UTF_8))
        val history = File(project, ".history").canonicalFile
        require(history.parentFile == project)
        if (!history.exists()) return emptyList()
        require(history.isDirectory)
        return history.listFiles().orEmpty().mapNotNull { file ->
            if (!file.isFile || !file.name.endsWith("-$safe.snapshot")) return@mapNotNull null
            val timestamp = file.name.substringBefore('-').toLongOrNull() ?: return@mapNotNull null
            FileSnapshot(file, timestamp)
        }.sortedByDescending { it.timestamp }
    }
}
