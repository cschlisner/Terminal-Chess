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
    PostOffice po;
    SharedPreferences sharedPref;
    TextView uuidView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_menu);

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        uuid = sharedPref.getString("uuid", "");
        System.out.println(uuid);
        uuidView = (TextView) findViewById(R.id.uuidView);
        uuidView.setText(uuid);
        po = new PostOffice();
        if (uuid == ""){
            System.out.println("Registering user...");
            try {
                uuid = po.register();
            } catch (Exception e){
                Toast.makeText(getApplicationContext(), "Network took too long to register user", Toast.LENGTH_SHORT).show();
            }
            System.out.println("Setting user ID to: " + uuid);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("uuid", uuid);
            editor.commit();
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
                                    uuid = po.register();
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString("uuid", uuid);
                                    editor.commit();
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
    }

    // get list of games user is enganged in, display them in a list for them to choose from
    public void resumeGame(View view){
        Intent i = new Intent(this, ResumeGameActivity.class).putExtra("uuid", uuid);
        startActivity(i);
    }

    // join game lobby to get matched, start checking for a new game in gamelist and enter it
    public void startNewNetworkGame(View view){
//        try {
//            String gameId = po.joinNewGame(uuid);
//            Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "new")
//                                                                .putExtra("opponent", "network")
//                                                                .putExtra("gameID", gameId);
//            startActivity(intent);
//        } catch (Exception e){
//            Toast.makeText(getApplicationContext(), "Network took too long", Toast.LENGTH_SHORT).show();
//        }
    }

    public void startNewLocalGame(View view){
        Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "new")
                                                            .putExtra("opponent", "local");
        startActivity(intent);
    }

    public void startNewAIGame(View view){
        Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "new")
                                                            .putExtra("opponent", "ai");
        startActivity(intent);
    }


}
