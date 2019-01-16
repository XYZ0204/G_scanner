package gomaji.com.g_scanner;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;

import static android.Manifest.permission.CAMERA;

public class QrCodeScannerActivity extends AppCompatActivity implements ScanInteractorImpl.ScanCallbackInterface {

    private static final int REQUEST_CAMERA = 1;

    //bundle key
    public static final String BUNDLE_HANDLE_WEBVIEW = "BUNDLE_HANDLE_WEBVIEW";
    public static final String BUNDLE_SCAN_CALLBACK = "BUNDLE_SCAN_CALLBACK";
    public static final String BUNDLE_HAS_FLASHLIGHT = "BUNDLE_HAS_FLASHLIGHT";
    public static final String BUNDLE_TITLE = "BUNDLE_TITLE";

    private boolean mHandleWebView;//掃描完成後是否要顯示webView

    private FrameLayout mCapturePreview;
    private CheckBox mFlashlightBtn;
    private TextView mTvTitle;
    private ScanInteractor scanInteractor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_g_scanner);

        scanInteractor = new ScanInteractorImpl(this);
        mCapturePreview = (FrameLayout) findViewById(R.id.capture_preview);
        mTvTitle = (TextView) findViewById(R.id.tv_title);

        //閃光燈
        mFlashlightBtn = (CheckBox) findViewById(R.id.toggle_flashlight);
        mFlashlightBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    scanInteractor.openFlash();
                } else {
                    scanInteractor.closeFlash();
                }

            }
        });

        //關閉按鈕
        findViewById(R.id.close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        handleBundleData();

    }

    private void handleBundleData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {

            mHandleWebView = bundle.getBoolean(BUNDLE_HANDLE_WEBVIEW, true);

            String title = bundle.getString(BUNDLE_TITLE, "scan QRcode");
            mTvTitle.setText(title);

            boolean hasFlashLight = bundle.getBoolean(BUNDLE_HAS_FLASHLIGHT, true);
            mFlashlightBtn.setVisibility(hasFlashLight ? View.VISIBLE : View.GONE);
        }
    }

    private boolean checkPermission() {
        return (ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, REQUEST_CAMERA);
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (grantResults.length > 0) {

                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (!cameraAccepted) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(CAMERA)) {
                                showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                requestPermissions(new String[]{CAMERA}, REQUEST_CAMERA);
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(QrCodeScannerActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();

        int currentapiVersion = Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.M) {
            if (checkPermission()) {
                mCapturePreview.removeAllViews();
                scanInteractor.initScan(mCapturePreview);
                scanInteractor.startPreview();

            } else {
                requestPermission();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        scanInteractor.stopPreview();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scanInteractor.stopPreview();
    }

    @Override
    public void receiveResult(String result) {
        if (mHandleWebView) {
            //way1
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result));
            startActivity(browserIntent);
        } else {
            //way2
            Intent intent = new Intent();
            intent.putExtra(BUNDLE_SCAN_CALLBACK, result);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}