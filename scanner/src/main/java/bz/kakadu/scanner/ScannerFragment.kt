package bz.kakadu.scanner

import android.Manifest
import android.os.Bundle
import android.os.Vibrator
import android.support.annotation.ColorInt
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bz.kakadu.scanner.OnBarcodeDetectorListener.Error
import com.google.android.gms.vision.barcode.Barcode
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Roman Tsarou on 25.05.2018.
 */
private const val REQUEST_PERMISSION = 321

class ScannerFragment : Fragment(), IScanner, OnBarcodeDetectorListener {

    protected lateinit var cameraSourcePreview: CameraSourcePreview
    private val isBarcodeDetected = AtomicBoolean()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        cameraSourcePreview = CameraSourcePreview(activity!!, null)
        arguments?.getInt("overlayColor").takeIf { it != 0 }?.also { overlayColor ->
            cameraSourcePreview.overlayColor = overlayColor
        }
        arguments?.getInt("scannerSize").takeIf { it != 0 }?.also { scannerSize ->
            cameraSourcePreview.scannerSize = scannerSize
        }
        arguments?.getInt("formatsInt").takeIf { it != 0 }?.also { formatsInt ->
            cameraSourcePreview.formatsInt = formatsInt
        }
        cameraSourcePreview.setOnBarcodeDetectorListener(this, viewLifecycleOwner)
        return cameraSourcePreview
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null && !CameraSourcePreview.hasCameraPermission(activity!!)) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (CameraSourcePreview.hasCameraPermission(activity!!)) {
                cameraSourcePreview.start()
            } else {
                onError(Error.NOT_PERMISSION)
            }
        }
    }

    override fun stopScan() {
        isBarcodeDetected.set(true)
    }

    override fun continueScan() {
        isBarcodeDetected.set(false)
    }

    override fun onBarcodeDetected(scanner: IScanner?, barcode: Barcode) {
        if (!isBarcodeDetected.getAndSet(true)) {
            val vibrator =
                activity?.getSystemService(AppCompatActivity.VIBRATOR_SERVICE) as Vibrator?
            @Suppress("DEPRECATION")
            vibrator?.vibrate(300)
            activity?.runOnUiThread { barcodeDetectorListener?.onBarcodeDetected(this, barcode) }
        }
    }

    private val barcodeDetectorListener: OnBarcodeDetectorListener?
        get() = when {
            parentFragment is OnBarcodeDetectorListener -> parentFragment as OnBarcodeDetectorListener
            activity is OnBarcodeDetectorListener -> activity as OnBarcodeDetectorListener
            else -> null
        }

    override fun onError(error: Error) {
        activity?.runOnUiThread {
            barcodeDetectorListener?.onError(error)
        }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun instance(
            @ColorInt overlayColor: Int = 0,
            scannerSize: Int = 0,
            formats: Int = Barcode.ALL_FORMATS
        ) = ScannerFragment().apply {
            arguments = Bundle().apply {
                putInt("overlayColor", overlayColor)
                putInt("scannerSize", scannerSize)
                putInt("formatsInt", formats)
            }
        }
    }
}