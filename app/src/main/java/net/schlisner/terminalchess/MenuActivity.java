package net.schlisner.terminalchess;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MenuActivity extends AppCompatActivity {
    String uuid;
    PostOffice po;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_menu);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        uuid = sharedPref.getString("uuid", "");
    }

    // get list of games user is enganged in, display them in a list for them to choose from
    public void resumeGame(View view){
        if (uuid == ""){
            // register with server
            uuid = po.register();
        }
        Intent intent = new Intent(this, ResumeGameActivity.class);
    }

    // join game lobby to get matched, start checking for a new game in gamelist and enter it
    public void startNewNetworkGame(View view){
        Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "new")
                                                            .putExtra("opponent", "network");
        startActivity(intent);
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
