package com.example.carrotpdf.workspace

import org.json.JSONArray
import org.json.JSONObject

data class CarrotWorkspace(
    val tabId: String,
    val pdfUri: String,
    val title: String,
    val notes: WorkspaceNotes = WorkspaceNotes(),
    val canvas: WorkspaceCanvas = WorkspaceCanvas(),
    val pageInk: List<PageInkStroke> = emptyList(),
    val textMarkers: List<PageTextMarker> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    fun withDocumentMetadata(
        pdfUri: String,
        title: String,
        now: Long = System.currentTimeMillis()
    ): CarrotWorkspace {
        if (this.pdfUri == pdfUri && this.title == title) {
            return this
        }

        return copy(
            pdfUri = pdfUri,
            title = title,
            updatedAt = now
        )
    }
}

data class WorkspaceNotes(
    val text: String = "",
    val updatedAt: Long = 0L
)

data class WorkspaceCanvas(
    val width: Float = DEFAULT_CANVAS_WIDTH,
    val height: Float = DEFAULT_CANVAS_HEIGHT,
    val strokes: List<CanvasInkStroke> = emptyList(),
    val updatedAt: Long = 0L
)

data class PageInkStroke(
    val id: String,
    val pageIndex: Int,
    val tool: InkTool,
    val color: Long,
    val width: Float,
    val points: List<InkPoint>,
    val createdAt: Long = System.currentTimeMillis()
)

data class PageTextMarker(
    val id: String,
    val pageIndex: Int,
    val text: String,
    val color: Long,
    val bounds: List<PageTextMarkerBounds>,
    val createdAt: Long = System.currentTimeMillis()
)

data class PageTextMarkerBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val pageWidth: Float,
    val pageHeight: Float
)

data class CanvasInkStroke(
    val id: String,
    val tool: InkTool,
    val color: Long,
    val width: Float,
    val points: List<InkPoint>,
    val createdAt: Long = System.currentTimeMillis()
)

data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f
)

enum class InkTool {
    Pen,
    Highlighter,
    Eraser
}

internal const val DEFAULT_CANVAS_WIDTH = 5000f
internal const val DEFAULT_CANVAS_HEIGHT = 7000f

internal fun CarrotWorkspace.toJson(): JSONObject {
    return JSONObject()
        .put("schemaVersion", WORKSPACE_SCHEMA_VERSION)
        .put("tabId", tabId)
        .put("pdfUri", pdfUri)
        .put("title", title)
        .put("notes", notes.toJson())
        .put("canvas", canvas.toJson())
        .put("pageInk", JSONArray().also { array ->
            pageInk.forEach { stroke -> array.put(stroke.toJson()) }
        })
        .put("textMarkers", JSONArray().also { array ->
            textMarkers.forEach { marker -> array.put(marker.toJson()) }
        })
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
}

internal fun workspaceFromJson(json: JSONObject): CarrotWorkspace {
    return CarrotWorkspace(
        tabId = json.optString("tabId"),
        pdfUri = json.optString("pdfUri"),
        title = json.optString("title").ifBlank { "Document.pdf" },
        notes = notesFromJson(json.optJSONObject("notes")),
        canvas = canvasFromJson(json.optJSONObject("canvas")),
        pageInk = pageInkFromJson(json.optJSONArray("pageInk")),
        textMarkers = textMarkersFromJson(json.optJSONArray("textMarkers")),
        createdAt = json.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = json.optLong("updatedAt", 0L)
    )
}

private fun WorkspaceNotes.toJson(): JSONObject {
    return JSONObject()
        .put("text", text)
        .put("updatedAt", updatedAt)
}

private fun notesFromJson(json: JSONObject?): WorkspaceNotes {
    if (json == null) {
        return WorkspaceNotes()
    }

    return WorkspaceNotes(
        text = json.optString("text"),
        updatedAt = json.optLong("updatedAt", 0L)
    )
}

private fun WorkspaceCanvas.toJson(): JSONObject {
    return JSONObject()
        .put("width", width.toDouble())
        .put("height", height.toDouble())
        .put("updatedAt", updatedAt)
        .put("strokes", JSONArray().also { array ->
            strokes.forEach { stroke -> array.put(stroke.toJson()) }
        })
}

private fun canvasFromJson(json: JSONObject?): WorkspaceCanvas {
    if (json == null) {
        return WorkspaceCanvas()
    }

    return WorkspaceCanvas(
        width = json.optDouble("width", DEFAULT_CANVAS_WIDTH.toDouble()).toFloat(),
        height = json.optDouble("height", DEFAULT_CANVAS_HEIGHT.toDouble()).toFloat(),
        strokes = canvasInkFromJson(json.optJSONArray("strokes")),
        updatedAt = json.optLong("updatedAt", 0L)
    )
}

private fun PageInkStroke.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("pageIndex", pageIndex)
        .put("tool", tool.name)
        .put("color", color)
        .put("width", width.toDouble())
        .put("createdAt", createdAt)
        .put("points", points.toJson())
}

private fun CanvasInkStroke.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("tool", tool.name)
        .put("color", color)
        .put("width", width.toDouble())
        .put("createdAt", createdAt)
        .put("points", points.toJson())
}

private fun PageTextMarker.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("pageIndex", pageIndex)
        .put("text", text)
        .put("color", color)
        .put("createdAt", createdAt)
        .put("bounds", JSONArray().also { array ->
            bounds.forEach { bound -> array.put(bound.toJson()) }
        })
}

private fun PageTextMarkerBounds.toJson(): JSONObject {
    return JSONObject()
        .put("left", left.toDouble())
        .put("top", top.toDouble())
        .put("right", right.toDouble())
        .put("bottom", bottom.toDouble())
        .put("pageWidth", pageWidth.toDouble())
        .put("pageHeight", pageHeight.toDouble())
}

private fun List<InkPoint>.toJson(): JSONArray {
    return JSONArray().also { array ->
        forEach { point ->
            array.put(
                JSONObject()
                    .put("x", point.x.toDouble())
                    .put("y", point.y.toDouble())
                    .put("pressure", point.pressure.toDouble())
            )
        }
    }
}

private fun pageInkFromJson(array: JSONArray?): List<PageInkStroke> {
    if (array == null) {
        return emptyList()
    }

    return buildList {
        repeat(array.length()) { index ->
            val item = array.optJSONObject(index) ?: return@repeat
            val id = item.optString("id").takeIf { it.isNotBlank() } ?: return@repeat
            val pageIndex = item.optInt("pageIndex", -1)

            if (pageIndex < 0) {
                return@repeat
            }

            add(
                PageInkStroke(
                    id = id,
                    pageIndex = pageIndex,
                    tool = inkToolFromJson(item.optString("tool")),
                    color = item.optLong("color", DEFAULT_INK_COLOR),
                    width = item.optDouble("width", DEFAULT_INK_WIDTH.toDouble()).toFloat(),
                    points = pointsFromJson(item.optJSONArray("points")),
                    createdAt = item.optLong("createdAt", 0L)
                )
            )
        }
    }
}

private fun canvasInkFromJson(array: JSONArray?): List<CanvasInkStroke> {
    if (array == null) {
        return emptyList()
    }

    return buildList {
        repeat(array.length()) { index ->
            val item = array.optJSONObject(index) ?: return@repeat
            val id = item.optString("id").takeIf { it.isNotBlank() } ?: return@repeat

            add(
                CanvasInkStroke(
                    id = id,
                    tool = inkToolFromJson(item.optString("tool")),
                    color = item.optLong("color", DEFAULT_INK_COLOR),
                    width = item.optDouble("width", DEFAULT_INK_WIDTH.toDouble()).toFloat(),
                    points = pointsFromJson(item.optJSONArray("points")),
                    createdAt = item.optLong("createdAt", 0L)
                )
            )
        }
    }
}

private fun textMarkersFromJson(array: JSONArray?): List<PageTextMarker> {
    if (array == null) {
        return emptyList()
    }

    return buildList {
        repeat(array.length()) { index ->
            val item = array.optJSONObject(index) ?: return@repeat
            val id = item.optString("id").takeIf { it.isNotBlank() } ?: return@repeat
            val pageIndex = item.optInt("pageIndex", -1)

            if (pageIndex < 0) {
                return@repeat
            }

            add(
                PageTextMarker(
                    id = id,
                    pageIndex = pageIndex,
                    text = item.optString("text"),
                    color = item.optLong("color", DEFAULT_MARKER_COLOR),
                    bounds = markerBoundsFromJson(item.optJSONArray("bounds")),
                    createdAt = item.optLong("createdAt", 0L)
                )
            )
        }
    }
}

private fun markerBoundsFromJson(array: JSONArray?): List<PageTextMarkerBounds> {
    if (array == null) {
        return emptyList()
    }

    return buildList {
        repeat(array.length()) { index ->
            val item = array.optJSONObject(index) ?: return@repeat
            val pageWidth = item.optDouble("pageWidth", 0.0).toFloat()
            val pageHeight = item.optDouble("pageHeight", 0.0).toFloat()
            val left = item.optDouble("left", 0.0).toFloat()
            val top = item.optDouble("top", 0.0).toFloat()
            val right = item.optDouble("right", 0.0).toFloat()
            val bottom = item.optDouble("bottom", 0.0).toFloat()

            if (pageWidth <= 0f || pageHeight <= 0f || right <= left || bottom <= top) {
                return@repeat
            }

            add(
                PageTextMarkerBounds(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    pageWidth = pageWidth,
                    pageHeight = pageHeight
                )
            )
        }
    }
}

private fun pointsFromJson(array: JSONArray?): List<InkPoint> {
    if (array == null) {
        return emptyList()
    }

    return buildList {
        repeat(array.length()) { index ->
            val item = array.optJSONObject(index) ?: return@repeat
            add(
                InkPoint(
                    x = item.optDouble("x", 0.0).toFloat(),
                    y = item.optDouble("y", 0.0).toFloat(),
                    pressure = item.optDouble("pressure", 1.0).toFloat()
                )
            )
        }
    }
}

private fun inkToolFromJson(raw: String): InkTool {
    return InkTool.entries.firstOrNull { tool -> tool.name == raw } ?: InkTool.Pen
}

private const val WORKSPACE_SCHEMA_VERSION = 1
private const val DEFAULT_INK_COLOR = 0xFFFF5A10
private const val DEFAULT_MARKER_COLOR = 0xFFFFD966
private const val DEFAULT_INK_WIDTH = 4f
