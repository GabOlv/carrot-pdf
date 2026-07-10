package com.example.carrotpdf.ui.components

import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType

internal fun PointerInputChange.isStylusLikePointer(): Boolean {
    return type == PointerType.Stylus || type == PointerType.Eraser
}

internal fun PointerInputChange.isStylusEraserPointer(): Boolean {
    return type == PointerType.Eraser
}

internal fun shouldRejectContactForPalmRejection(
    stylusActive: Boolean,
    contactIsStylus: Boolean
): Boolean {
    return stylusActive && !contactIsStylus
}

internal fun List<PointerInputChange>.consumePalmContactsWhenStylusActive(
    stylusActive: Boolean
) {
    forEach { change ->
        if (
            shouldRejectContactForPalmRejection(
                stylusActive = stylusActive,
                contactIsStylus = change.isStylusLikePointer()
            )
        ) {
            change.consume()
        }
    }
}

internal fun List<PointerInputChange>.preferredInkChange(
    activePointerId: PointerId?
): PointerInputChange? {
    if (isEmpty()) {
        return null
    }

    val activeChange = activePointerId?.let { id ->
        firstOrNull { change -> change.id == id && change.pressed }
    }

    if (activeChange?.isStylusLikePointer() == true) {
        return activeChange
    }

    val stylusChange = firstOrNull { change -> change.isStylusLikePointer() }
    if (stylusChange != null) {
        return stylusChange
    }

    if (activeChange != null) {
        return activeChange
    }

    return firstOrNull { change -> change.type == PointerType.Mouse } ?: first()
}
