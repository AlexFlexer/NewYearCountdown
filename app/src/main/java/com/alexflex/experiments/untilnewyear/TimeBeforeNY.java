package com.alexflex.experiments.untilnewyear;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static android.os.Build.VERSION_CODES.O;

//todo добавить сервис в автозагрузку и добавить остановку по клику на кнопку
public class TimeBeforeNY extends Service {

    private char mode;
    private int nextYear;
    //используется в связке с mode
    private int interval_num;
    private Thread timer;
    private Runnable runnable;
    private NotificationManager notificationManager;

    public TimeBeforeNY(){
        nextYear = new Date().getYear() + 1;
    }

    @Override
    public void onCreate(){

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getMode();
        runnable = new Runnable() {

            @Override
            public void run() {
                notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                while (!(Thread.interrupted())) {
                    notificationManager.notify(1, workWithNotifications());
                    //сколько времени будет висеть уведомление без обновления
                    try {
                        switch (interval_num) {
                            case 0:
                                Thread.sleep(60_000);
                                break;
                            case 2:
                                Thread.sleep(1_800_000);
                                break;
                            case 3:
                                Thread.sleep(3_600_000);
                                break;
                            case 4:
                                Thread.sleep(10_800_000);
                                break;
                            case 5:
                                Thread.sleep(21_600_000);
                                break;
                            case 6:
                                Thread.sleep(43_200_000);
                                break;
                            case 7:
                                Thread.sleep(86_400_000);
                                break;
                            default:
                                Thread.sleep(900_000);
                        }
                    } catch (InterruptedException e){
                        break;
                    }
                }
            }
        };
        timer = new Thread(runnable);
        timer.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        try {
            notificationManager.cancel(1);
            SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences("MainActivity", MODE_PRIVATE).edit();
            editor.putBoolean("isServiceStarted", false);
            editor.apply();
            if (timer.isAlive()) timer.interrupt();
        } catch (NullPointerException e){
            //я хз, когда это исключение появляется, но оно появляется
        } catch (Throwable e) {
            //правильная остановка сервиса
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void getMode(){
        interval_num = getApplicationContext().getSharedPreferences("MainActivity", MODE_PRIVATE).getInt("interval", 1);
        switch (interval_num){
            case 0:
            case 1:
            case 2:
                mode = 'm';
                break;

            case 3:
            case 4:
            case 5:
            case 6:
                mode = 'h';
                break;

            case 7:
                mode = 'd';
        }
    }

    //получаем разницу в датах
    public String getDiffBetweenDates(Date date1, Date date2, char mode){
        Date difference = new Date(date2.getTime() - date1.getTime()); //искомая разница в датах
        long days = difference.getTime()/86_400_000 - 1; //разница в днях
        Calendar calendar = new GregorianCalendar();
        int hours = 23 - calendar.get(Calendar.HOUR_OF_DAY); //разница в часах
        long minutes = 60 - calendar.get(Calendar.MINUTE);


        switch (mode) {
            //раз в n минут
            case 'm':
                return  days + " " + "дней" + " " + hours + " " + "часов" + " " + minutes + " " + "минут";

            //раз в n часов
            case 'h':
                return days + " " + "дней" + " " + hours + " " + "часов";

            //раз в сутки
            case 'd':
                return days + " " + "дней";
        }

        //todo осторожно, помни о NullPointerException
        return null;
    }

    public Notification workWithNotifications(){
        RemoteViews remoteViews;
        switch (getApplicationContext().getSharedPreferences("MainActivity", MODE_PRIVATE).getInt("selectedImage", R.id.a)){
            case R.id.b:
                remoteViews = new RemoteViews(getPackageName(), R.layout.notification_2);
                break;
            case R.id.c:
                remoteViews = new RemoteViews(getPackageName(), R.layout.notification_3);
                break;
            default:
                remoteViews = new RemoteViews(getPackageName(), R.layout.notification_1);
        }
        remoteViews.setTextViewText(R.id.title, "До Нового года осталось:");
        remoteViews.setTextViewText(R.id.timer, getDiffBetweenDates(new Date(), new Date(nextYear, 0, 1), mode));
        NotificationCompat.Builder builder;
        if(Build.VERSION.SDK_INT>=O) {
            builder = new NotificationCompat.Builder(getApplicationContext(), "forTime");
        } else {
            builder = new NotificationCompat.Builder(getApplicationContext());
        }
        builder.setContent(remoteViews);
        builder.setSmallIcon(R.drawable.the_fir);
        builder.setOngoing(true);

        //при нажатии на уведомление возвращаемся обратно в приложение
        PendingIntent backToApp = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(backToApp);

        return builder.build();
    }
}
