package net.schlisner.terminalchess;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;

import com.rethinkdb.ast.query.gen.Json;

import org.json.JSONArray;
import org.json.JSONObject;

import uniChess.Move;
import uniChess.Piece;

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

            System.out.println("Updating Games uuid="+uuid);
            JSONArray savedGames = new JSONArray(savedGameList);
            JSONArray games = PostOffice.listGamesJSON(uuid);

            boolean newGame = savedGames.length() != games.length();

            if (newGame){

                System.out.println("nice meme: ");
            }
            else {
                int newMoves = 0;
                JSONObject game = null;

                for (int i = 0; i < games.length(); ++i){
                    JSONObject netGame = games.getJSONObject(i);
                    JSONObject savedGame = savedGames.getJSONObject(i);
                    System.out.format("network: %s\nsaved: %s", netGame, savedGame);

                    if (netGame.getJSONArray("moves").length() > savedGame.getJSONArray("moves").length()){
                        ++newMoves;
                        game = netGame;
                    }

                }

                if (newMoves > 0){
                    Intent notificationIntent;
                    String text;

                    // Starting wrong game??
//                    if (newMoves == 1) {
//                        JSONArray mv = game.getJSONArray("moves");
//
//                        String an = mv.getString(mv.length()-1);
//                        Piece p = Piece.synthesizePiece(an.charAt(0));
//                        text = p.getSymbol()+" -> "+an.substring(3);
//
//                        notificationIntent = new Intent(context, GameActivity.class);
//                        notificationIntent.putExtra("uuid", uuid);
//                        notificationIntent.putExtra("gameJSON", game.toString());
//                        notificationIntent.putExtra("opponent", "network");
//                    }
//                    else {
                        text = "Attacks underway.";
                        notificationIntent = new Intent(context, ResumeGameActivity.class);
                        notificationIntent.putExtra("uuid", uuid);
//                    }


                    PendingIntent pIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

// build notification
// the addAction re-use the same intent to keep the example short
                    Notification n  = new Notification.Builder(context)
                            .setContentTitle((newMoves > 1) ? "Opponents have advanced.":"Opponent has advanced.")
                            .setContentText(text)
                            .setLights(ContextCompat.getColor(context, R.color.chessBoardHighlight), 500, 1000)
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
        System.out.println("Alarm Set");
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, ChessUpdater.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime()+1000, 1000*60, pi); // Millisec * Second * Minute
    }

    public void cancelAlarm(Context context)
    {
        Intent intent = new Intent(context, ChessUpdater.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
