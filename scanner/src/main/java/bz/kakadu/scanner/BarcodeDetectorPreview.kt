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

import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import bz.kakadu.scanner.ScannerError.NOT_AVAILABLE
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector

class BarcodeDetectorPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CameraSourcePreview(context, attrs, defStyleAttr) {


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
    private lateinit var onBarcodeDetectorListener: OnBarcodeDetectorListener

    fun setOnBarcodeDetectorListener(
        onBarcodeDetectorListener: OnBarcodeDetectorListener,
        lifecycleOwner: LifecycleOwner? = null
    ) {
        this.onBarcodeDetectorListener = onBarcodeDetectorListener
        setOnScannerListener(onBarcodeDetectorListener, lifecycleOwner)
    }

    override fun createDetector(): Detector<Barcode>? {
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
                            onBarcodeDetectorListener.onBarcodeDetected(null, barcode)
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
            onBarcodeDetectorListener.onError(NOT_AVAILABLE)
            return null
        }
        return barcodeDetector
    }

    interface OnBarcodeDetectorListener : OnScannerListener {
        fun onBarcodeDetected(scanner: IScanner?, barcode: Barcode)
    }
}
