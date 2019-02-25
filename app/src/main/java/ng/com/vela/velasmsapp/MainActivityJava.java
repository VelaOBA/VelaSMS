package ng.com.vela.velasmsapp;

import android.Manifest;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import ng.com.vela.velasms.VelaSMS;
import ng.com.vela.velasms.VelaSMSReceiver;
import ng.com.vela.velasms.encryption.SecurityUtils;
import ng.com.vela.velasms.interfaces.VelaSMSEvent;
import ng.com.vela.velasms.model.SSOEvent;
import ng.com.vela.velasms.utils.VelaSMSConstants;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;
import timber.log.Timber;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MainActivityJava extends AppCompatActivity implements VelaSMSEvent, EasyPermissions.PermissionCallbacks {


    private final int RC_VELA_SMS = 0211;

    private Button testSmsButton;
    private ProgressBar progressBar;
    private TextView progressUpdateTv;
    private LinearLayout progressWrapper;
    private final String[] perms = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
//            Manifest.permission.MODIFY_PHONE_STATE,
            Manifest.permission.READ_PHONE_STATE
    };

    private boolean progresShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        testSmsButton = findViewById(R.id.testSmsButton);
        progressBar = findViewById(R.id.progressBar);
        progressUpdateTv = findViewById(R.id.progressTv);
        progressWrapper = findViewById(R.id.progressWrapper);

        //add this as an Observer
        VelaSMS.addEventObserver(this);

        //call vela sms receiver
        VelaSMSReceiver.getSMSEvent().observe(this, new Observer<SSOEvent>() {
            @Override
            public void onChanged(SSOEvent ssoEvent) {
                Timber.d("SSO Event: %s", ssoEvent);
                final String[] parts = ssoEvent.getData().split(VelaSMSConstants.VELA_DELIMITERS);
                final String status = parts[0];

                switch (status) {
                    case VelaSMSConstants.RESPONSE_SUCCESS: {
                        //Use the actual response here.
                        Timber.d("Success Response: %s", parts[1]);

                        final String encryptionKey = VelaSMS.INSTANCE.getEncryptionKey(MainActivityJava.this);
                        Timber.d("Using Encryption key: %s", encryptionKey);

                        try {
                            final SecurityUtils encryption = SecurityUtils.Companion.getInstance(encryptionKey);
                            final String decryptedResult = encryption.decrypt(parts[1].trim());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    case VelaSMSConstants.RESPONSE_ERROR_CLIENT_APP_ID: {
                        Timber.d("Error: Invalid Client App Id: %s", parts[1]);
                    }
                    case VelaSMSConstants.RESPONSE_ERROR_INVALID_MERCHANT_RESPONSE: {
                        Timber.d("Error: Due to merchant Response: %s", parts[1]);
                    }
                    case VelaSMSConstants.RESPONSE_ERROR: {
                        Timber.d("Unknown Error occurred: %s", parts[1]);
                    }
                }

            }
        });

        testSmsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EasyPermissions.hasPermissions(MainActivityJava.this, perms)) {
                    sendSMS();
                } else {
                    requestPermissions();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        //register receivers.
        VelaSMS.registerObservers(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregister all the receivers
        VelaSMS.unregisterObservers(this);

    }

    @AfterPermissionGranted(RC_VELA_SMS)
    private void sendSMS() {
        final String samplePayload = "1:20:181702773757!48000000:08060000000:NG:t3stt3st:352085099972999";

        //send sms
        VelaSMS.send(this, samplePayload);

    }

    private void requestPermissions() {
        EasyPermissions.requestPermissions(
                new PermissionRequest.Builder(this, RC_VELA_SMS, perms)
                        .setRationale(ng.com.vela.velasms.R.string.sms_rationale)
                        .setPositiveButtonText(ng.com.vela.velasms.R.string.rationale_ask_ok)
                        .setNegativeButtonText(ng.com.vela.velasms.R.string.rationale_ask_cancel)
//                .setTheme(R.style.my_fancy_style)
                        .build()
        );
    }

    @Override
    public void hideProgress() {
        progressWrapper.setVisibility(View.GONE);
        progresShowing = false;

    }

    @Override
    public void showError(@NotNull String s) {
        Toast.makeText(this, "An Error occurred: $s", Toast.LENGTH_LONG).show();
    }

    @Override
    public void showProgress() {
        progressWrapper.setVisibility(View.VISIBLE);
        progresShowing = true;
    }

    @Override
    public void updateProgress(@NotNull String s) {
        progressUpdateTv.setText(s);

    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Timber.d("onPermissionsGranted(): Code: %s | Perms: %s", requestCode, perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Timber.d("onPermissionDenied(): Code: %s | Perms: %s", requestCode, perms);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Timber.d("onRequestPermissionsResult(): Code: %s | Perms: %s", requestCode, permissions);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}
