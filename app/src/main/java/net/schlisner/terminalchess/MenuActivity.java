package net.schlisner.terminalchess;

import android.content.Intent;
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
    TextView opponentIp;
    String ip;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_menu);

        TextView tv = (TextView)findViewById(R.id.deviceIP);
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        tv.setText(ip);

        opponentIp = (TextView) findViewById(R.id.ipText);

    }

    public void resumeGame(View view){
        Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "resume");
        startActivity(intent);
    }

    public void startNewNetworkGame(View view){
        Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "new")
                                                            .putExtra("opponent", "network")
                                                            .putExtra("self_ip", ip)
                                                            .putExtra("opponent_ip", String.valueOf(opponentIp.getText()));
        startActivity(intent);
    }

    public void startNewLocalGame(View view){
        Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "new").putExtra("opponent", "local");
        startActivity(intent);
    }

    public void startNewAIGame(View view){
        Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "new").putExtra("opponent", "ai");
        startActivity(intent);
    }


}
