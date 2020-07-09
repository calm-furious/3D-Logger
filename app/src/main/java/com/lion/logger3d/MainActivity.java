package com.lion.logger3d;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button startService;
    Button stopService;
    Switch flagSwitch;

    Intent intent;
    boolean mBound = false;
    boolean log_flag = false;
    LogService mService;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LogService.MyBinder binder = (LogService.MyBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);



        flagSwitch = findViewById(R.id.switch1);
        flagSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    log_flag= true;
                    if(mBound){
                        mService.changeLogFlag(log_flag);
                        Log.d("cjc","in switch handler");
                    }
                }else {
                    log_flag= false;
                    if(mBound){
                        mService.changeLogFlag(log_flag);
                        Log.d("cjc","in switch handler");
                    }
                }
            }
        });

        startService = findViewById(R.id.start);
        intent = new Intent(MainActivity.this, LogService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                mService.changeLogFlag(log_flag);
                mService.startRecord();


                startService.setEnabled(false);
                stopService.setEnabled(true);
            }
        });
        stopService = findViewById(R.id.stop);
        stopService.setEnabled(false);
        stopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //intent = new Intent(MainActivity.this, LogService.class);

                mService.stopRecord();
//                stopService(intent);
//                unbindService(connection);
//                mBound = false;
                startService.setEnabled(true);
                stopService.setEnabled(false);
            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();
//        if(isWorked("com.lion.logger3d.LogService")){
//            startService.setEnabled(false);
//            stopService.setEnabled(true);
//            Log.d("cjc","service working");
//        }else {
//            Log.d("cjc","service not working");
//        }
    }
    @Override
    protected void onStop(){
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        //intent = new Intent(MainActivity.this, LogService.class);
        stopService(intent);
        if(mBound){
            unbindService(connection);
            mBound = false;
        }
        super.onDestroy();
    }

    private boolean isWorked(String className) {
        ActivityManager myManager = (ActivityManager) this
                .getApplicationContext().getSystemService(
                        Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);
        Log.d("cjc",String.valueOf(runningService.size()));
        for (int i = 0; i < runningService.size(); i++) {
            Log.d("cjc",runningService.get(i).service.getClassName());
            if (runningService.get(i).service.getClassName().toString()
                    .equals(className)) {
                return true;
            }
        }
        return false;
    }


    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to
     * grant permissions
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
// Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
// We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    1);
        }
    }
}
