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
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer

class CardDetectorPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CameraSourcePreview(context, attrs, defStyleAttr) {

    private lateinit var onCardDetectorListener: OnCardDetectorListener

    fun setOnCardDetectorListener(
        onCardDetectorListener: OnCardDetectorListener,
        lifecycleOwner: LifecycleOwner? = null
    ) {
        this.onCardDetectorListener = onCardDetectorListener
        setOnScannerListener(onCardDetectorListener, lifecycleOwner)
    }

    override fun createDetector(): Detector<TextBlock>? {
        val context = context.applicationContext
        val detector = TextRecognizer.Builder(context)
            .build()
        detector.setProcessor(
            object : Detector.Processor<TextBlock> {
                override fun release() {
                }

                override fun receiveDetections(detections: Detector.Detections<TextBlock>) {
                    //                        Log.i(TAG, "receiveDetections: " + detections.getDetectedItems() + "layoutByPreview=" + layoutByPreview);
                    if (!hasWindowFocus()) return
                    for (i in 0 until detections.detectedItems.size()) {

                        val textBlock =
                            detections.detectedItems.get(detections.detectedItems.keyAt(i))
                        val boundingBox = textBlock.boundingBox
                        //                            Log.v("rom", "boundingBox: " + boundingBox + ", scannerPreviewBounds=" + scannerPreviewBounds);
                        if (scannerPreviewBounds.contains(boundingBox)
                        ) {
                            Log.i(
                                TAG,
                                "textBlock: ${textBlock.value}, ${textBlock.components.map { it.value + "(${it.components.size})" }}"
                            )
//                            onCardDetectorListener.onCardDetected(null, textBlock)
//                            return
                        }
                    }
                    if (!layoutByPreview) {
                        post { requestLayout() }
                    }
                }
            })

        if (!detector.isOperational) {
            // Note: The first time that an app using the Card or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any Cards
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.")
            onCardDetectorListener.onError(NOT_AVAILABLE)
            return null
        }
        return detector
    }

    interface OnCardDetectorListener : OnScannerListener {
        fun onCardDetected(scanner: IScanner?, cardInfo: CardInfo)
    }

    data class CardInfo(val cardHolder: String?, val number: String?, val date: String?)
}
