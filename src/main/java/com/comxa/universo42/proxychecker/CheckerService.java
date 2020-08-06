package com.comxa.universo42.proxychecker;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.comxa.universo42.proxychecker.modelo.Proxy;
import com.comxa.universo42.proxychecker.modelo.ProxyChecker;

import java.util.List;

public class CheckerService extends Service implements CheckerControl {
    public static final String SERVICE_BROADCAST_STR = "SERVICE_PROXY_CHECKER";
    public static final int NOTINICATION_ID = 42;

    private NotificationManager notificationManager;
    private Builder notificationBuilder;

    private ServiceController controller = new ServiceController();
    private boolean isOnForeground;
    private ProxyChecker checker;


    @Override
    public IBinder onBind(Intent intent) {
        return controller;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (checker != null && checker.isRunning())
            checker.stop();
        if (notificationManager != null)
            notificationManager.cancel(NOTINICATION_ID);
    }

    private void startForeground() {
        if (isOnForeground)
            return;

        isOnForeground = true;

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.checking)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("...")
                .setContentIntent(pendingIntent);

        startForeground(NOTINICATION_ID, notificationBuilder.build());
    }

    @Override
    public ProxyChecker getChecker() {
        return checker;
    }

    @Override
    public void setChecker(List<Proxy> proxyList, int qtdThreads, String payload) {
        checker = new ProxyChecker(proxyList, qtdThreads, payload) {
            @Override
            public void onLog(String str) {
                if (notificationBuilder != null) {
                    notificationBuilder.setContentText(str);
                    notificationManager.notify(NOTINICATION_ID, notificationBuilder.build());
                }
            }

            @Override
            public void onComplete() {
                if (notificationBuilder != null) {
                    notificationBuilder.setSmallIcon(R.drawable.checked);
                    notificationManager.notify(NOTINICATION_ID, notificationBuilder.build());
                }
            }
        };
    }

    public class ServiceController extends Binder {
        public CheckerControl getControl() {
            return CheckerService.this;
        }
    }
}
