package com.lion.logger3d;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

public class LogService extends Service implements SensorEventListener {
    public LogService() {
    }

    private IntentFilter mIntentFilter;
    private ScreenOffReceiver mReceiver;
    private SensorManager sm;
    private boolean doWrite = false;
    private long totalTime;
    private int lines;
    private Timer mTimer;
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        totalTime = 0;
        lines = 0;
        //1, 定义锁屏的IntentFilter
        mIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        mIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        mIntentFilter.addAction(Intent.ACTION_USER_PRESENT);
        mIntentFilter.addAction(Intent.ACTION_USER_UNLOCKED);
        //2, 定义锁屏广播接收者
        mReceiver = new ScreenOffReceiver();
        //3, 注册广播接收者
        registerReceiver(mReceiver,mIntentFilter);

        startForeground();
        write2file("START\n");
        doWrite = true;
        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        try {
            FileOutputStream fout = openFileOutput("acc.txt",
                    Context.MODE_PRIVATE);
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                totalTime+=1;
            }
        }, 0, 1/* 表示1000毫秒之後，每隔1000毫秒執行一次 */);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        stopForeground(true);
        write2file("STOP\n");
        doWrite = false;
        write2file(String.valueOf(lines)+"lines in"+String.valueOf(totalTime)+"milliseconds");
        super.onDestroy();
    }

    private void write2file(String a) {
        try {
            File file = new File("/sdcard/acc.txt");//write the result into/sdcard/acc.txt
            if (!file.exists()) {
                file.createNewFile();
            }
            // Open a random access file stream for reading and writing
            RandomAccessFile randomFile = new RandomAccessFile("/sdcard/acc.txt", "rw");
            // The length of the file (the number of bytes)
            long fileLength = randomFile.length();
            // Move the file pointer to the end of the file
            randomFile.seek(fileLength);
            randomFile.writeBytes(a);
            randomFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startForeground() {

        NotificationManager service = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId ="";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId =  createNotificationChannel();
        }
        Notification.Builder notificationBuilder;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new Notification.Builder(getApplication(), channelId);
        }
        else{
            notificationBuilder = new Notification.Builder(getApplication());
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplication());
        Intent intent =new Intent (this, MainActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        //PendingIntent pendingIntent =PendingIntent.getBroadcast(getApplication(), 0, intent, 0);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Logger3D service working.")
                .setContentText("in foreground")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(1, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(){
        String channelId = "my_service";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE);

        chan.setImportance(NotificationManager.IMPORTANCE_NONE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }

    private float lowX = 0, lowY = 0, lowZ = 0;
    private final float FILTERING_VALAUE = 0.1f;
    @Override
    public void onSensorChanged(SensorEvent event) {
        String message = new String();
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float X = event.values[0];
            float Y = event.values[1];
            float Z = event.values[2];
//            //Low-Pass Filter
//            lowX = X * FILTERING_VALAUE + lowX * (1.0f -
//                    FILTERING_VALAUE);
//            lowY = Y * FILTERING_VALAUE + lowY * (1.0f -
//                    FILTERING_VALAUE);
//            lowZ = Z * FILTERING_VALAUE + lowZ * (1.0f -
//                    FILTERING_VALAUE);
//            //High-pass filter
//            float highX = X - lowX;
//            float highY = Y - lowY;
//            float highZ = Z - lowZ;
//            double highA = Math.sqrt(highX * highX + highY * highY + highZ
//                    * highZ);
            DecimalFormat df = new DecimalFormat("#,##0.000");
//            message = df.format(highX) + " ";
//            message += df.format(highY) + " ";
//            message += df.format(highZ) + " ";
//            message += df.format(highA) + "\n";
            message = String.valueOf(totalTime) + " ";
            message += df.format(X) + " ";
            message += df.format(Y) + " ";
            message += df.format(Z) + " ";
            double A = Math.sqrt(X * X + Y * Y + Z * Z);
            message += df.format(A) + "\n";
            if(doWrite){
                write2file(message);
            }
            lines+=1;

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case Intent.ACTION_SCREEN_OFF:{
                    write2file("screen off\n");
                    doWrite = false;
                    break;
                }
                case Intent.ACTION_SCREEN_ON:{
                    write2file("screen on\n");
                    doWrite = true;
                    break;
                }
//                case Intent.ACTION_USER_PRESENT:{
//                    write2file("user present\n");
//                    doWrite = true;
//                    break;
//                }
//                case Intent.ACTION_USER_UNLOCKED:{
//                    write2file("USER\n");
//                    break;
//                }
            }
//            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
//                write2file("screen off\n");
//                Log.d("cjc","清理所有进程完成");
//            }
//            else if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
//
//            }
//            else if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
//
//            }

        }
    }
}
