package net.schlisner.terminalchess;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

public class NetworkLobbyActivity extends AppCompatActivity {

    Runnable gameUpdateTask;
    Handler updateHandler = new Handler();

    String uuid;

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

        final TextView onlinePlayers = (TextView) findViewById(R.id.usersInLobbyText);

        PostOffice.checkIn(uuid);

        gameUpdateTask = new Runnable() {
            @Override
            public void run() {
                try {
                        // update text
//                    int n = PostOffice.getOnlinePlayers();
//                    onlinePlayers.setText("Online Players: "+n);

                        // check to see if we are in a new game
//                    String gameId = PostOffice.inNewGame(uuid);

                        // if we aren't wait a second and check again
//                    if (gameId == null)
//                        updateHandler.postDelayed(this, 1000);
                        // if we are start up the game and enter it
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

        updateHandler.postDelayed(gameUpdateTask, 1000);
    }
}
