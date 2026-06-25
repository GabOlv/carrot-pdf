package com.example.carrotpdf.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.workspace.CanvasInkStroke
import com.example.carrotpdf.workspace.DEFAULT_CANVAS_HEIGHT
import com.example.carrotpdf.workspace.DEFAULT_CANVAS_WIDTH
import com.example.carrotpdf.workspace.InkPoint
import com.example.carrotpdf.workspace.InkTool
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class WorkspaceMode {
    Notes,
    Draw
}

enum class DrawTarget {
    Canvas,
    Pdf
}

enum class WorkspaceSheetLayout {
    Bottom,
    Side
}

enum class WorkspaceDrawTool {
    Pen,
    Marker,
    Move,
    Eraser
}

@Composable
fun NotesWorkspaceSheet(
    notesText: String,
    onNotesChange: (String) -> Unit,
    selectedMode: WorkspaceMode,
    onModeChange: (WorkspaceMode) -> Unit,
    selectedDrawTarget: DrawTarget,
    onDrawTargetChange: (DrawTarget) -> Unit,
    selectedDrawTool: WorkspaceDrawTool,
    onDrawToolChange: (WorkspaceDrawTool) -> Unit,
    selectedInkColor: Long,
    onInkColorChange: (Long) -> Unit,
    selectedStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    canvasStrokes: List<CanvasInkStroke>,
    onCanvasStrokesChange: (List<CanvasInkStroke>) -> Unit,
    pageInkStrokeCount: Int,
    onUndoPageInk: () -> Unit,
    onCollapse: () -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    layout: WorkspaceSheetLayout,
    modifier: Modifier = Modifier
) {
    val isSideLayout = layout == WorkspaceSheetLayout.Side
    val sheetShape = if (isSideLayout) {
        RoundedCornerShape(20.dp)
    } else {
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    }
    val sheetModifier = if (isSideLayout) {
        modifier
            .fillMaxHeight()
            .shadow(
                elevation = 18.dp,
                shape = sheetShape
            )
    } else {
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = 22.dp,
                shape = sheetShape
            )
    }

    Surface(
        modifier = sheetModifier,
        color = if (isSideLayout) Color(0xF8F7F7F4) else Color(0xF8F7F7F4),
        shape = sheetShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.08f),
                    shape = sheetShape
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            WorkspaceDragHandle(
                isExpanded = isExpanded,
                onExpandedChange = onExpandedChange,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                WorkspaceModeButton(
                    mode = WorkspaceMode.Notes,
                    selected = selectedMode == WorkspaceMode.Notes,
                    onClick = {
                        onModeChange(WorkspaceMode.Notes)
                    }
                )

                Spacer(modifier = Modifier.width(16.dp))

                WorkspaceModeButton(
                    mode = WorkspaceMode.Draw,
                    selected = selectedMode == WorkspaceMode.Draw,
                    onClick = {
                        onModeChange(WorkspaceMode.Draw)
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                WorkspaceCollapseButton(onClick = onCollapse)
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedMode) {
                WorkspaceMode.Notes -> NotesEditor(
                    notesText = notesText,
                    onNotesChange = onNotesChange,
                    isSideLayout = isSideLayout,
                    modifier = Modifier.weight(1f)
                )

                WorkspaceMode.Draw -> DrawWorkspace(
                    selectedDrawTarget = selectedDrawTarget,
                    onDrawTargetChange = onDrawTargetChange,
                    selectedDrawTool = selectedDrawTool,
                    onDrawToolChange = onDrawToolChange,
                    selectedInkColor = selectedInkColor,
                    onInkColorChange = onInkColorChange,
                    selectedStrokeWidth = selectedStrokeWidth,
                    onStrokeWidthChange = onStrokeWidthChange,
                    canvasStrokes = canvasStrokes,
                    onCanvasStrokesChange = onCanvasStrokesChange,
                    pageInkStrokeCount = pageInkStrokeCount,
                    onUndoPageInk = onUndoPageInk,
                    isSideLayout = isSideLayout,
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NotesEditor(
    notesText: String,
    onNotesChange: (String) -> Unit,
    isSideLayout: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val fieldShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .background(
                color = Color.White,
                shape = fieldShape
            )
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.08f),
                shape = fieldShape
            )
            .clip(fieldShape)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val lineSpacing = 24.dp.toPx()
            val leftInset = 14.dp.toPx()
            val rightInset = size.width - 14.dp.toPx()
            var y = 42.dp.toPx()

            while (y < size.height) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.055f),
                    start = Offset(leftInset, y),
                    end = Offset(rightInset, y),
                    strokeWidth = 1f
                )
                y += lineSpacing
            }
        }

        BasicTextField(
            value = notesText,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(14.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF1D2228)
            ),
            cursorBrush = SolidColor(CarrotColors.Accent),
            decorationBox = { innerTextField ->
                if (notesText.isBlank()) {
                    Text(
                        text = "Type your notes...",
                        color = Color(0xFF8D949C),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                innerTextField()
            }
        )
    }
}

@Composable
private fun DrawWorkspace(
    selectedDrawTarget: DrawTarget,
    onDrawTargetChange: (DrawTarget) -> Unit,
    selectedDrawTool: WorkspaceDrawTool,
    onDrawToolChange: (WorkspaceDrawTool) -> Unit,
    selectedInkColor: Long,
    onInkColorChange: (Long) -> Unit,
    selectedStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    canvasStrokes: List<CanvasInkStroke>,
    onCanvasStrokesChange: (List<CanvasInkStroke>) -> Unit,
    pageInkStrokeCount: Int,
    onUndoPageInk: () -> Unit,
    isSideLayout: Boolean,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DrawControlHeader(
            selectedDrawTarget = selectedDrawTarget,
            onDrawTargetChange = onDrawTargetChange,
            selectedTool = selectedDrawTool,
            onToolChange = onDrawToolChange,
            canUndo = if (selectedDrawTarget == DrawTarget.Canvas) {
                canvasStrokes.isNotEmpty()
            } else {
                pageInkStrokeCount > 0
            },
            onUndo = {
                if (selectedDrawTarget == DrawTarget.Canvas) {
                    if (canvasStrokes.isNotEmpty()) {
                        onCanvasStrokesChange(canvasStrokes.dropLast(1))
                    }
                } else {
                    onUndoPageInk()
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        DrawColorSizeRow(
            selectedColor = selectedInkColor,
            onColorChange = onInkColorChange,
            selectedStrokeWidth = selectedStrokeWidth,
            onStrokeWidthChange = onStrokeWidthChange
        )

        if (selectedDrawTarget == DrawTarget.Canvas) {
            Spacer(modifier = Modifier.height(10.dp))
        }

        when (selectedDrawTarget) {
            DrawTarget.Canvas -> FreeDrawCanvas(
                strokes = canvasStrokes,
                onStrokesChange = onCanvasStrokesChange,
                selectedTool = selectedDrawTool,
                onToolChange = onDrawToolChange,
                selectedColor = selectedInkColor,
                onColorChange = onInkColorChange,
                selectedStrokeWidth = selectedStrokeWidth,
                onStrokeWidthChange = onStrokeWidthChange,
                fillAvailableHeight = isSideLayout || isExpanded,
                modifier = if (isSideLayout || isExpanded) Modifier.weight(1f) else Modifier
            )

            DrawTarget.Pdf -> Unit
        }
    }
}

@Composable
private fun FreeDrawCanvas(
    strokes: List<CanvasInkStroke>,
    onStrokesChange: (List<CanvasInkStroke>) -> Unit,
    selectedTool: WorkspaceDrawTool,
    onToolChange: (WorkspaceDrawTool) -> Unit,
    selectedColor: Long,
    onColorChange: (Long) -> Unit,
    selectedStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    fillAvailableHeight: Boolean,
    modifier: Modifier = Modifier
) {
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var canvasScale by remember { mutableFloatStateOf(INITIAL_CANVAS_SCALE) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    var hasInitializedCanvasViewport by remember { mutableStateOf(false) }
    var currentStrokePoints by remember { mutableStateOf<List<InkPoint>>(emptyList()) }
    var currentErasePoints by remember { mutableStateOf<List<InkPoint>>(emptyList()) }
    val latestStrokes by rememberUpdatedState(strokes)
    val latestSelectedColor by rememberUpdatedState(selectedColor)
    val latestSelectedStrokeWidth by rememberUpdatedState(selectedStrokeWidth)
    val latestOnStrokesChange by rememberUpdatedState(onStrokesChange)

    fun clampOffset(offset: Offset, scale: Float): Offset {
        val width = viewportSize.width.toFloat()
        val height = viewportSize.height.toFloat()

        if (width <= 0f || height <= 0f) {
            return offset
        }

        val scaledCanvasWidth = DRAW_CANVAS_WIDTH * scale
        val scaledCanvasHeight = DRAW_CANVAS_HEIGHT * scale
        val minX = min(0f, width - scaledCanvasWidth)
        val minY = min(0f, height - scaledCanvasHeight)
        val maxX = max(0f, width - scaledCanvasWidth)
        val maxY = max(0f, height - scaledCanvasHeight)

        return Offset(
            x = offset.x.coerceIn(minX, maxX),
            y = offset.y.coerceIn(minY, maxY)
        )
    }

    fun screenToCanvas(
        position: Offset,
        pressure: Float = 1f
    ): InkPoint {
        return InkPoint(
            x = ((position.x - canvasOffset.x) / canvasScale)
                .coerceIn(0f, DRAW_CANVAS_WIDTH),
            y = ((position.y - canvasOffset.y) / canvasScale)
                .coerceIn(0f, DRAW_CANVAS_HEIGHT),
            pressure = pressure.coerceIn(0f, 1f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (fillAvailableHeight) {
                    Modifier
                } else {
                    Modifier.height(178.dp)
                }
            )
                .background(
                    color = Color(0xFFF9F8F4),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .onSizeChanged { size ->
                    viewportSize = size
                    if (!hasInitializedCanvasViewport && size.width > 0 && size.height > 0) {
                        canvasScale = (
                            max(size.width, size.height).toFloat() /
                                DEFAULT_CANVAS_REGION_SIZE
                            ).coerceIn(MIN_CANVAS_SCALE, MAX_CANVAS_SCALE)
                        canvasOffset = Offset.Zero
                        hasInitializedCanvasViewport = true
                    }
                    canvasOffset = clampOffset(canvasOffset, canvasScale)
                }
                .pointerInput(selectedTool) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var activePointerId = down.id
                        var isStylusGesture = down.isStylusLikePointer()
                        var effectiveTool = if (down.isStylusEraserPointer()) {
                            WorkspaceDrawTool.Eraser
                        } else {
                            selectedTool
                        }
                        var gestureStrokePoints = if (effectiveTool == WorkspaceDrawTool.Pen || effectiveTool == WorkspaceDrawTool.Marker) {
                            listOf(screenToCanvas(down.position, down.pressure))
                        } else {
                            emptyList()
                        }
                        var erasePoints = if (effectiveTool == WorkspaceDrawTool.Eraser) {
                            listOf(screenToCanvas(down.position, down.pressure))
                        } else {
                            emptyList()
                        }
                        var isTransforming = false
                        var lastCentroid: Offset? = null
                        var lastDistance = 0f
                        var workingStrokes = latestStrokes

                        currentStrokePoints = gestureStrokePoints
                        currentErasePoints = erasePoints

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedChanges = event.changes.filter { change -> change.pressed }
                            val activeWasReleased = event.changes.any { change ->
                                change.id == activePointerId && !change.pressed
                            }

                            if (pressedChanges.isEmpty() || (isStylusGesture && activeWasReleased)) {
                                if (
                                    !isTransforming &&
                                    (effectiveTool == WorkspaceDrawTool.Pen || effectiveTool == WorkspaceDrawTool.Marker) &&
                                    gestureStrokePoints.size > 1
                                ) {
                                    latestOnStrokesChange(
                                        latestStrokes + CanvasInkStroke(
                                            id = UUID.randomUUID().toString(),
                                            tool = if (effectiveTool == WorkspaceDrawTool.Marker) {
                                                InkTool.Highlighter
                                            } else {
                                                InkTool.Pen
                                            },
                                            color = latestSelectedColor,
                                            width = latestSelectedStrokeWidth,
                                            points = gestureStrokePoints
                                        )
                                    )
                                } else if (
                                    !isTransforming &&
                                    effectiveTool == WorkspaceDrawTool.Eraser &&
                                    erasePoints.isNotEmpty()
                                ) {
                                    latestOnStrokesChange(workingStrokes)
                                }

                                currentStrokePoints = emptyList()
                                currentErasePoints = emptyList()
                                break
                            }

                            val change = pressedChanges.preferredInkChange(activePointerId) ?: continue

                            if (!isStylusGesture && change.isStylusLikePointer()) {
                                activePointerId = change.id
                                isStylusGesture = true
                                effectiveTool = if (change.isStylusEraserPointer()) {
                                    WorkspaceDrawTool.Eraser
                                } else {
                                    selectedTool
                                }
                                gestureStrokePoints =
                                    if (effectiveTool == WorkspaceDrawTool.Pen || effectiveTool == WorkspaceDrawTool.Marker) {
                                        listOf(screenToCanvas(change.position, change.pressure))
                                    } else {
                                        emptyList()
                                    }
                                erasePoints = if (effectiveTool == WorkspaceDrawTool.Eraser) {
                                    listOf(screenToCanvas(change.position, change.pressure))
                                } else {
                                    emptyList()
                                }
                                currentStrokePoints = gestureStrokePoints
                                currentErasePoints = erasePoints
                                isTransforming = false
                                lastCentroid = null
                                lastDistance = 0f
                            } else if (change.isStylusEraserPointer()) {
                                effectiveTool = WorkspaceDrawTool.Eraser
                                currentStrokePoints = emptyList()
                                currentErasePoints = erasePoints
                            }

                            if (isStylusGesture && !change.isStylusLikePointer()) {
                                pressedChanges.forEach { pressedChange -> pressedChange.consume() }
                                continue
                            }

                            if (!isStylusGesture && pressedChanges.size >= 2) {
                                isTransforming = true
                                gestureStrokePoints = emptyList()
                                currentStrokePoints = emptyList()
                                currentErasePoints = emptyList()

                                val centroid = centroidOf(pressedChanges)
                                val distance = averageDistanceFromCentroid(
                                    changes = pressedChanges,
                                    centroid = centroid
                                )
                                val previousCentroid = lastCentroid

                                if (previousCentroid != null) {
                                    val oldScale = canvasScale
                                    val zoomChange = if (lastDistance > 0f && distance > 0f) {
                                        distance / lastDistance
                                    } else {
                                        1f
                                    }
                                    val newScale = (oldScale * zoomChange).coerceIn(
                                        MIN_CANVAS_SCALE,
                                        MAX_CANVAS_SCALE
                                    )
                                    val contentX = (previousCentroid.x - canvasOffset.x) / oldScale
                                    val contentY = (previousCentroid.y - canvasOffset.y) / oldScale

                                    canvasScale = newScale
                                    canvasOffset = clampOffset(
                                        Offset(
                                            x = centroid.x - (contentX * newScale),
                                            y = centroid.y - (contentY * newScale)
                                        ),
                                        newScale
                                    )
                                }

                                lastCentroid = centroid
                                lastDistance = distance
                                pressedChanges.forEach { change -> change.consume() }
                            } else {
                                lastCentroid = null
                                lastDistance = 0f

                                if (isTransforming || effectiveTool == WorkspaceDrawTool.Move) {
                                    canvasOffset = clampOffset(
                                        canvasOffset + change.positionChange(),
                                        canvasScale
                                    )
                                } else if (effectiveTool == WorkspaceDrawTool.Eraser) {
                                    erasePoints = erasePoints.appendInkPointIfFarEnough(
                                        point = screenToCanvas(change.position, change.pressure),
                                        minDistance = CANVAS_ERASER_SAMPLE_DISTANCE
                                    )
                                    currentErasePoints = erasePoints
                                    workingStrokes = workingStrokes.eraseCanvasStrokes(erasePoints)
                                    latestOnStrokesChange(workingStrokes)
                                } else {
                                    gestureStrokePoints = gestureStrokePoints.appendInkPointIfFarEnough(
                                        point = screenToCanvas(change.position, change.pressure),
                                        minDistance = (latestSelectedStrokeWidth * 0.08f)
                                            .coerceAtLeast(MIN_CANVAS_INK_POINT_DISTANCE)
                                    )
                                    currentStrokePoints = gestureStrokePoints
                                }

                                pressedChanges.forEach { pressedChange ->
                                    if (isStylusGesture || pressedChange.id == activePointerId) {
                                        pressedChange.consume()
                                    }
                                }
                            }
                        }
                    }
                }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCanvasGrid(
                canvasOffset = canvasOffset,
                canvasScale = canvasScale
            )

            strokes.forEach { stroke ->
                drawInkStroke(
                    points = stroke.points,
                    color = argbColor(stroke.color),
                    width = stroke.width,
                    canvasOffset = canvasOffset,
                    canvasScale = canvasScale
                )
            }

            if (currentStrokePoints.isNotEmpty()) {
                drawInkStroke(
                    points = currentStrokePoints,
                    color = argbColor(selectedColor),
                    width = selectedStrokeWidth,
                    canvasOffset = canvasOffset,
                    canvasScale = canvasScale
                )
            }

            if (currentErasePoints.isNotEmpty()) {
                drawEraserPreview(
                    points = currentErasePoints,
                    canvasOffset = canvasOffset,
                    canvasScale = canvasScale
                )
            }
        }
    }
}

@Composable
private fun DrawControlHeader(
    selectedDrawTarget: DrawTarget,
    onDrawTargetChange: (DrawTarget) -> Unit,
    selectedTool: WorkspaceDrawTool,
    onToolChange: (WorkspaceDrawTool) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawTargetButton(
            target = DrawTarget.Canvas,
            selected = selectedDrawTarget == DrawTarget.Canvas,
            onClick = {
                onDrawTargetChange(DrawTarget.Canvas)
            }
        )

        DrawTargetButton(
            target = DrawTarget.Pdf,
            selected = selectedDrawTarget == DrawTarget.Pdf,
            onClick = {
                onDrawTargetChange(DrawTarget.Pdf)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        CompactDrawToolButton(
            tool = WorkspaceDrawTool.Pen,
            selected = selectedTool == WorkspaceDrawTool.Pen,
            onClick = {
                onToolChange(WorkspaceDrawTool.Pen)
            }
        )

        CompactDrawToolButton(
            tool = WorkspaceDrawTool.Marker,
            selected = selectedTool == WorkspaceDrawTool.Marker,
            onClick = {
                onToolChange(WorkspaceDrawTool.Marker)
            }
        )

        CompactDrawToolButton(
            tool = WorkspaceDrawTool.Move,
            selected = selectedTool == WorkspaceDrawTool.Move,
            onClick = {
                onToolChange(WorkspaceDrawTool.Move)
            }
        )

        CompactDrawToolButton(
            tool = WorkspaceDrawTool.Eraser,
            selected = selectedTool == WorkspaceDrawTool.Eraser,
            onClick = {
                onToolChange(WorkspaceDrawTool.Eraser)
            }
        )

        DrawIconAction(
            icon = DrawActionIcon.Undo,
            enabled = canUndo,
            onClick = onUndo
        )
    }
}

@Composable
private fun DrawColorSizeRow(
    selectedColor: Long,
    onColorChange: (Long) -> Unit,
    selectedStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DRAW_COLORS.forEach { color ->
            ColorSwatch(
                color = color,
                selected = color == selectedColor,
                onClick = {
                    onColorChange(color)
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        StrokeSizeSelector(
            selectedStrokeWidth = selectedStrokeWidth,
            onStrokeWidthChange = onStrokeWidthChange
        )
    }
}

@Composable
private fun CompactDrawToolButton(
    tool: WorkspaceDrawTool,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = CarrotColors.Accent
    val iconColor = if (selected) accent else Color(0xFF3A424A)

    Box(
        modifier = Modifier
            .size(36.dp)
            .semantics {
                contentDescription = tool.accessibleLabel()
            }
            .background(
                color = if (selected) accent.copy(alpha = 0.12f) else Color.Transparent,
                shape = RoundedCornerShape(999.dp)
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) accent else Color.Transparent,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            drawToolIcon(
                tool = tool,
                color = iconColor
            )
        }
    }
}

@Composable
private fun PdfDrawPanel(
    selectedTool: WorkspaceDrawTool,
    onToolChange: (WorkspaceDrawTool) -> Unit,
    selectedColor: Long,
    onColorChange: (Long) -> Unit,
    selectedStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    strokeCount: Int,
    onUndo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        DrawToolbar(
            selectedTool = selectedTool,
            onToolChange = { tool ->
                onToolChange(tool)
            },
            selectedColor = selectedColor,
            onColorChange = onColorChange,
            selectedStrokeWidth = selectedStrokeWidth,
            onStrokeWidthChange = onStrokeWidthChange,
            canUndo = strokeCount > 0,
            onUndo = onUndo,
            showMove = true,
            showUndo = false
        )
    }
}

@Composable
private fun DrawToolbar(
    selectedTool: WorkspaceDrawTool,
    onToolChange: (WorkspaceDrawTool) -> Unit,
    selectedColor: Long,
    onColorChange: (Long) -> Unit,
    selectedStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    showMove: Boolean = true,
    showUndo: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawToolButton(
                    tool = WorkspaceDrawTool.Pen,
                    selected = selectedTool == WorkspaceDrawTool.Pen,
                    onClick = { onToolChange(WorkspaceDrawTool.Pen) }
                )
                if (showMove) {
                    DrawToolButton(
                        tool = WorkspaceDrawTool.Move,
                        selected = selectedTool == WorkspaceDrawTool.Move,
                        onClick = { onToolChange(WorkspaceDrawTool.Move) }
                    )
                }
                DrawToolButton(
                    tool = WorkspaceDrawTool.Eraser,
                    selected = selectedTool == WorkspaceDrawTool.Eraser,
                    onClick = { onToolChange(WorkspaceDrawTool.Eraser) }
                )
            }

            if (showUndo) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawIconAction(
                        icon = DrawActionIcon.Undo,
                        enabled = canUndo,
                        onClick = onUndo
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(42.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DRAW_COLORS.forEach { color ->
                ColorSwatch(
                    color = color,
                    selected = color == selectedColor,
                    onClick = {
                        onColorChange(color)
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            StrokeSizeSelector(
                selectedStrokeWidth = selectedStrokeWidth,
                onStrokeWidthChange = onStrokeWidthChange
            )
        }
    }
}

@Composable
private fun DrawToolButton(
    tool: WorkspaceDrawTool,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = CarrotColors.Accent
    val iconColor = if (selected) accent else Color(0xFF3A424A)

    Column(
        modifier = Modifier
            .semantics {
                contentDescription = tool.accessibleLabel()
            }
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = if (selected) accent.copy(alpha = 0.12f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (selected) 1.dp else 0.dp,
                    color = if (selected) accent else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                drawToolIcon(
                    tool = tool,
                    color = iconColor
                )
            }
        }

        Text(
            text = when (tool) {
                WorkspaceDrawTool.Pen -> "Pen"
                WorkspaceDrawTool.Marker -> "Marker"
                WorkspaceDrawTool.Move -> "Move"
                WorkspaceDrawTool.Eraser -> "Eraser"
            },
            color = if (selected) accent else Color(0xFF4F5963),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun DrawIconAction(
    icon: DrawActionIcon,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .semantics {
                contentDescription = icon.accessibleLabel()
            }
            .background(
                color = Color.White.copy(alpha = if (enabled) 0.55f else 0.24f),
                shape = RoundedCornerShape(999.dp)
            )
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.07f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            drawActionIcon(
                icon = icon,
                color = if (enabled) Color(0xFF3A424A) else Color(0xFF9AA1A9)
            )
        }
    }
}

@Composable
private fun DrawChip(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = when {
                    selected -> CarrotColors.AccentSoft
                    else -> Color.White.copy(alpha = 0.06f)
                },
                shape = RoundedCornerShape(999.dp)
            )
            .border(
                width = 1.dp,
                color = when {
                    selected -> CarrotColors.Accent
                    else -> Color.White.copy(alpha = 0.08f)
                },
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = when {
                !enabled -> CarrotColors.TextMuted
                selected -> CarrotColors.Accent
                else -> CarrotColors.TextSecondary
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Long,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(
                color = argbColor(color),
                shape = CircleShape
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
private fun StrokeSizeSelector(
    selectedStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        STROKE_WIDTHS.forEach { width ->
            val selected = width == selectedStrokeWidth
            val lineColor = if (selected) CarrotColors.Accent else Color(0xFF55606A)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        color = if (selected) CarrotColors.Accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) CarrotColors.Accent else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onStrokeWidthChange(width) },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawLine(
                        color = lineColor,
                        start = Offset(2.dp.toPx(), size.height / 2f),
                        end = Offset(size.width - 2.dp.toPx(), size.height / 2f),
                        strokeWidth = (width / 12f).coerceIn(1.6f, 4.6f),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceDragHandle(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 18.dp)
            .pointerInput(isExpanded) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalDragY = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { change -> change.pressed }

                        if (pressedChanges.isEmpty()) {
                            if (abs(totalDragY) < WORKSPACE_HANDLE_TAP_SLOP_PX) {
                                onExpandedChange(!isExpanded)
                            } else {
                                onExpandedChange(totalDragY < 0f)
                            }
                            break
                        }

                        pressedChanges.forEach { change ->
                            totalDragY += change.positionChange().y
                            change.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 42.dp, height = 4.dp)
                .background(
                    color = Color.Black.copy(alpha = if (isExpanded) 0.36f else 0.28f),
                    shape = RoundedCornerShape(999.dp)
                )
        )
    }
}

@Composable
private fun WorkspaceCollapseButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val stroke = 1.8.dp.toPx()
            drawLine(
                color = Color(0xFF2D343B),
                start = Offset(5.dp.toPx(), 8.dp.toPx()),
                end = Offset(10.dp.toPx(), 13.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF2D343B),
                start = Offset(15.dp.toPx(), 8.dp.toPx()),
                end = Offset(10.dp.toPx(), 13.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun WorkspaceModeButton(
    mode: WorkspaceMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconColor = if (selected) CarrotColors.Accent else Color(0xFF2D343B)
    val underlineColor = if (selected) CarrotColors.Accent else Color.Transparent

    Column(
        modifier = Modifier
            .semantics {
                contentDescription = mode.accessibleLabel()
            }
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            drawWorkspaceModeIcon(
                mode = mode,
                color = iconColor
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 2.dp)
                .background(
                    color = underlineColor,
                    shape = RoundedCornerShape(999.dp)
                )
        )
    }
}

@Composable
private fun DrawTargetButton(
    target: DrawTarget,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconColor = if (selected) CarrotColors.Accent else Color(0xFF3A424A)

    Box(
        modifier = Modifier
            .size(38.dp)
            .semantics {
                contentDescription = target.accessibleLabel()
            }
            .background(
                color = if (selected) CarrotColors.Accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.42f),
                shape = RoundedCornerShape(999.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) CarrotColors.Accent else Color.Black.copy(alpha = 0.08f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            drawDrawTargetIcon(
                target = target,
                color = iconColor
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCanvasGrid(
    canvasOffset: Offset,
    canvasScale: Float
) {
    if (canvasScale <= 0f) {
        return
    }

    var gridStep = CANVAS_GRID_BASE_STEP
    while (gridStep * canvasScale < CANVAS_GRID_MIN_SCREEN_STEP) {
        gridStep *= 2f
    }

    val startX = (floor((-canvasOffset.x / canvasScale) / gridStep) * gridStep)
        .coerceAtLeast(0f)
    val endX = (ceil(((size.width - canvasOffset.x) / canvasScale) / gridStep) * gridStep)
        .coerceAtMost(DRAW_CANVAS_WIDTH)
    val startY = (floor((-canvasOffset.y / canvasScale) / gridStep) * gridStep)
        .coerceAtLeast(0f)
    val endY = (ceil(((size.height - canvasOffset.y) / canvasScale) / gridStep) * gridStep)
        .coerceAtMost(DRAW_CANVAS_HEIGHT)

    var x = startX
    var column = (x / gridStep).toInt()
    while (x <= endX) {
        val major = column % CANVAS_GRID_MAJOR_INTERVAL == 0
        val screenX = x * canvasScale + canvasOffset.x
        drawLine(
            color = Color(0xFF59636D).copy(alpha = if (major) 0.20f else 0.09f),
            start = Offset(screenX, 0f),
            end = Offset(screenX, size.height),
            strokeWidth = if (major) 1.15f else 0.7f
        )
        x += gridStep
        column += 1
    }

    var y = startY
    var row = (y / gridStep).toInt()
    while (y <= endY) {
        val major = row % CANVAS_GRID_MAJOR_INTERVAL == 0
        val screenY = y * canvasScale + canvasOffset.y
        drawLine(
            color = Color(0xFF59636D).copy(alpha = if (major) 0.20f else 0.09f),
            start = Offset(0f, screenY),
            end = Offset(size.width, screenY),
            strokeWidth = if (major) 1.15f else 0.7f
        )
        y += gridStep
        row += 1
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEraserPreview(
    points: List<InkPoint>,
    canvasOffset: Offset,
    canvasScale: Float
) {
    val lastPoint = points.lastOrNull() ?: return
    val center = Offset(
        x = lastPoint.x * canvasScale + canvasOffset.x,
        y = lastPoint.y * canvasScale + canvasOffset.y
    )
    val radius = (CANVAS_ERASER_RADIUS * canvasScale).coerceAtLeast(10f)

    drawCircle(
        color = Color.White.copy(alpha = 0.38f),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color(0xFF59636D).copy(alpha = 0.62f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.4.dp.toPx())
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInkStroke(
    points: List<InkPoint>,
    color: Color,
    width: Float,
    canvasOffset: Offset,
    canvasScale: Float
) {
    if (points.isEmpty()) {
        return
    }

    if (points.size == 1) {
        val point = points.first()
        drawCircle(
            color = color,
            radius = (width * canvasScale) / 2f,
            center = Offset(
                x = point.x * canvasScale + canvasOffset.x,
                y = point.y * canvasScale + canvasOffset.y
            )
        )
        return
    }

    val path = smoothedInkPath(
        points = points,
        transform = { point ->
            Offset(
                x = point.x * canvasScale + canvasOffset.x,
                y = point.y * canvasScale + canvasOffset.y
            )
        }
    )

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = width * canvasScale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun List<CanvasInkStroke>.eraseCanvasStrokes(
    erasePoints: List<InkPoint>
): List<CanvasInkStroke> {
    if (erasePoints.isEmpty()) {
        return this
    }

    return filterNot { stroke ->
        stroke.points.isNearInkPath(
            testPoints = erasePoints,
            radius = CANVAS_ERASER_RADIUS
        )
    }
}

private fun List<InkPoint>.appendInkPointIfFarEnough(
    point: InkPoint,
    minDistance: Float
): List<InkPoint> {
    val lastPoint = lastOrNull() ?: return this + point
    val dx = point.x - lastPoint.x
    val dy = point.y - lastPoint.y

    return if (dx * dx + dy * dy >= minDistance * minDistance) {
        this + point
    } else {
        this
    }
}

private fun List<InkPoint>.isNearInkPath(
    testPoints: List<InkPoint>,
    radius: Float
): Boolean {
    if (isEmpty() || testPoints.isEmpty()) {
        return false
    }

    val radiusSquared = radius * radius

    return testPoints.any { testPoint ->
        any { point ->
            point.distanceSquaredTo(testPoint) <= radiusSquared
        } || zipWithNext().any { (start, end) ->
            testPoint.distanceSquaredToSegment(start, end) <= radiusSquared
        }
    }
}

private fun smoothedInkPath(
    points: List<InkPoint>,
    transform: (InkPoint) -> Offset
): Path {
    val transformed = points.map(transform)

    return Path().apply {
        val first = transformed.first()
        moveTo(first.x, first.y)

        if (transformed.size == 2) {
            val last = transformed.last()
            lineTo(last.x, last.y)
            return@apply
        }

        for (index in 1 until transformed.lastIndex) {
            val current = transformed[index]
            val next = transformed[index + 1]
            val mid = Offset(
                x = (current.x + next.x) / 2f,
                y = (current.y + next.y) / 2f
            )
            quadraticTo(current.x, current.y, mid.x, mid.y)
        }

        val last = transformed.last()
        lineTo(last.x, last.y)
    }
}

private fun InkPoint.distanceSquaredTo(other: InkPoint): Float {
    val dx = x - other.x
    val dy = y - other.y

    return dx * dx + dy * dy
}

private fun InkPoint.distanceSquaredToSegment(
    start: InkPoint,
    end: InkPoint
): Float {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val lengthSquared = dx * dx + dy * dy

    if (lengthSquared <= 0.000001f) {
        return distanceSquaredTo(start)
    }

    val t = (((x - start.x) * dx + (y - start.y) * dy) / lengthSquared)
        .coerceIn(0f, 1f)
    val projection = InkPoint(
        x = start.x + t * dx,
        y = start.y + t * dy
    )

    return distanceSquaredTo(projection)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawToolIcon(
    tool: WorkspaceDrawTool,
    color: Color
) {
    val stroke = 1.9.dp.toPx()

    when (tool) {
        WorkspaceDrawTool.Pen -> {
            val penBody = Path().apply {
                moveTo(6.dp.toPx(), 16.dp.toPx())
                lineTo(14.5.dp.toPx(), 7.5.dp.toPx())
                lineTo(17.5.dp.toPx(), 10.5.dp.toPx())
                lineTo(9.dp.toPx(), 19.dp.toPx())
                close()
            }
            drawPath(
                path = penBody,
                color = color,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawLine(
                color = color,
                start = Offset(14.dp.toPx(), 8.dp.toPx()),
                end = Offset(17.dp.toPx(), 5.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(17.dp.toPx(), 5.dp.toPx()),
                end = Offset(20.dp.toPx(), 8.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(6.dp.toPx(), 19.dp.toPx()),
                end = Offset(10.dp.toPx(), 18.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        WorkspaceDrawTool.Marker -> {
            val markerBody = Path().apply {
                moveTo(6.dp.toPx(), 14.dp.toPx())
                lineTo(14.dp.toPx(), 6.dp.toPx())
                lineTo(18.dp.toPx(), 10.dp.toPx())
                lineTo(10.dp.toPx(), 18.dp.toPx())
                close()
            }
            drawPath(
                path = markerBody,
                color = color,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawLine(
                color = color,
                start = Offset(7.dp.toPx(), 14.dp.toPx()),
                end = Offset(10.dp.toPx(), 17.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color.copy(alpha = 0.55f),
                start = Offset(4.dp.toPx(), 20.dp.toPx()),
                end = Offset(14.dp.toPx(), 20.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        WorkspaceDrawTool.Move -> {
            drawLine(color, Offset(11.dp.toPx(), 4.dp.toPx()), Offset(11.dp.toPx(), 18.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(4.dp.toPx(), 11.dp.toPx()), Offset(18.dp.toPx(), 11.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(11.dp.toPx(), 4.dp.toPx()), Offset(8.dp.toPx(), 7.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(11.dp.toPx(), 4.dp.toPx()), Offset(14.dp.toPx(), 7.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(11.dp.toPx(), 18.dp.toPx()), Offset(8.dp.toPx(), 15.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(11.dp.toPx(), 18.dp.toPx()), Offset(14.dp.toPx(), 15.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(4.dp.toPx(), 11.dp.toPx()), Offset(7.dp.toPx(), 8.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(4.dp.toPx(), 11.dp.toPx()), Offset(7.dp.toPx(), 14.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(18.dp.toPx(), 11.dp.toPx()), Offset(15.dp.toPx(), 8.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(18.dp.toPx(), 11.dp.toPx()), Offset(15.dp.toPx(), 14.dp.toPx()), stroke, StrokeCap.Round)
        }

        WorkspaceDrawTool.Eraser -> {
            val eraser = Path().apply {
                moveTo(5.dp.toPx(), 14.dp.toPx())
                lineTo(12.dp.toPx(), 7.dp.toPx())
                lineTo(18.dp.toPx(), 13.dp.toPx())
                lineTo(11.dp.toPx(), 20.dp.toPx())
                close()
            }
            drawPath(
                path = eraser,
                color = color,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawLine(
                color = color,
                start = Offset(8.dp.toPx(), 11.dp.toPx()),
                end = Offset(14.dp.toPx(), 17.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(color, Offset(11.dp.toPx(), 20.dp.toPx()), Offset(19.dp.toPx(), 20.dp.toPx()), stroke, StrokeCap.Round)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWorkspaceModeIcon(
    mode: WorkspaceMode,
    color: Color
) {
    val stroke = 1.8.dp.toPx()

    when (mode) {
        WorkspaceMode.Notes -> {
            val note = Path().apply {
                moveTo(5.dp.toPx(), 3.dp.toPx())
                lineTo(15.dp.toPx(), 3.dp.toPx())
                lineTo(19.dp.toPx(), 7.dp.toPx())
                lineTo(19.dp.toPx(), 21.dp.toPx())
                lineTo(5.dp.toPx(), 21.dp.toPx())
                close()
            }
            drawPath(note, color, style = Stroke(width = stroke, join = StrokeJoin.Round))
            drawLine(color, Offset(15.dp.toPx(), 3.dp.toPx()), Offset(15.dp.toPx(), 7.dp.toPx()), stroke)
            drawLine(color, Offset(15.dp.toPx(), 7.dp.toPx()), Offset(19.dp.toPx(), 7.dp.toPx()), stroke)
            drawLine(color, Offset(8.dp.toPx(), 11.dp.toPx()), Offset(16.dp.toPx(), 11.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(8.dp.toPx(), 15.dp.toPx()), Offset(16.dp.toPx(), 15.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(8.dp.toPx(), 19.dp.toPx()), Offset(13.dp.toPx(), 19.dp.toPx()), stroke, StrokeCap.Round)
        }

        WorkspaceMode.Draw -> {
            drawLine(color, Offset(6.dp.toPx(), 16.dp.toPx()), Offset(16.dp.toPx(), 6.dp.toPx()), 3.2.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(15.dp.toPx(), 5.dp.toPx()), Offset(19.dp.toPx(), 9.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(5.dp.toPx(), 19.dp.toPx()), Offset(9.dp.toPx(), 18.dp.toPx()), stroke, StrokeCap.Round)
            drawArc(
                color = color.copy(alpha = 0.72f),
                startAngle = 185f,
                sweepAngle = 165f,
                useCenter = false,
                topLeft = Offset(7.dp.toPx(), 15.dp.toPx()),
                size = Size(12.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDrawTargetIcon(
    target: DrawTarget,
    color: Color
) {
    val stroke = 1.7.dp.toPx()

    when (target) {
        DrawTarget.Canvas -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(3.dp.toPx(), 4.dp.toPx()),
                size = Size(14.dp.toPx(), 13.dp.toPx()),
                style = Stroke(width = stroke),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
            drawCircle(color, 1.4.dp.toPx(), Offset(7.dp.toPx(), 8.dp.toPx()), style = Stroke(width = 1.2.dp.toPx()))
            val landscape = Path().apply {
                moveTo(5.dp.toPx(), 15.dp.toPx())
                lineTo(9.dp.toPx(), 11.dp.toPx())
                lineTo(12.dp.toPx(), 14.dp.toPx())
                lineTo(15.dp.toPx(), 10.dp.toPx())
            }
            drawPath(landscape, color, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(color, Offset(6.dp.toPx(), 20.dp.toPx()), Offset(14.dp.toPx(), 20.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(8.dp.toPx(), 17.dp.toPx()), Offset(6.dp.toPx(), 20.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(12.dp.toPx(), 17.dp.toPx()), Offset(14.dp.toPx(), 20.dp.toPx()), stroke, StrokeCap.Round)
        }

        DrawTarget.Pdf -> {
            val document = Path().apply {
                moveTo(4.dp.toPx(), 2.dp.toPx())
                lineTo(13.dp.toPx(), 2.dp.toPx())
                lineTo(18.dp.toPx(), 7.dp.toPx())
                lineTo(18.dp.toPx(), 19.dp.toPx())
                lineTo(4.dp.toPx(), 19.dp.toPx())
                close()
            }
            drawPath(document, color, style = Stroke(width = stroke, join = StrokeJoin.Round))
            drawLine(color, Offset(13.dp.toPx(), 2.dp.toPx()), Offset(13.dp.toPx(), 7.dp.toPx()), stroke)
            drawLine(color, Offset(13.dp.toPx(), 7.dp.toPx()), Offset(18.dp.toPx(), 7.dp.toPx()), stroke)
            drawLine(color, Offset(7.dp.toPx(), 11.dp.toPx()), Offset(15.dp.toPx(), 11.dp.toPx()), 1.2.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(7.dp.toPx(), 14.dp.toPx()), Offset(15.dp.toPx(), 14.dp.toPx()), 1.2.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(7.dp.toPx(), 17.dp.toPx()), Offset(11.dp.toPx(), 17.dp.toPx()), 1.2.dp.toPx(), StrokeCap.Round)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawActionIcon(
    icon: DrawActionIcon,
    color: Color
) {
    val stroke = 1.8.dp.toPx()

    when (icon) {
        DrawActionIcon.Undo -> {
            drawArc(
                color = color,
                startAngle = 205f,
                sweepAngle = 245f,
                useCenter = false,
                topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
                size = Size(13.dp.toPx(), 13.dp.toPx()),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawLine(color, Offset(5.dp.toPx(), 11.dp.toPx()), Offset(3.dp.toPx(), 7.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(color, Offset(5.dp.toPx(), 11.dp.toPx()), Offset(9.dp.toPx(), 10.dp.toPx()), stroke, StrokeCap.Round)
        }
    }
}

private fun centroidOf(
    changes: List<PointerInputChange>
): Offset {
    if (changes.isEmpty()) {
        return Offset.Zero
    }

    var x = 0f
    var y = 0f

    changes.forEach { change ->
        x += change.position.x
        y += change.position.y
    }

    return Offset(
        x = x / changes.size,
        y = y / changes.size
    )
}

private fun averageDistanceFromCentroid(
    changes: List<PointerInputChange>,
    centroid: Offset
): Float {
    if (changes.isEmpty()) {
        return 0f
    }

    var total = 0f

    changes.forEach { change ->
        total += (change.position - centroid).getDistance()
    }

    return total / changes.size
}

private fun argbColor(value: Long): Color {
    return Color(value.toInt())
}

private fun WorkspaceDrawTool.accessibleLabel(): String {
    return when (this) {
        WorkspaceDrawTool.Pen -> "Pen tool"
        WorkspaceDrawTool.Marker -> "Text marker tool"
        WorkspaceDrawTool.Move -> "Move canvas or PDF"
        WorkspaceDrawTool.Eraser -> "Eraser tool"
    }
}

private fun WorkspaceMode.accessibleLabel(): String {
    return when (this) {
        WorkspaceMode.Notes -> "Open notes"
        WorkspaceMode.Draw -> "Open drawing tools"
    }
}

private fun DrawTarget.accessibleLabel(): String {
    return when (this) {
        DrawTarget.Canvas -> "Draw on separate canvas"
        DrawTarget.Pdf -> "Draw directly on PDF"
    }
}

private fun DrawActionIcon.accessibleLabel(): String {
    return when (this) {
        DrawActionIcon.Undo -> "Undo last drawing"
    }
}

private enum class DrawActionIcon {
    Undo
}

private val DRAW_COLORS = listOf(
    0xFFFF8A3DL,
    0xFFFF6B78L,
    0xFFFFD966L,
    0xFF65D58AL,
    0xFF6EA1FFL,
    0xFFB58CFFL,
    0xFF202329L
)

private val STROKE_WIDTHS = listOf(22f, 34f, 48f)

private const val DRAW_CANVAS_WIDTH = DEFAULT_CANVAS_WIDTH
private const val DRAW_CANVAS_HEIGHT = DEFAULT_CANVAS_HEIGHT
private const val INITIAL_CANVAS_SCALE = 0.05f
private const val MIN_CANVAS_SCALE = 0.02f
private const val MAX_CANVAS_SCALE = 0.9f
private const val DEFAULT_CANVAS_REGION_SIZE = 2_000f
private const val MIN_CANVAS_INK_POINT_DISTANCE = 2f
private const val CANVAS_ERASER_SAMPLE_DISTANCE = 24f
private const val CANVAS_ERASER_RADIUS = 120f
private const val CANVAS_GRID_BASE_STEP = 120f
private const val CANVAS_GRID_MIN_SCREEN_STEP = 18f
private const val CANVAS_GRID_MAJOR_INTERVAL = 5
private const val WORKSPACE_HANDLE_TAP_SLOP_PX = 18f
