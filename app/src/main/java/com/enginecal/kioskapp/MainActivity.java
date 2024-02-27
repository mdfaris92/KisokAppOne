package com.enginecal.kioskapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.os.UserManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity implements BatteryStateReceiver {

    public Button mButton;
    Button checkUpdate;

    private View mDecorView;
    private DevicePolicyManager mDpm;
    public static boolean mIsKioskEnabled = false;
    TextView mTextField;
    ImageView imageView;
    CountDownTimer countDownTimer;

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;

    private static final String KIOSK_PACKAGE = "com.enginecal.kioskapp";
    private static final String APP_PLAYER_PACKAGE = "com.example.player";
    private static final String[] APP_PACKAGES = {KIOSK_PACKAGE, APP_PLAYER_PACKAGE};

    private static final String CHANNEL_ID = "eva_channel_01";
    private static final String CHANNEL_NAME = "eva_channel_dock_mode";


    private PowerManager.WakeLock wakeLock;
    private static final int REQUEST_DISABLE_KEYGUARD = 1;



    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        checkUpdate = findViewById(R.id.button_check_update);
        mButton = findViewById(R.id.button_toggle_kiosk);
        mTextField = findViewById(R.id.mTextField);
        imageView = findViewById(R.id.imageView);

        findViewById(R.id.button_toggle_kiosk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableKioskMode(!mIsKioskEnabled);
            }
        });

        ComponentName deviceAdmin = new ComponentName(this, AdminReceiver.class);
        mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        String[] restrictions = {
                UserManager.DISALLOW_FACTORY_RESET,
                UserManager.DISALLOW_SAFE_BOOT,
                UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA,
                UserManager.DISALLOW_ADD_USER,
                UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS,
                };


//        UserManager.DISALLOW_ADJUST_VOLUME

        if (!mDpm.isAdminActive(deviceAdmin)) {

            Toast.makeText(this, getString(R.string.not_device_admin), Toast.LENGTH_SHORT).show();
        }

        if (mDpm.isDeviceOwnerApp(getPackageName())) {
            for (String restriction: restrictions) {
                mDpm.addUserRestriction(deviceAdmin, restriction);
            }
            mDpm.setLockTaskPackages(deviceAdmin, new String[]{getPackageName()});
        } else {
            Toast.makeText(this, getString(R.string.not_device_owner), Toast.LENGTH_SHORT).show();
        }

        mDecorView = getWindow().getDecorView();

        countDownTimer = new CountDownTimer(3000000, 1000) {

            public void onTick(long millisUntilFinished) {
                mTextField.setText("Timer: " + millisUntilFinished / 1000);
                // logic to set the EditText could go here
            }

            public void onFinish() {
                mTextField.setText("done!");
                countDownTimer.start();
            }

        }.start();

        //startService(new Intent(this, MyPhoneStateListener.class));
        Intent i = new Intent(MainActivity.this, IncomingCallReceiver.class);
        sendBroadcast(i);

        // Call the method to cancel all notifications from your app
        cancelAllNotifications();

        // Call the method to disable heads-up notifications
        disableHeadsUpNotifications();

        // Register screen receiver
        PowerButtonReceiver screenReceiver = new PowerButtonReceiver(new Handler(),MainActivity.this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);

        BatteryReceiver batteryReceiver = new BatteryReceiver(this,this);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(Intent.ACTION_POWER_CONNECTED);
        filter2.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(batteryReceiver,filter2);


    }

    private void disableHeadsUpNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create or retrieve the notification channel
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }

            // Disable heads-up notifications
            channel.setVibrationPattern(new long[]{0});
            channel.enableVibration(true);
            channel.setSound(null, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();

        Intent i = new Intent(MainActivity.this, IncomingCallReceiver.class);
        sendBroadcast(i);
        if(mIsKioskEnabled)
            hideSystemUI();
        else
            Log.e("Test","test");

    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        | View.SCREEN_STATE_ON|View.KEEP_SCREEN_ON);
    }

    private void enableKioskMode(boolean enabled) {
        try {
            if (enabled) {
                if (mDpm.isLockTaskPermitted(this.getPackageName())) {
                   /* CallHelper callHelper = new CallHelper(MainActivity.this);
                    callHelper.endCall();*/
                    /*if(isLocalVoiceInteractionSupported()){
                        stopLocalVoiceInteraction();
                    }*/

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.DISABLE_KEYGUARD)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Permission is not granted
                        // Request the permission
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.DISABLE_KEYGUARD},
                                REQUEST_DISABLE_KEYGUARD);
                    } else {
                        // Permission has been granted
                        disableKeyguard();
                    }


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) {
                            startLockTask();
                            // Keep the screen on
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


                        }
                    }
                    mIsKioskEnabled = true;
                    mButton.setText(getString(R.string.exit_kiosk_mode));
                } else {
                    Toast.makeText(this, getString(R.string.kiosk_not_permitted), Toast.LENGTH_SHORT).show();
                }
            } else {
                stopLockTask();
                mIsKioskEnabled = false;
                mButton.setText(getString(R.string.enter_kiosk_mode));
            }
        } catch (Exception e) {
            // TODO: Log and handle appropriately
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Intercept key events and prevent them from reaching the system
        if(mIsKioskEnabled) {
            int keyCode = event.getKeyCode();

           /*if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
                // Power button pressed
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    // Acquire wake lock
                    acquireWakeLock();
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    // Release wake lock
                    releaseWakeLock();
                }
                return true; // Consume the event
            }*/
             if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ) {
                // Handle the key event within your app
                //Toast.makeText(MainActivity.this, "hi", Toast.LENGTH_SHORT).show();
                return true; // Consume the event and prevent it from being dispatched to the system
            }

            /*else if(keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_DPAD_UP){
                Toast.makeText(MainActivity.this, "hiii", Toast.LENGTH_SHORT).show();
                //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                *//*Intent iPower =new Intent(MainActivity.this,PowerButtonReceiver.class);
                sendBroadcast(iPower);*//*
                return true;
            }*/
        }
        return super.dispatchKeyEvent(event);
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "MyApp:WakeLockTag");
        wakeLock.acquire(10*60*1000L /*10 minutes*/);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private void requestDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, AdminReceiver.class));
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This app needs admin rights to restrict the power button.");
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == Activity.RESULT_OK) {
                // Device admin activated successfully
                // Now enforce device policies
                enforceDevicePolicies();
            } else {
                // User didn't activate device admin
                // Handle accordingly
            }
        }
    }

    private void enforceDevicePolicies() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, AdminReceiver.class);

        // Restrict power button
        /*devicePolicyManager.setGlobalSetting(adminComponent,
                Settings.Global.POWER_BUTTON_BEHAVIOR,
                Integer.toString(DevicePolicyManager.POWER_BUTTON_DISABLED));*/

        // Restrict power button
        /*devicePolicyManager.setGlobalSetting(adminComponent,
                DevicePolicyManager.POLICY_DISABLE_POWER_KEY, "1");*/

        // Check if the app has permission to write settings
        /*if (Settings.System.canWrite(MainActivity.this)) {
            // Change power button behavior
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            Settings.Global.putInt(getContentResolver(), Settings.Global., Integer.toString(DevicePolicyManager.POWER_BUTTON_DISABLED));
        } else {
            // If the app doesn't have permission, request it from the user
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            startActivity(intent);
        }*/
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //if(mIsKioskEnabled) {
        Log.d("keyDown",event.getKeyCode()+"");
            if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
                // Handle power button press
                // You might show a dialog confirming the action
                //Toast.makeText(MainActivity.this, "hiii22", Toast.LENGTH_SHORT).show();
                //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Intent iPower =new Intent(MainActivity.this,PowerButtonReceiver.class);
                sendBroadcast(iPower);
                return true; // Consume the event
            }
        //}
        return super.onKeyDown(keyCode, event);
    }

    public void screenOn(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void cancelAllNotifications() {
        // Get the NotificationManager instance
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Cancel all notifications from your app
        notificationManager.cancelAll();
    }


    private void disableKeyguard() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("kioskApp:DisableKeyguard");
            keyguardLock.disableKeyguard();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_DISABLE_KEYGUARD) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted
                disableKeyguard();
            } else {
                Log.d("permission_scrren","acces");
                // Permission denied
                // Handle accordingly
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Do nothing (disable back button)

    }

    @Override
    public void batteryStatus(boolean isCharged) {
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        final int orientation = display.getOrientation();
        if(isCharged){
            enableKioskMode(true);

//            imageView.setImageResource(R.drawable.logo_stamp);
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        }else{
            enableKioskMode(false);
//            imageView.setImageResource(R.drawable.logo);
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }
}