package net.schlisner.terminalchess;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_menu);
    }

    public void resumeGame(View view){
        Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "resume");
        startActivity(intent);
    }

    public void startNewNetworkGame(View view){
        Intent intent = new Intent(this, GameActivity.class).putExtra("init_mode", "new").putExtra("opponent", "network");
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
