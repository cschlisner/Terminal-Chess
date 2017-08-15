package net.schlisner.terminalchess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import static net.schlisner.terminalchess.PostOffice.listGames;
import static net.schlisner.terminalchess.PostOffice.listGamesJSON;

public class NetworkLobbyActivity extends AppCompatActivity {

    Runnable gameUpdateTask;
    Handler updateHandler = null;
    HandlerThread mHandlerThread = null;

    String uuid;
    int gameCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_network_lobby);

        uuid = getIntent().getStringExtra("uuid");

        // recolor progressbar
        ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar2);
        pb.getIndeterminateDrawable().setColorFilter(Color.parseColor("#00740c"), PorterDuff.Mode.MULTIPLY);

        try {
            gameCount = PostOffice.listGamesJSON(uuid).length();
//            System.out.println("gamecount = "+gameCount);
        } catch (Exception e){
            // couldn't list games
        }
        PostOffice.checkIn(uuid);
        gameUpdateTask = new Runnable() {
            @Override
            public void run() {
                try {
                        // update text
//                    int n = PostOffice.getOnlinePlayers();
//                    onlinePlayers.setText("Online Players: "+n);
                    JSONArray gl = PostOffice.listGamesJSON(uuid);

                    JSONObject gameJSON = (gl.length() > gameCount) ? gl.getJSONObject(0) : null;
//                    System.out.println("gamecount = "+gl.length());

                    // if we aren't wait a second and check again
                    if (gameJSON == null)
                        updateHandler.postDelayed(this, 1000);
                        // if we are start up the game and enter it
                    else {

                        Intent i = new Intent(getApplicationContext(), GameActivity.class)
                                .putExtra("opponent", GameActivity.OPPONENT_NETWORK)
                                .putExtra("uuid", uuid)
                                .putExtra("startFromExt", true)
                                .putExtra("gameJSON", gameJSON.toString());
                        startActivity(i);
                        finish();
                    }

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        mHandlerThread = new HandlerThread("HandlerThread");
        mHandlerThread.start();
        updateHandler = new Handler(mHandlerThread.getLooper());

        updateHandler.postDelayed(gameUpdateTask, 1000);

    }

    @Override
    public void onPause(){
        super.onPause();

        updateHandler.removeCallbacks(gameUpdateTask);

        PostOffice.checkOut(uuid);
    }
}
