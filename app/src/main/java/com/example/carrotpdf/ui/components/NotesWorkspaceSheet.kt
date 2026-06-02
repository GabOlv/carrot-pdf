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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.workspace.CanvasInkStroke
import com.example.carrotpdf.workspace.InkPoint
import com.example.carrotpdf.workspace.InkTool
import java.util.UUID
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
    canvasStrokes: List<CanvasInkStroke>,
    onCanvasStrokesChange: (List<CanvasInkStroke>) -> Unit,
    pageInkStrokeCount: Int,
    onUndoPageInk: () -> Unit,
    onCollapse: () -> Unit,
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
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.08f),
                    shape = sheetShape
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (!isSideLayout) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 42.dp, height = 4.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(999.dp)
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                WorkspaceTabLabel(
                    text = "Notes",
                    selected = selectedMode == WorkspaceMode.Notes,
                    onClick = {
                        onModeChange(WorkspaceMode.Notes)
                    }
                )

                Spacer(modifier = Modifier.width(36.dp))

                WorkspaceTabLabel(
                    text = "Draw",
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
                    isSideLayout = isSideLayout
                )

                WorkspaceMode.Draw -> DrawWorkspace(
                    selectedDrawTarget = selectedDrawTarget,
                    onDrawTargetChange = onDrawTargetChange,
                    selectedDrawTool = selectedDrawTool,
                    onDrawToolChange = onDrawToolChange,
                    selectedInkColor = selectedInkColor,
                    onInkColorChange = onInkColorChange,
                    canvasStrokes = canvasStrokes,
                    onCanvasStrokesChange = onCanvasStrokesChange,
                    pageInkStrokeCount = pageInkStrokeCount,
                    onUndoPageInk = onUndoPageInk,
                    isSideLayout = isSideLayout
                )
            }
        }
    }
}

@Composable
private fun NotesEditor(
    notesText: String,
    onNotesChange: (String) -> Unit,
    isSideLayout: Boolean
) {
    BasicTextField(
        value = notesText,
        onValueChange = onNotesChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = if (isSideLayout) 260.dp else 150.dp,
                max = if (isSideLayout) 720.dp else 260.dp
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .verticalScroll(rememberScrollState())
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

@Composable
private fun DrawWorkspace(
    selectedDrawTarget: DrawTarget,
    onDrawTargetChange: (DrawTarget) -> Unit,
    selectedDrawTool: WorkspaceDrawTool,
    onDrawToolChange: (WorkspaceDrawTool) -> Unit,
    selectedInkColor: Long,
    onInkColorChange: (Long) -> Unit,
    canvasStrokes: List<CanvasInkStroke>,
    onCanvasStrokesChange: (List<CanvasInkStroke>) -> Unit,
    pageInkStrokeCount: Int,
    onUndoPageInk: () -> Unit,
    isSideLayout: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawChip(
                text = "Canvas",
                selected = selectedDrawTarget == DrawTarget.Canvas,
                onClick = {
                    onDrawTargetChange(DrawTarget.Canvas)
                }
            )

            Spacer(modifier = Modifier.width(10.dp))

            DrawChip(
                text = "PDF",
                selected = selectedDrawTarget == DrawTarget.Pdf,
                onClick = {
                    onDrawTargetChange(DrawTarget.Pdf)
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedDrawTarget) {
            DrawTarget.Canvas -> FreeDrawCanvas(
                strokes = canvasStrokes,
                onStrokesChange = onCanvasStrokesChange,
                selectedTool = selectedDrawTool,
                onToolChange = onDrawToolChange,
                selectedColor = selectedInkColor,
                onColorChange = onInkColorChange,
                isSideLayout = isSideLayout
            )

            DrawTarget.Pdf -> PdfDrawPanel(
                selectedTool = selectedDrawTool.takeIf { tool -> tool != WorkspaceDrawTool.Move }
                    ?: WorkspaceDrawTool.Pen,
                onToolChange = onDrawToolChange,
                selectedColor = selectedInkColor,
                onColorChange = onInkColorChange,
                strokeCount = pageInkStrokeCount,
                onUndo = onUndoPageInk
            )
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
    isSideLayout: Boolean
) {
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var canvasScale by remember { mutableFloatStateOf(INITIAL_CANVAS_SCALE) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    var currentStrokePoints by remember { mutableStateOf<List<InkPoint>>(emptyList()) }
    val latestStrokes by rememberUpdatedState(strokes)
    val latestSelectedColor by rememberUpdatedState(selectedColor)
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

    fun screenToCanvas(position: Offset): InkPoint {
        return InkPoint(
            x = ((position.x - canvasOffset.x) / canvasScale)
                .coerceIn(0f, DRAW_CANVAS_WIDTH),
            y = ((position.y - canvasOffset.y) / canvasScale)
                .coerceIn(0f, DRAW_CANVAS_HEIGHT)
        )
    }

    Column {
        DrawToolbar(
            selectedTool = selectedTool,
            onToolChange = { tool ->
                onToolChange(tool)
                currentStrokePoints = emptyList()
            },
            selectedColor = selectedColor,
            onColorChange = { color ->
                onColorChange(color)
            },
            canUndo = strokes.isNotEmpty(),
            onUndo = {
                if (strokes.isNotEmpty()) {
                    onStrokesChange(strokes.dropLast(1))
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isSideLayout) 360.dp else 260.dp)
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
                    canvasOffset = clampOffset(canvasOffset, canvasScale)
                }
                .pointerInput(selectedTool) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var gestureStrokePoints = if (selectedTool == WorkspaceDrawTool.Pen) {
                            listOf(screenToCanvas(down.position))
                        } else {
                            emptyList()
                        }
                        var erasePoints = if (selectedTool == WorkspaceDrawTool.Eraser) {
                            listOf(screenToCanvas(down.position))
                        } else {
                            emptyList()
                        }
                        var isTransforming = false
                        var lastCentroid: Offset? = null
                        var lastDistance = 0f

                        currentStrokePoints = gestureStrokePoints

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedChanges = event.changes.filter { change -> change.pressed }

                            if (pressedChanges.isEmpty()) {
                                if (
                                    !isTransforming &&
                                    selectedTool == WorkspaceDrawTool.Pen &&
                                    gestureStrokePoints.size > 1
                                ) {
                                    latestOnStrokesChange(
                                        latestStrokes + CanvasInkStroke(
                                            id = UUID.randomUUID().toString(),
                                            tool = InkTool.Pen,
                                            color = latestSelectedColor,
                                            width = DEFAULT_CANVAS_STROKE_WIDTH,
                                            points = gestureStrokePoints
                                        )
                                    )
                                } else if (
                                    !isTransforming &&
                                    selectedTool == WorkspaceDrawTool.Eraser &&
                                    erasePoints.isNotEmpty()
                                ) {
                                    latestOnStrokesChange(
                                        latestStrokes.eraseCanvasStrokes(erasePoints)
                                    )
                                }

                                currentStrokePoints = emptyList()
                                break
                            }

                            if (pressedChanges.size >= 2) {
                                isTransforming = true
                                gestureStrokePoints = emptyList()
                                currentStrokePoints = emptyList()

                                val centroid = centroidOf(pressedChanges)
                                val distance = averageDistanceFromCentroid(
                                    changes = pressedChanges,
                                    centroid = centroid
                                )
                                val previousCentroid = lastCentroid

                                if (previousCentroid != null) {
                                    val pan = centroid - previousCentroid
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
                                    val contentX = (centroid.x - canvasOffset.x) / oldScale
                                    val contentY = (centroid.y - canvasOffset.y) / oldScale

                                    canvasScale = newScale
                                    canvasOffset = clampOffset(
                                        Offset(
                                            x = centroid.x - (contentX * newScale) + pan.x,
                                            y = centroid.y - (contentY * newScale) + pan.y
                                        ),
                                        newScale
                                    )
                                }

                                lastCentroid = centroid
                                lastDistance = distance
                                pressedChanges.forEach { change -> change.consume() }
                            } else {
                                val change = pressedChanges.first()
                                lastCentroid = null
                                lastDistance = 0f

                                if (isTransforming || selectedTool == WorkspaceDrawTool.Move) {
                                    canvasOffset = clampOffset(
                                        canvasOffset + change.positionChange(),
                                        canvasScale
                                    )
                                } else if (selectedTool == WorkspaceDrawTool.Eraser) {
                                    erasePoints = erasePoints + screenToCanvas(change.position)
                                } else {
                                    gestureStrokePoints = gestureStrokePoints +
                                        screenToCanvas(change.position)
                                    currentStrokePoints = gestureStrokePoints
                                }

                                change.consume()
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawWorkspaceGrid(
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
                        width = DEFAULT_CANVAS_STROKE_WIDTH,
                        canvasOffset = canvasOffset,
                        canvasScale = canvasScale
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfDrawPanel(
    selectedTool: WorkspaceDrawTool,
    onToolChange: (WorkspaceDrawTool) -> Unit,
    selectedColor: Long,
    onColorChange: (Long) -> Unit,
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
                if (tool != WorkspaceDrawTool.Move) {
                    onToolChange(tool)
                }
            },
            selectedColor = selectedColor,
            onColorChange = onColorChange,
            canUndo = strokeCount > 0,
            onUndo = onUndo,
            showMove = false
        )
    }
}

@Composable
private fun DrawToolbar(
    selectedTool: WorkspaceDrawTool,
    onToolChange: (WorkspaceDrawTool) -> Unit,
    selectedColor: Long,
    onColorChange: (Long) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    showMove: Boolean = true
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawIconAction(
                    text = "Undo",
                    icon = DrawActionIcon.Undo,
                    enabled = canUndo,
                    onClick = onUndo
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
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
        modifier = Modifier.clickable { onClick() },
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
    text: String,
    icon: DrawActionIcon,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(enabled = enabled) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            drawActionIcon(
                icon = icon,
                color = if (enabled) Color(0xFF3A424A) else Color(0xFF9AA1A9)
            )
        }

        Text(
            text = text,
            color = if (enabled) Color(0xFF4F5963) else Color(0xFF9AA1A9),
            style = MaterialTheme.typography.labelSmall
        )
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
private fun WorkspaceTabLabel(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (selected) CarrotColors.Accent else Color(0xFF2D343B),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(width = 42.dp, height = 2.dp)
                .background(
                    color = if (selected) CarrotColors.Accent else Color.Transparent,
                    shape = RoundedCornerShape(999.dp)
                )
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWorkspaceGrid(
    canvasOffset: Offset,
    canvasScale: Float
) {
    val gridColor = Color(0x22000000)
    val majorGridColor = Color(0x33000000)
    val gridStep = 180f * canvasScale
    val majorGridStep = gridStep * 4f

    if (gridStep <= 2f) {
        return
    }

    var x = canvasOffset.x % gridStep
    while (x < size.width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += gridStep
    }

    var y = canvasOffset.y % gridStep
    while (y < size.height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += gridStep
    }

    x = canvasOffset.x % majorGridStep
    while (x < size.width) {
        drawLine(
            color = majorGridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.5f
        )
        x += majorGridStep
    }

    y = canvasOffset.y % majorGridStep
    while (y < size.height) {
        drawLine(
            color = majorGridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.5f
        )
        y += majorGridStep
    }
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

    val path = Path().apply {
        val first = points.first()
        moveTo(
            x = first.x * canvasScale + canvasOffset.x,
            y = first.y * canvasScale + canvasOffset.y
        )

        points.drop(1).forEach { point ->
            lineTo(
                x = point.x * canvasScale + canvasOffset.x,
                y = point.y * canvasScale + canvasOffset.y
            )
        }
    }

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
        stroke.points.any { point ->
            erasePoints.any { eraser ->
                val dx = point.x - eraser.x
                val dy = point.y - eraser.y
                dx * dx + dy * dy <= CANVAS_ERASER_RADIUS * CANVAS_ERASER_RADIUS
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawToolIcon(
    tool: WorkspaceDrawTool,
    color: Color
) {
    val stroke = 1.9.dp.toPx()

    when (tool) {
        WorkspaceDrawTool.Pen -> {
            drawLine(
                color = color,
                start = Offset(6.dp.toPx(), 17.dp.toPx()),
                end = Offset(16.dp.toPx(), 7.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(14.dp.toPx(), 5.dp.toPx()),
                end = Offset(18.dp.toPx(), 9.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(5.dp.toPx(), 18.dp.toPx()),
                end = Offset(10.dp.toPx(), 17.dp.toPx()),
                strokeWidth = stroke,
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
        }

        WorkspaceDrawTool.Eraser -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 9.dp.toPx()),
                style = Stroke(width = stroke),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
            drawLine(
                color = color,
                start = Offset(8.dp.toPx(), 16.dp.toPx()),
                end = Offset(18.dp.toPx(), 16.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
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
                size = androidx.compose.ui.geometry.Size(13.dp.toPx(), 13.dp.toPx()),
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

private enum class DrawActionIcon {
    Undo
}

private val DRAW_COLORS = listOf(
    0xFFFF7A1AL,
    0xFFE82929L,
    0xFFFFD83DL,
    0xFF1DB954L,
    0xFF3677F5L,
    0xFF111111L
)

private const val DRAW_CANVAS_WIDTH = 5000f
private const val DRAW_CANVAS_HEIGHT = 7000f
private const val INITIAL_CANVAS_SCALE = 0.22f
private const val MIN_CANVAS_SCALE = 0.08f
private const val MAX_CANVAS_SCALE = 0.9f
private const val DEFAULT_CANVAS_STROKE_WIDTH = 34f
private const val CANVAS_ERASER_RADIUS = 120f
