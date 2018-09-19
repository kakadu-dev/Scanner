package bz.kakadu.demoapp

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import bz.kakadu.scanner.*
import bz.kakadu.scanner.BarcodeDetectorPreview.OnBarcodeDetectorListener
import bz.kakadu.scanner.CardDetectorPreview.OnCardDetectorListener
import com.google.android.gms.vision.barcode.Barcode

class MainActivity : AppCompatActivity(), OnBarcodeDetectorListener, OnCardDetectorListener {
    override fun onCardDetected(scanner: IScanner?, cardInfo: CardDetectorPreview.CardInfo) {
        AlertDialog.Builder(this)
            .setTitle("Card")
            .setMessage(cardInfo.toString())
            .setPositiveButton(android.R.string.ok, null)
            .setOnDismissListener { scanner?.continueScan() }
            .show()
    }

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

    override fun onError(error: ScannerError) {
        Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
        supportFragmentManager.popBackStack()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun clickStartCardScanner(v: View) {
        supportFragmentManager
            .beginTransaction()
            .replace(
                android.R.id.content,
                CardScannerFragment.instance(
                    overlayColor = 0xAA000000.toInt()
                )
            )
            .addToBackStack(null)
            .commit()
    }

    fun clickStartScanner(v: View) {
        supportFragmentManager
            .beginTransaction()
            .replace(
                android.R.id.content,
                BarcodeScannerFragment.instance(
                    overlayColor = 0xAAFF0000.toInt(),
                    formats = Barcode.EAN_13 or Barcode.QR_CODE
                )
            )
            .addToBackStack(null)
            .commit()
    }
}
