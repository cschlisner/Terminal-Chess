package net.schlisner.terminalchess;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;

import com.rethinkdb.ast.query.gen.Json;

import org.json.JSONArray;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by cschl_000 on 7/26/2017.
 */

public class ChessUpdater extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            this.setAlarm(context);
            return;
        }
        try {
            SharedPreferences sharedPref = context.getSharedPreferences("Dank Memes(c)", Context.MODE_PRIVATE);
            String savedGameList = sharedPref.getString("savedGames", null);
            String uuid = sharedPref.getString("uuid", "");

            JSONArray savedGames = new JSONArray(savedGameList);
            JSONArray games = PostOffice.listGamesJSON(uuid);

            boolean newGame = savedGameList.length() != games.length();

            if (newGame){
                //idk
            }
            else {
                int newMoves = 0;
                int game = 0;

                for (int i = 0; i < games.length(); ++i){
                    if (games.getJSONObject(i).getJSONArray("moves").length() >
                            savedGames.getJSONObject(i).getJSONArray("moves").length()) {
                        ++newMoves;
                        game = i;
                    }
                }

                if (newMoves > 0){
                    Intent notificationIntent;
                    String text;
                    if (newMoves == 1) {
                        JSONArray mv = games.getJSONObject(game).getJSONArray("moves");
                        text = mv.getString(mv.length()-1);

                        notificationIntent = new Intent(context, GameActivity.class);
                        notificationIntent.putExtra("uuid", uuid);
                        notificationIntent.putExtra("gameJSON", games.getJSONObject(game).toString());
                        notificationIntent.putExtra("opponent", "network");
                    }
                    else {
                        text = "Multiple attacls underway.";
                        notificationIntent = new Intent(context, ResumeGameActivity.class);
                        notificationIntent.putExtra("uuid", uuid);
                    }


                    PendingIntent pIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

// build notification
// the addAction re-use the same intent to keep the example short
                    Notification n  = new Notification.Builder(context)
                            .setContentTitle("Opponent has advanced.")
                            .setContentText(text)
                            .setColor(ContextCompat.getColor(context, R.color.chessBoardHighlight))
                            .setSmallIcon(R.drawable.ic_stat_)
                            .setContentIntent(pIntent)
                            .setAutoCancel(true).build();


                    NotificationManager notificationManager =
                            (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

                    notificationManager.notify(0, n);

                }
            }
        } catch (Exception e){
            System.out.println("Welp. I tried.");
        }
    }

    public void setAlarm(Context context)
    {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, ChessUpdater.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 1, pi); // Millisec * Second * Minute
    }

    public void cancelAlarm(Context context)
    {
        Intent intent = new Intent(context, ChessUpdater.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
