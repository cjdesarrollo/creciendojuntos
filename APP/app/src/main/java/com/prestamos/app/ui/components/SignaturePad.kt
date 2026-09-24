package com.prestamos.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.prestamos.app.ui.theme.BorderSlate
import com.prestamos.app.ui.theme.CardSurface
import com.prestamos.app.ui.theme.NavyPrimary

data class PathState(
    val path: Path,
    val color: Color = Color.Black,
    val strokeWidth: Float = 6f
)

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSignatureSaved: (Bitmap) -> Unit,
    onClear: () -> Unit = {}
) {
    val pathList = remember { mutableStateListOf<PathState>() }
    var currentPath = remember { Path() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Firma Digital del Cliente",
            style = MaterialTheme.typography.titleMedium,
            color = NavyPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface)
                .border(1.5.dp, BorderSlate, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = Path().apply { moveTo(offset.x, offset.y) }
                            pathList.add(PathState(currentPath))
                        },
                        onDrag = { change, _ ->
                            currentPath.lineTo(change.position.x, change.position.y)
                            // Forzar redibujado agregando/actualizando el último elemento
                            if (pathList.isNotEmpty()) {
                                pathList[pathList.size - 1] = PathState(currentPath)
                            }
                        }
                    )
                }
        ) {
            pathList.forEach { pathState ->
                drawPath(
                    path = pathState.path,
                    color = pathState.color,
                    style = Stroke(
                        width = pathState.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = {
                    pathList.clear()
                    onClear()
                }
            ) {
                Text("Limpiar")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    if (pathList.isNotEmpty()) {
                        val bitmap = createBitmapFromPathList(pathList, 800, 400)
                        onSignatureSaved(bitmap)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Guardar Firma")
            }
        }
    }
}

private fun createBitmapFromPathList(pathList: List<PathState>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = AndroidPaint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 8f
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
        isAntiAlias = true
    }

    pathList.forEach { pathState ->
        canvas.drawPath(pathState.path.asAndroidPath(), paint)
    }

    return bitmap
}
