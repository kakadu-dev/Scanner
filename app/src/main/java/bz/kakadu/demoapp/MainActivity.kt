package bz.kakadu.demoapp

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
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
                        "format: ${barcode.format}"
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
                    overlayColor = 0xAAFF0000.toInt(),
                    formats = Barcode.EAN_13 or Barcode.QR_CODE
                )
            )
            .addToBackStack(null)
            .commit()
    }
}
