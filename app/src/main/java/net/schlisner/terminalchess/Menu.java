package net.schlisner.terminalchess;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Menu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        setContentView(R.layout.activity_menu);

        Button newGameBtn = (Button) this.findViewById(R.id.newGameButton);

    }

    public void startNewGame(View view){
        Intent intent = new Intent(this, Game.class).putExtra("FRESH_MEMES", "new");
        startActivity(intent);
    }
}
