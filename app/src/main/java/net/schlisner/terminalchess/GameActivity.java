package net.schlisner.terminalchess;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.UUID;

import uniChess.*;

public class GameActivity extends AppCompatActivity {

    Game chessGame;

    Player<String> playerOne;
    Player<String> playerTwo;

    UUID uuid;

    String opponentType = "";

    BoardView boardView;
    TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.hide();

        setContentView(R.layout.activity_game);

        boardView = (BoardView) findViewById(R.id.boardviewer);
        statusView = (TextView) findViewById(R.id.aiOutTextView);

        Intent menuIntent = getIntent();
        opponentType = menuIntent.getStringExtra("opponent");
        if (menuIntent.getStringExtra("init_mode").equals("new")){
            // decide which colorz
            boolean white = (new Random(System.currentTimeMillis())).nextBoolean();
            switch (opponentType){
                case "local":
                    // both players interact with the game in the same way
                    playerOne = new Player<>("WHITE", Game.Color.WHITE);
                    playerTwo = new Player<>("BLACK", Game.Color.BLACK);
                    break;
                case "network":
                    playerOne = new HTTPNetworkPlayer<>(uuid.toString(), white ? Game.Color.WHITE : Game.Color.BLACK);

                    statusView.setText(uuid.toString());

                    playerTwo = ((HTTPNetworkPlayer)playerOne).findOpponent();

                    if (!white) {
                        boardView.flipBoard();
                        // find other player in db and create game
//                        r.db("chess").table('games').insert({
//                                "id": "test",
//                                "player_to_move": "WHITE",
//                                "moves": []
//                        })
                    }
                    break;
                case "ai":
                    Toast.makeText(getApplicationContext(), String.format("You will be playing as %s", white ? "white" : "black"), Toast.LENGTH_SHORT).show();

                    playerTwo = white ? new Chesster<>("BLACK", Game.Color.BLACK)
                                        : new Player<>("BLACK", Game.Color.BLACK);
                    playerOne = white ? new Player<>("WHITE", Game.Color.WHITE)
                                        : new Chesster<>("WHITE", Game.Color.WHITE);

                    break;
            }

            chessGame = new uniChess.Game(playerOne, playerTwo);

        }
        else if (menuIntent.getStringExtra("init_mode").equals("resume")){
            // load saved uniChess.GameActivity
            String gameString = "actually_load_a_saved_thing_here";

            playerTwo = new Player<>("BLACK", Game.Color.BLACK);
            playerOne = new Player<>("WHITE", Game.Color.WHITE);
            chessGame = new uniChess.Game(playerOne, playerTwo, gameString);
        }

        else chessGame = new uniChess.Game(playerOne, playerTwo, menuIntent.getStringExtra("FRESH_MEMES"));

        //uniChess.Game.useDarkChars = true;

        boardView.setBoard(chessGame.getCurrentBoard());

        statusView.setText(opponentType);

        boardView.updateValidMoves();

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
                            break;
                        case MotionEvent.ACTION_UP:
                            Move selection = bV.selectTile((int)x, (int)y, chessGame.getCurrentPlayer());
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
        Game.GameEvent gameResponse = chessGame.advance(in);
        boardView.setBoard(chessGame.getCurrentBoard());

        String responseMove = null;
        switch(gameResponse){
            case CHECK:
                Toast.makeText(getApplicationContext(), "You are in check!", Toast.LENGTH_SHORT).show();
            case OK:
                boardView.updateValidMoves();

                switch (opponentType){

                    case "local":
                        boardView.flipBoard();
                        Toast.makeText(getApplicationContext(), (chessGame.getCurrentPlayer().color)+" to move", Toast.LENGTH_SHORT).show();
                        break;

                    case "network":
                        ((HTTPNetworkPlayer)chessGame.getDormantPlayer()).sendMoveAN(in);
                        Toast.makeText(getApplicationContext(), "Waiting for opponent...", Toast.LENGTH_SHORT).show();
                        //responseMove = ((HTTPNetworkPlayer)chessGame.getCurrentPlayer()).getMoveAN();
                        break;

                    case "ai":
                        responseMove = ((Chesster) chessGame.getCurrentPlayer()).getMove().getANString();
                        break;
                }
                break;
            case CHECKMATE:
                Toast.makeText(getApplicationContext(), "Checkmate! You lose!", Toast.LENGTH_SHORT).show();
                break;
            case STALEMATE:
                Toast.makeText(getApplicationContext(), "Stalemate! GameActivity ends in draw!", Toast.LENGTH_SHORT).show();
                break;
            case DRAW:
                Toast.makeText(getApplicationContext(), "Draw!", Toast.LENGTH_SHORT).show();
                break;
            default:
                // Save game, exit to menu
                break;
        }
        if (responseMove != null){
            chessGame.advance(responseMove);
            boardView.setBoard(chessGame.getCurrentBoard());
        }
    }
}
