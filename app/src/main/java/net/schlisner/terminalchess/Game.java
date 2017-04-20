package net.schlisner.terminalchess;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import uniChess.Move;
import uniChess.Player;

public class Game extends AppCompatActivity {

    uniChess.Game chessGame;
    Player<String> whitePlayer = new Player<>("WHITE", uniChess.Game.Color.WHITE);
    Player<String> blackPlayer = new Player<>("BLACK", uniChess.Game.Color.BLACK);
    BoardView boardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.hide();

        Intent menuIntent = getIntent();

        if (menuIntent.getStringExtra("FRESH_MEMES").equals("new"))
            chessGame = new uniChess.Game(whitePlayer, blackPlayer);

        else chessGame = new uniChess.Game(whitePlayer, blackPlayer, menuIntent.getStringExtra("FRESH_MEMES"));

        //uniChess.Game.useDarkChars = true;

        setContentView(R.layout.activity_game);

        boardView = (BoardView) findViewById(R.id.boardviewer);
        boardView.setBoard(chessGame.getCurrentBoard());

        boardView.setOnTouchListener(new View.OnTouchListener() {
            int rank, file;
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (v instanceof BoardView) {
                    BoardView bV = (BoardView) v;
                    float x = e.getX();
                    float y = e.getY();
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            rank = (int) Math.floor(x / bV.tileDim);
                            file = (int) Math.floor(y / bV.tileDim);
                            break;
                        case MotionEvent.ACTION_UP:
                            Move selection = bV.selectTile(rank, file, chessGame.getCurrentPlayer());
                            if (selection != null){
                                gameAdvance(selection.getANString());
                            }
                            break;
                    }
                    return true;
                }
                return true;
            }
        });
    }

    private void gameAdvance(String in){

        uniChess.Game.GameEvent gameResponse = chessGame.advance(in);
        boardView.setBoard(chessGame.getCurrentBoard());

        switch(gameResponse){
            case CHECK:
                Toast.makeText(getApplicationContext(), "You are in check!", Toast.LENGTH_SHORT).show();
                break;
            case OK:
                Toast.makeText(getApplicationContext(), "Waiting for opponent...", Toast.LENGTH_SHORT).show();
                break;
            case CHECKMATE:
                Toast.makeText(getApplicationContext(), "Checkmate! You lose!", Toast.LENGTH_SHORT).show();
                break;
            case STALEMATE:
                Toast.makeText(getApplicationContext(), "Stalemate! Game ends in draw!", Toast.LENGTH_SHORT).show();
                break;
            case DRAW:
                Toast.makeText(getApplicationContext(), "Draw!", Toast.LENGTH_SHORT).show();
                break;
            default:
                // Save game, exit to menu
                break;
        }
    }
}
