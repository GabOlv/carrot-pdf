package com.example.carrotpdf.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.input.pointer.pointerInput
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

@Composable
fun NotesWorkspaceSheet(
    notesText: String,
    onNotesChange: (String) -> Unit,
    selectedMode: WorkspaceMode,
    onModeChange: (WorkspaceMode) -> Unit,
    canvasStrokes: List<CanvasInkStroke>,
    onCanvasStrokesChange: (List<CanvasInkStroke>) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 22.dp,
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            ),
        color = Color(0xF51A1F25),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                )
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 4.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.42f),
                            shape = RoundedCornerShape(999.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedMode) {
                WorkspaceMode.Notes -> NotesEditor(
                    notesText = notesText,
                    onNotesChange = onNotesChange
                )

                WorkspaceMode.Draw -> FreeDrawCanvas(
                    strokes = canvasStrokes,
                    onStrokesChange = onCanvasStrokesChange
                )
            }
        }
    }
}

@Composable
private fun NotesEditor(
    notesText: String,
    onNotesChange: (String) -> Unit
) {
    BasicTextField(
        value = notesText,
        onValueChange = onNotesChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 260.dp)
            .background(
                color = Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = CarrotColors.TextPrimary
        ),
        cursorBrush = SolidColor(CarrotColors.Accent),
        decorationBox = { innerTextField ->
            if (notesText.isBlank()) {
                Text(
                    text = "Type your notes...",
                    color = CarrotColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            innerTextField()
        }
    )
}

@Composable
private fun FreeDrawCanvas(
    strokes: List<CanvasInkStroke>,
    onStrokesChange: (List<CanvasInkStroke>) -> Unit
) {
    var interactionMode by remember { mutableStateOf(CanvasInteractionMode.Draw) }
    var selectedColor by remember { mutableStateOf(DRAW_COLORS.first()) }
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
            interactionMode = interactionMode,
            onInteractionModeChange = { mode ->
                interactionMode = mode
                currentStrokePoints = emptyList()
            },
            selectedColor = selectedColor,
            onColorChange = { color ->
                selectedColor = color
            },
            canUndo = strokes.isNotEmpty(),
            onUndo = {
                if (strokes.isNotEmpty()) {
                    onStrokesChange(strokes.dropLast(1))
                }
            },
            onClear = {
                if (strokes.isNotEmpty()) {
                    onStrokesChange(emptyList())
                }
            },
            onZoomOut = {
                canvasScale = (canvasScale * 0.82f).coerceIn(
                    MIN_CANVAS_SCALE,
                    MAX_CANVAS_SCALE
                )
                canvasOffset = clampOffset(canvasOffset, canvasScale)
            },
            onZoomIn = {
                canvasScale = (canvasScale * 1.18f).coerceIn(
                    MIN_CANVAS_SCALE,
                    MAX_CANVAS_SCALE
                )
                canvasOffset = clampOffset(canvasOffset, canvasScale)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
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
                .pointerInput(interactionMode) {
                    detectDragGestures(
                        onDragStart = { position ->
                            if (interactionMode == CanvasInteractionMode.Draw) {
                                currentStrokePoints = listOf(screenToCanvas(position))
                            }
                        },
                        onDragEnd = {
                            if (
                                interactionMode == CanvasInteractionMode.Draw &&
                                currentStrokePoints.size > 1
                            ) {
                                latestOnStrokesChange(
                                    latestStrokes + CanvasInkStroke(
                                        id = UUID.randomUUID().toString(),
                                        tool = InkTool.Pen,
                                        color = latestSelectedColor,
                                        width = DEFAULT_CANVAS_STROKE_WIDTH,
                                        points = currentStrokePoints
                                    )
                                )
                            }

                            currentStrokePoints = emptyList()
                        },
                        onDragCancel = {
                            currentStrokePoints = emptyList()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            if (interactionMode == CanvasInteractionMode.Move) {
                                canvasOffset = clampOffset(
                                    canvasOffset + dragAmount,
                                    canvasScale
                                )
                            } else {
                                currentStrokePoints = currentStrokePoints +
                                    screenToCanvas(change.position)
                            }
                        }
                    )
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
                        color = Color(stroke.color.toULong()),
                        width = stroke.width,
                        canvasOffset = canvasOffset,
                        canvasScale = canvasScale
                    )
                }

                if (currentStrokePoints.isNotEmpty()) {
                    drawInkStroke(
                        points = currentStrokePoints,
                        color = Color(selectedColor.toULong()),
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
private fun DrawToolbar(
    interactionMode: CanvasInteractionMode,
    onInteractionModeChange: (CanvasInteractionMode) -> Unit,
    selectedColor: Long,
    onColorChange: (Long) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit
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
                DrawChip(
                    text = "Pen",
                    selected = interactionMode == CanvasInteractionMode.Draw,
                    onClick = {
                        onInteractionModeChange(CanvasInteractionMode.Draw)
                    }
                )
                DrawChip(
                    text = "Move",
                    selected = interactionMode == CanvasInteractionMode.Move,
                    onClick = {
                        onInteractionModeChange(CanvasInteractionMode.Move)
                    }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawChip(
                    text = "-",
                    selected = false,
                    onClick = onZoomOut
                )
                DrawChip(
                    text = "+",
                    selected = false,
                    onClick = onZoomIn
                )
                DrawChip(
                    text = "Undo",
                    selected = false,
                    enabled = canUndo,
                    onClick = onUndo
                )
                DrawChip(
                    text = "Clear",
                    selected = false,
                    enabled = canUndo,
                    onClick = onClear
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
                color = Color(color.toULong()),
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
            color = if (selected) CarrotColors.Accent else CarrotColors.TextSecondary,
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

private enum class CanvasInteractionMode {
    Draw,
    Move
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
