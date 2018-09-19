package bz.kakadu.scanner

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.ColorDrawable

/**
 * Created by Roman Tsarou on 12.04.2018.
 */
internal class OverlayDrawable(scannerSize: Int) : ColorDrawable(0xAA000000.toInt()) {
    private val scannerBounds = Rect(0, 0, scannerSize, scannerSize)
    var scannerSize = scannerSize
        set(value) {
            if (value != field) {
                field = value
                scannerBounds.set(
                    0, 0, value, value
                )
                updateScannerBounds()
                invalidateSelf()
            }
        }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        updateScannerBounds()
    }

    private fun updateScannerBounds() {
        scannerBounds.offsetTo(
            bounds.centerX() - scannerBounds.width() / 2,
            bounds.centerY() - scannerBounds.height() / 2
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        @Suppress("DEPRECATION")
        canvas.clipRect(scannerBounds, Region.Op.DIFFERENCE)
        super.draw(canvas)
        canvas.restore()
    }
}
