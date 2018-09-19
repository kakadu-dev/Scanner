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
import bz.kakadu.scanner.BarcodeDetectorPreview.OnBarcodeDetectorListener
import bz.kakadu.scanner.ScannerUtils.hasCameraPermission
import com.google.android.gms.vision.barcode.Barcode
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by Roman Tsarou on 25.05.2018.
 */
private const val REQUEST_PERMISSION = 321

class BarcodeScannerFragment : Fragment(), IScanner,
    OnBarcodeDetectorListener {

    protected lateinit var detectorPreview: BarcodeDetectorPreview
    private val isBarcodeDetected = AtomicBoolean()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        detectorPreview = BarcodeDetectorPreview(activity!!)
        arguments?.getInt("overlayColor").takeIf { it != 0 }?.also { overlayColor ->
            detectorPreview.overlayColor = overlayColor
        }
        arguments?.getInt("scannerSize").takeIf { it != 0 }?.also { scannerSize ->
            detectorPreview.scannerSize = scannerSize
        }
        arguments?.getInt("formatsInt").takeIf { it != 0 }?.also { formatsInt ->
            detectorPreview.formatsInt = formatsInt
        }
        detectorPreview.setOnBarcodeDetectorListener(this, viewLifecycleOwner)
        return detectorPreview
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null && !hasCameraPermission(activity!!)) {
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
            if (hasCameraPermission(activity!!)) {
                detectorPreview.start()
            } else {
                onError(ScannerError.NOT_PERMISSION)
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

    override fun onError(error: ScannerError) {
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
        ) = BarcodeScannerFragment().apply {
            arguments = Bundle().apply {
                putInt("overlayColor", overlayColor)
                putInt("scannerSize", scannerSize)
                putInt("formatsInt", formats)
            }
        }
    }
}