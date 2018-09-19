/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bz.kakadu.scanner

import android.Manifest
import android.arch.lifecycle.Lifecycle.Event.ON_PAUSE
import android.arch.lifecycle.Lifecycle.Event.ON_RESUME
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.support.annotation.RequiresPermission
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import bz.kakadu.scanner.OnBarcodeDetectorListener.Error.*
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException

class CameraSourcePreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    LifecycleObserver {
    private val scannerPreviewBounds = Rect()
    private val tempBounds = Rect()
    var scannerSize: Int = 0
        set(value) {
            field = value
            overlayDrawable.scannerSize = value
        }
    private val surfaceView: SurfaceView
    private var startRequested: Boolean = false
    private var surfaceAvailable: Boolean = false
    private var cameraSource: CameraSource? = null
    private var onBarcodeDetectorListener: OnBarcodeDetectorListener? = null
    private var layoutByPreview: Boolean = false
    private val overlayDrawable: OverlayDrawable
    var overlayColor: Int = 0
        get() = overlayDrawable.color
        set(value) {
            field = value
            overlayDrawable.color = value
        }
    /**
     * Barcode.ALL_FORMATS,
     * Barcode.CODE_128,
     * Barcode.CODE_39,
     * Barcode.CODE_93,
     * Barcode.CODABAR,
     * Barcode.DATA_MATRIX,
     * Barcode.EAN_13,
     * Barcode.EAN_8,
     * Barcode.ITF,
     * Barcode.QR_CODE,
     * Barcode.UPC_A,
     * Barcode.UPC_E,
     * Barcode.PDF417,
     * Barcode.AZTEC
     */
    var formatsInt = Barcode.ALL_FORMATS


    init {
        val dm = resources.displayMetrics
        startRequested = false
        surfaceAvailable = false
        surfaceView = SurfaceView(context)
        surfaceView.holder.addCallback(SurfaceCallback())
        val overlay = View(context)
        val scannerSize = (Math.min(dm.heightPixels, dm.widthPixels) * .7f).toInt()
        overlayDrawable = OverlayDrawable(scannerSize)
        this.scannerSize = scannerSize
        overlay.background = overlayDrawable
        addView(
            overlay,
            0,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        addView(
            surfaceView,
            0,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    @OnLifecycleEvent(ON_PAUSE)
    fun release() {
        cameraSource?.release()
        cameraSource = null
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @Throws(IOException::class, SecurityException::class)
    private fun startIfReady() {
        if (!hasCameraPermission(context)) return
        if (startRequested && surfaceAvailable) {
            cameraSource!!.start(surfaceView.holder)
            startRequested = false
        }
    }

    @OnLifecycleEvent(ON_RESUME)
    fun start() {
        try {
            if (cameraSource == null) {
                createCameraSource()
            }


            if (cameraSource != null) {
                startRequested = true
                startIfReady()
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "Do not have permission to start the camera", se)
            onBarcodeDetectorListener!!.onError(NOT_PERMISSION)
        } catch (e: IOException) {
            Log.e(TAG, "Could not start camera source.", e)
            onBarcodeDetectorListener!!.onError(NOT_START)
        }

    }

    private fun createCameraSource() {
        if (cameraSource != null) return
        val context = context.applicationContext
        val barcodeDetector = BarcodeDetector.Builder(context)
            .setBarcodeFormats(formatsInt)
            .build()
        barcodeDetector.setProcessor(
            object : Detector.Processor<Barcode> {
                override fun release() {
                }

                override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                    //                        Log.i(TAG, "receiveDetections: " + detections.getDetectedItems() + "layoutByPreview=" + layoutByPreview);
                    if (!hasWindowFocus()) return
                    for (i in 0 until detections.detectedItems.size()) {

                        val barcode =
                            detections.detectedItems.get(detections.detectedItems.keyAt(i))
                        val boundingBox = barcode.boundingBox
                        //                            Log.v("rom", "boundingBox: " + boundingBox + ", scannerPreviewBounds=" + scannerPreviewBounds);
                        if (scannerPreviewBounds.contains(boundingBox) || boundingBox.contains(
                                scannerPreviewBounds.centerX(),
                                scannerPreviewBounds.centerY()
                            )
                        ) {
                            onBarcodeDetectorListener!!.onBarcodeDetected(null, barcode)
                            return
                        }
                    }
                    if (!layoutByPreview) {
                        post { requestLayout() }
                    }
                }
            })

        if (!barcodeDetector.isOperational) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.")
            onBarcodeDetectorListener!!.onError(NOT_AVAILABLE)
            return
        }

        // Check for low storage.  If there is low storage, the native library will not be
        // downloaded, so detection will not become operational.
        val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
        val hasLowStorage = context.registerReceiver(null, lowstorageFilter) != null

        if (hasLowStorage) {
            Log.w(TAG, "Low storage error")
            onBarcodeDetectorListener!!.onError(STORAGE_LOW)
            return
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        val size = 1920
        val builder = CameraSource.Builder(context, barcodeDetector)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setAutoFocusEnabled(true)
            .setRequestedPreviewSize(size, size)
            .setRequestedFps(15.0f)
        cameraSource = builder
            .build()

    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (cameraSource != null) {
            val size = cameraSource!!.previewSize
            if (size != null) {
                val width = size.width
                val height = size.height
                val parentWidth = right - left
                val parentHeight = bottom - top
                val scale = Math.max(
                    parentHeight.toFloat() / width,
                    parentWidth.toFloat() / height
                )
                tempBounds.set(
                    0,
                    0,
                    (height * scale).toInt(),//width for portail
                    (width * scale).toInt()
                )
                val scannerPreviewSize = (scannerSize / scale).toInt()
                scannerPreviewBounds.set(0, 0, scannerPreviewSize, scannerPreviewSize)
                scannerPreviewBounds.offsetTo(
                    (height / 2f - scannerPreviewSize / 2f).toInt(),
                    (width / 2f - scannerPreviewSize / 2f).toInt()
                )
                tempBounds.offsetTo(
                    (left + (parentWidth - tempBounds.width()) / 2f).toInt(),
                    (top + (parentHeight - tempBounds.height()) / 2f).toInt()
                )
                surfaceView.requestLayout()//?
                surfaceView.layout(
                    tempBounds.left,
                    tempBounds.top,
                    tempBounds.right,
                    tempBounds.bottom

                )


                layoutByPreview = true
                start()
                return
            }
        }
        super.onLayout(changed, left, top, right, bottom)
        start()
    }

    fun setOnBarcodeDetectorListener(
        onBarcodeDetectorListener: OnBarcodeDetectorListener,
        lifecycleOwner: LifecycleOwner? = null
    ) {
        lifecycleOwner?.lifecycle?.addObserver(this)
        this.onBarcodeDetectorListener = onBarcodeDetectorListener
    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(surface: SurfaceHolder) {
            surfaceAvailable = true
            try {
                startIfReady()
            } catch (se: SecurityException) {
                Log.e(TAG, "Do not have permission to start the camera", se)
                onBarcodeDetectorListener!!.onError(NOT_PERMISSION)
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
                onBarcodeDetectorListener!!.onError(NOT_START)
            }

        }

        override fun surfaceDestroyed(surface: SurfaceHolder) {
            surfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    }

    companion object {
        private const val TAG = "CameraSourcePreview"
        @JvmStatic
        fun hasCameraPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
