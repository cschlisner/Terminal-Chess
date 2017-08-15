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
import static net.schlisner.terminalchess.GameActivity.OPPONENT_NETWORK;

/**
 * Created by cschl_000 on 7/26/2017.
 */

public class ChessUpdater extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            setAlarm(context);
            return;
        }
        try {
            SharedPreferences sharedPref = context.getSharedPreferences("Dank Memes(c)", Context.MODE_PRIVATE);
            String savedGameList = sharedPref.getString("savedGames", null);
            String uuid = sharedPref.getString("uuid", "");

            System.out.println("________\nUpdating Games for user: "+uuid);


            JSONArray savedGames = new JSONArray(savedGameList);
            JSONArray games = PostOffice.listGamesJSON(uuid);
            boolean newGame = savedGames.length() < games.length();

            System.out.format("Saved Games: %s\n", savedGames.length());
            System.out.format("Network Games: %s\nNew: %s\n", games.length(), games.length() - savedGames.length());

            if (newGame){
                System.out.println("New game: \n"+games.getJSONObject(games.length()-1).toString(4));
            }
            else {
                int newMoves = 0;
                boolean drawOffered = false, drawAccepted = false;
                JSONObject drawnGame = null;
                JSONObject game = null;

                for (int i = 0; i < games.length(); ++i){
                    JSONObject netGame = games.getJSONObject(i);
                    JSONObject savedGame = savedGames.getJSONObject(i);

                    if (netGame.getJSONArray("moves").length() > savedGame.getJSONArray("moves").length()){
                        ++newMoves;
                        game = netGame;
                        System.out.println("Modified game:\n"+game.toString(4));
                    }
                    boolean userIsWhite = PostOffice.isWhite(netGame.getString("white_md5uuid"), uuid);
                    if (netGame.optBoolean((userIsWhite ? "black_draw" : "white_draw"), false)) {
                        drawOffered = true;
                        drawnGame = netGame;
                        if (savedGame.optBoolean((!userIsWhite ? "black_draw" : "white_draw"), false))
                            drawAccepted = true;
                    }
                }

                if (newMoves > 0 || drawOffered){
                    Intent notificationIntent;
                    String text;

                    // Starting wrong game??
                    if (newMoves == 0){
                        text = (drawAccepted) ? "Draw Accepted." : "Draw Offered.";
                        notificationIntent = new Intent(context, GameActivity.class);
                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        notificationIntent.putExtra("uuid", uuid);
                        notificationIntent.putExtra("gameJSON", game.toString());
                        notificationIntent.putExtra("opponent", OPPONENT_NETWORK);
                    }
                    else if (newMoves == 1 && !drawOffered) {
                        JSONArray mv = game.getJSONArray("moves");
                        String an = mv.getString(mv.length() - 1);
                        Piece p = Piece.synthesizePiece(Character.toUpperCase(an.charAt(0)));
                        text = p.getSymbol() + " -> " + an.substring(3);

                        notificationIntent = new Intent(context, GameActivity.class);
                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        notificationIntent.putExtra("uuid", uuid);
                        notificationIntent.putExtra("gameJSON", game.toString());
                        notificationIntent.putExtra("opponent", OPPONENT_NETWORK);
                    }
                    else {
                        text = "Attacks underway.";
                        notificationIntent = new Intent(context, ResumeGameActivity.class);
                        notificationIntent.putExtra("uuid", uuid);
                    }

                    PendingIntent pIntent = PendingIntent.getActivity(context, 667, notificationIntent,
                                                PendingIntent.FLAG_CANCEL_CURRENT);

                    Notification n  = new Notification.Builder(context)
                            .setContentTitle((newMoves > 1) ? "Opponents have advanced.":"Opponent has advanced.")
                            .setContentText(text)
                            .setLights(ContextCompat.getColor(context, R.color.chessBoardHighlight), 500, 1000)
                            .setColor(ContextCompat.getColor(context, R.color.chessBoardDark))
                            .setSmallIcon(R.drawable.ic_stat_)
                            .setContentIntent(pIntent)
                            .setAutoCancel(true).build();


                    NotificationManager notificationManager =
                            (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

                    notificationManager.notify(665, n);

                }

            }
        } catch (Exception e){
            System.out.println("");
        }
    }

    public static final String intentAction = "net.schlisner.TerminalChess.ChessUpdate";

    public static void setAlarm(Context context)
    {
        System.out.println("Alarm Set");
        Intent i = new Intent(intentAction);
        i.setClass(context, ChessUpdater.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 666, i, 0);

        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime()+1000, 1000*10, pi); // Millisec * Second * Minute
    }

    public static void cancelAlarm(Context context)
    {
        System.out.println("Alarm Cancelled");
        Intent i = new Intent(intentAction);
        i.setClass(context, ChessUpdater.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 666, i, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
        sender.cancel();
    }
}
