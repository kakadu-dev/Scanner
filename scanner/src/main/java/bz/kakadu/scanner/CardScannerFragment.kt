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
import bz.kakadu.scanner.CardDetectorPreview.OnCardDetectorListener
import bz.kakadu.scanner.ScannerUtils.hasCameraPermission
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by Roman Tsarou on 25.05.2018.
 */
private const val REQUEST_PERMISSION = 322

class CardScannerFragment : Fragment(), IScanner,
    OnCardDetectorListener {

    protected lateinit var detectorPreview: CardDetectorPreview
    private val isCardDetected = AtomicBoolean()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        detectorPreview = CardDetectorPreview(activity!!)
        arguments?.getInt("overlayColor").takeIf { it != 0 }?.also { overlayColor ->
            detectorPreview.overlayColor = overlayColor
        }
        arguments?.getInt("scannerSize").takeIf { it != 0 }?.also { scannerSize ->
            detectorPreview.scannerSize = scannerSize
        }
        detectorPreview.setOnCardDetectorListener(this, viewLifecycleOwner)
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
        isCardDetected.set(true)
    }

    override fun continueScan() {
        isCardDetected.set(false)
    }

    override fun onCardDetected(scanner: IScanner?, cardInfo: CardDetectorPreview.CardInfo) {
        if (!isCardDetected.getAndSet(true)) {
            val vibrator =
                activity?.getSystemService(AppCompatActivity.VIBRATOR_SERVICE) as Vibrator?
            @Suppress("DEPRECATION")
            vibrator?.vibrate(300)
            activity?.runOnUiThread { barcodeDetectorListener?.onCardDetected(this, cardInfo) }
        }
    }

    private val barcodeDetectorListener: OnCardDetectorListener?
        get() = when {
            parentFragment is OnCardDetectorListener -> parentFragment as OnCardDetectorListener
            activity is OnCardDetectorListener -> activity as OnCardDetectorListener
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
            scannerSize: Int = 0
        ) = CardScannerFragment().apply {
            arguments = Bundle().apply {
                putInt("overlayColor", overlayColor)
                putInt("scannerSize", scannerSize)
            }
        }
    }
}