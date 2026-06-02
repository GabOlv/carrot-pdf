package com.example.carrotpdf.workspace

import android.content.Context
import com.example.carrotpdf.model.PdfTab
import java.io.File

class WorkspaceRepository(
    context: Context
) {
    private val workspaceDirectory = File(context.filesDir, WORKSPACE_DIRECTORY_NAME)

    fun loadOrCreate(tab: PdfTab): CarrotWorkspace {
        val existing = load(tab.id)
        val workspace = existing ?: CarrotWorkspace(
            tabId = tab.id,
            pdfUri = tab.uri.toString(),
            title = tab.title
        )
        val updated = workspace.withDocumentMetadata(
            pdfUri = tab.uri.toString(),
            title = tab.title
        )

        if (existing == null || updated != existing) {
            save(updated)
        }

        return updated
    }

    fun ensureWorkspaces(tabs: List<PdfTab>) {
        tabs.forEach { tab ->
            loadOrCreate(tab)
        }
    }

    fun updateNotes(
        tab: PdfTab,
        text: String
    ): CarrotWorkspace {
        val now = System.currentTimeMillis()
        val workspace = loadOrCreate(tab)

        if (workspace.notes.text == text) {
            return workspace
        }

        val updated = workspace.copy(
            notes = WorkspaceNotes(
                text = text,
                updatedAt = now
            ),
            updatedAt = now
        )

        save(updated)
        return updated
    }

    fun load(tabId: String): CarrotWorkspace? {
        val file = workspaceFile(tabId)

        if (!file.exists()) {
            return null
        }

        return runCatching {
            workspaceFromJson(org.json.JSONObject(file.readText()))
        }.getOrNull()
    }

    fun save(workspace: CarrotWorkspace) {
        if (!workspaceDirectory.exists()) {
            workspaceDirectory.mkdirs()
        }

        workspaceFile(workspace.tabId).writeText(
            workspace.toJson().toString(2)
        )
    }

    private fun workspaceFile(tabId: String): File {
        return File(workspaceDirectory, "${tabId.safeWorkspaceFileName()}.json")
    }

    private fun String.safeWorkspaceFileName(): String {
        return replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .take(96)
            .ifBlank { "workspace" }
    }

    private companion object {
        const val WORKSPACE_DIRECTORY_NAME = "workspaces"
    }
}
