package bz.kakadu.demoapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import bz.kakadu.scanner.IScanner
import bz.kakadu.scanner.OnBarcodeDetectorListener
import bz.kakadu.scanner.ScannerFragment
import com.google.android.gms.vision.barcode.Barcode

class MainActivity : AppCompatActivity(), OnBarcodeDetectorListener {
    override fun onBarcodeDetected(scanner: IScanner?, barcode: Barcode) {
        AlertDialog.Builder(this)
            .setTitle("Barcode")
            .setMessage(
                "displayValue: ${barcode.displayValue}\n" +
                        "format: ${barcode.formatName}"
            )
            .setPositiveButton(android.R.string.ok, null)
            .setOnDismissListener { scanner?.continueScan() }
            .show()
    }

    override fun onError(error: OnBarcodeDetectorListener.Error) {
        Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
        supportFragmentManager.popBackStack()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun clickStartScanner(v: View) {
        supportFragmentManager
            .beginTransaction()
            .replace(
                android.R.id.content,
                ScannerFragment.instance(
                    overlayColor = 0xAAFF0000.toInt()//,   formats = Barcode.EAN_13 or Barcode.QR_CODE
                )
            )
            .addToBackStack(null)
            .commit()
    }
}

private val Barcode.formatName
    get() = when (format) {
        Barcode.ALL_FORMATS -> "ALL_FORMATS"
        Barcode.CODE_128 -> "CODE_128"
        Barcode.CODE_39 -> "CODE_39"
        Barcode.CODE_93 -> "CODE_93"
        Barcode.CODABAR -> "CODABAR"
        Barcode.DATA_MATRIX -> "DATA_MATRIX"
        Barcode.EAN_13 -> "EAN_13"
        Barcode.EAN_8 -> "EAN_8"
        Barcode.ITF -> "ITF"
        Barcode.QR_CODE -> "QR_CODE"
        Barcode.UPC_A -> "UPC_A"
        Barcode.UPC_E -> "UPC_E"
        Barcode.PDF417 -> "PDF417"
        Barcode.AZTEC -> "AZTEC"
        Barcode.ISBN -> "ISBN"
        Barcode.PRODUCT -> "PRODUCT"
        Barcode.SMS -> "SMS"
        Barcode.TEXT -> "TEXT"
        Barcode.WIFI -> "WIFI"
        Barcode.GEO -> "GEO"
        Barcode.CALENDAR_EVENT -> "CALENDAR_EVENT"
        Barcode.DRIVER_LICENSE -> "DRIVER_LICENSE"
        else -> "Unknown"
    }
