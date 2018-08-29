package bz.kakadu.scanner

import com.google.android.gms.vision.barcode.Barcode

/**
 * Created by Roman Tsarou on 29.08.2018.
 */
interface OnBarcodeDetectorListener {
    enum class Error {
        /**
         * Low storage error
         */
        STORAGE_LOW,
        /**
         * Detector dependencies are not yet available
         */
        NOT_AVAILABLE,
        /**
         * Could not start camera source
         */
        NOT_START,
        /**
         * Do not have permission to start the camera
         */
        NOT_PERMISSION,


    }

    fun onBarcodeDetected(scanner: IScanner?, barcode: Barcode)

    fun onError(error: Error)
}
