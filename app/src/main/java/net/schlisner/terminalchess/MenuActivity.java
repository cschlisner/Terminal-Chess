package net.schlisner.terminalchess;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.List;
import java.util.concurrent.TimeoutException;

import uniChess.Game;
import uniChess.Player;

public class MenuActivity extends AppCompatActivity {
    String uuid;
    SharedPreferences sharedPref;
    TextView uuidView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_menu);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        sharedPref = this.getSharedPreferences("Dank Memes(c)", Context.MODE_PRIVATE);
        uuid = sharedPref.getString("uuid", "");
        System.out.println(uuid);

        uuidView = (TextView) findViewById(R.id.uuidView);
        uuidView.setText(uuid);

        TextView score_w = (TextView) findViewById(R.id.score_wins);
        score_w.setText(String.valueOf(sharedPref.getInt("score_w", 0)));
        TextView score_l = (TextView) findViewById(R.id.score_losses);
        score_l.setText(String.valueOf(sharedPref.getInt("score_l", 0)));
        TextView score_d = (TextView) findViewById(R.id.score_draws);
        score_d.setText(String.valueOf(sharedPref.getInt("score_d", 0)));

        if (uuid.equals("")){
            System.out.println("Registering user...");
            try {
                uuid = PostOffice.register();
                PostOffice.checkOut(uuid);
            } catch (Exception e){
                Toast.makeText(getApplicationContext(), "Network took too long to register user", Toast.LENGTH_SHORT).show();
            }
            System.out.println("Setting user ID to: " + uuid);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("uuid", uuid);
            editor.apply();
            uuidView.setText(uuid);
        }
        uuidView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final View tv = v;
                AlertDialog.Builder adbuilder = new AlertDialog.Builder(MenuActivity.this);
                adbuilder.setTitle("Assume New Identity?")
                        .setMessage("This will forfeit all current games.")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    uuid = PostOffice.register();
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString("uuid", uuid);
                                    editor.apply();
                                    ((TextView)tv).setText(uuid);
                                } catch (Exception e){
                                    Toast.makeText(getApplicationContext(), "Network took too long to register user", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                AlertDialog ad = adbuilder.create();
                ad.show();
                return true;
            }
        });

        // TODO: load user-set font ?
        FontManager.setFont(getApplicationContext(), "seguisym.ttf");

    }

    // get list of games user is enganged in, display them in a list for them to choose from
    public void resumeGame(View view){
        Intent i = new Intent(this, ResumeGameActivity.class).putExtra("uuid", uuid);
        startActivity(i);
    }

    // go to networking menu
    public void startNewNetworkGame(View view){
        Intent i = new Intent(this, NetworkGameMenu.class);
        startActivity(i);
    }

    // start local game
    public void startNewLocalGame(View view){
        Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "new")
                                                            .putExtra("opponent", GameActivity.OPPONENT_LOCAL);
        startActivity(intent);
    }

    // start local AI game
    public void startNewAIGame(View view){
        Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "new")
                                                            .putExtra("opponent", GameActivity.OPPONENT_AI);
        startActivity(intent);
    }

    @Override
    public void onPause(){
        super.onPause();
        ChessUpdater.setAlarm(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        ChessUpdater.cancelAlarm(this);
    }
}
