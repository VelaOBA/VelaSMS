package ng.com.vela.velasmsapp

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import ng.com.vela.velasms.VelaSMS
import ng.com.vela.velasms.VelaSMSReceiver
import ng.com.vela.velasms.encryption.SecurityUtils
import ng.com.vela.velasms.interfaces.VelaSMSEvent
import ng.com.vela.velasms.model.SSOEvent
import ng.com.vela.velasms.utils.VelaSMSConstants
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import timber.log.Timber

class MainActivityKotlin : AppCompatActivity(), VelaSMSEvent, EasyPermissions.PermissionCallbacks {


    private var testSmsButton: Button? = null
    private var progressBar: ProgressBar? = null
    private var progressUpdateTv: TextView? = null
    private var progressWrapper: LinearLayout? = null
    private val perms = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS,
            //            Manifest.permission.MODIFY_PHONE_STATE,
            Manifest.permission.READ_PHONE_STATE)

    private var progresShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        testSmsButton = findViewById(R.id.testSmsButton)
        progressBar = findViewById(R.id.progressBar)
        progressUpdateTv = findViewById(R.id.progressTv)
        progressWrapper = findViewById(R.id.progressWrapper)

        //add this as an Observer
        VelaSMS.addEventObserver(this)

        //call vela sms receiver
        VelaSMSReceiver.SMSEvent.observe(this, object : Observer<SSOEvent> {
            override fun onChanged(ssoEvent: SSOEvent) {
                Timber.d("SSO Event: %s", ssoEvent)
                val parts = ssoEvent.data.split(VelaSMSConstants.VELA_DELIMITERS.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val status = parts[0]

                when (status) {
                    VelaSMSConstants.RESPONSE_SUCCESS -> {
                        run {
                            //Use the actual response here.
                            Timber.d("Success Response: %s", parts[1])

                            val encryptionKey = VelaSMS.getEncryptionKey(this@MainActivityKotlin)
                            Timber.d("Using Encryption Key: $encryptionKey")
                            val encryption = SecurityUtils.getInstance(encryptionKey)

                            val decrypted = encryption.decrypt(parts[1].trim())
                            val result = "Result: $decrypted"
                        }
                        run { Timber.d("Error: Invalid Client App Id: %s", parts[1]) }
                        run { Timber.d("Error: Due to merchant Response: %s", parts[1]) }
                        run { Timber.d("Unknown Error occurred: %s", parts[1]) }
                    }
                    VelaSMSConstants.RESPONSE_ERROR_CLIENT_APP_ID -> {
                        run { Timber.d("Error: Invalid Client App Id: %s", parts[1]) }
                        run { Timber.d("Error: Due to merchant Response: %s", parts[1]) }
                        run { Timber.d("Unknown Error occurred: %s", parts[1]) }
                    }
                    VelaSMSConstants.RESPONSE_ERROR_INVALID_MERCHANT_RESPONSE -> {
                        run { Timber.d("Error: Due to merchant Response: %s", parts[1]) }
                        run { Timber.d("Unknown Error occurred: %s", parts[1]) }
                    }
                    VelaSMSConstants.RESPONSE_ERROR -> {
                        Timber.d("Unknown Error occurred: %s", parts[1])
                    }
                }

            }
        })

        testSmsButton!!.setOnClickListener {
            if (EasyPermissions.hasPermissions(this@MainActivityKotlin, *perms)) {
                sendSMS()
            } else {
                requestPermissions()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        //register receivers.
        VelaSMS.registerObservers(this)

    }

    override fun onPause() {
        super.onPause()
        //unregister all the receivers
        VelaSMS.unregisterObservers(this)

    }

    @AfterPermissionGranted(RC_VELA_SMS)
    private fun sendSMS() {
        val samplePayload = "1:20:181702773757!48000000:08060000000:NG:t3stt3st:352085099972999"

        //send sms
        VelaSMS.send(this, samplePayload)

    }

    private fun requestPermissions() {
        EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, RC_VELA_SMS, *perms)
                        .setRationale(ng.com.vela.velasms.R.string.sms_rationale)
                        .setPositiveButtonText(ng.com.vela.velasms.R.string.rationale_ask_ok)
                        .setNegativeButtonText(ng.com.vela.velasms.R.string.rationale_ask_cancel)
                        //                .setTheme(R.style.my_fancy_style)
                        .build()
        )
    }

    override fun hideProgress() {
        progressWrapper!!.visibility = View.GONE
        progresShowing = false

    }

    override fun showError(s: String) {
        Toast.makeText(this, "An Error occurred: \$s", Toast.LENGTH_LONG).show()
    }

    override fun showProgress() {
        progressWrapper!!.visibility = View.VISIBLE
        progresShowing = true
    }

    override fun updateProgress(s: String) {
        progressUpdateTv!!.text = s

    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Timber.d("onPermissionsGranted(): Code: %s | Perms: %s", requestCode, perms)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Timber.d("onPermissionDenied(): Code: %s | Perms: %s", requestCode, perms)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Timber.d("onRequestPermissionsResult(): Code: %s | Perms: %s", requestCode, permissions)

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    companion object {
        private const val RC_VELA_SMS = 137
    }
}
