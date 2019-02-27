package io.dcloud.feature.aps;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ApsActionService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            NotificationReceiver.sOnReceiver(getBaseContext(), intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
