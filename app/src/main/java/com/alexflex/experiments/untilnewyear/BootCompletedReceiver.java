package com.alexflex.experiments.untilnewyear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//автозагрузка уведомлений
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
       if(context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getBoolean("isServiceStarted", false)) {
           intent = new Intent(context, TimeBeforeNY.class);
           context.startService(intent);
       }
    }
}
