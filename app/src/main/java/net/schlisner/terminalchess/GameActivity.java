package net.schlisner.terminalchess;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;

import uniChess.*;

public class GameActivity extends AppCompatActivity {

    Game chessGame;


    String uuid;
    boolean userIsWhite;

    String opponentType = "";

    BoardView boardView;
    TextView deathRowOpponent;
    TextView deathRowUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long t1 = System.currentTimeMillis();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.hide();

        setContentView(R.layout.activity_game);

        boardView = (BoardView) findViewById(R.id.boardviewer);
        deathRowOpponent = (TextView) findViewById(R.id.deathRowOpponent);
        deathRowUser = (TextView) findViewById(R.id.deathRowUser);

        Intent menuIntent = getIntent();

        opponentType = menuIntent.getStringExtra("opponent");
        boolean white = (new Random(System.currentTimeMillis())).nextBoolean();

        Player<String> playerOne;
        Player<String> playerTwo;

        switch (opponentType){
            case "local":
                playerOne = new Player<>("WHITE", Game.Color.WHITE);
                playerTwo = new Player<>("BLACK", Game.Color.BLACK);
                chessGame = new Game(playerOne, playerTwo);
                break;
            case "network":
                try {
                    uuid = menuIntent.getStringExtra("uuid");
                    String gameJSONString = menuIntent.getStringExtra("gameJSON");
                    gameJSON = new JSONObject(gameJSONString);
                    white = PostOffice.isWhite(gameJSON.getString("white_md5uuid"), uuid);
                    chessGame = PostOffice.JSONToGame(gameJSON);
                } catch (Exception e){
                    Toast.makeText(getApplicationContext(), String.format("Could not initialize game. Your opponent is likely hacking."), Toast.LENGTH_SHORT).show();
                }
                break;
            case "ai":
                Toast.makeText(getApplicationContext(), String.format("You will be playing as %s", white ? "white" : "black"), Toast.LENGTH_SHORT).show();
                playerTwo = white ? new Chesster<>("BLACK", Game.Color.BLACK)
                                    : new Player<>("BLACK", Game.Color.BLACK);
                playerOne = white ? new Player<>("WHITE", Game.Color.WHITE)
                                    : new Chesster<>("WHITE", Game.Color.WHITE);
                chessGame = new Game(playerOne, playerTwo);
                break;
        }

        userIsWhite = white;

        boardView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if ((userIsWhite ^ chessGame.getCurrentPlayer().color.equals(Game.Color.BLACK)) && v instanceof BoardView) {
                    BoardView bV = (BoardView) v;
                    float x = e.getX();
                    float y = e.getY();
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            break;
                        case MotionEvent.ACTION_UP:
                            Move selection = bV.selectTile((int)x, (int)y, userIsWhite ? chessGame.getPlayer(Game.Color.WHITE) : chessGame.getPlayer(Game.Color.BLACK));
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
    private JSONObject gameJSON;
    @Override
    public void onResume(){
        super.onResume();
        if (gameJSON != null) {
            final JSONObject fJSON = gameJSON;
            new AsyncTask<JSONObject, Void, Void>() {
                @Override
                protected void onPreExecute() {
                    boardView.setLayout(fJSON);
                    if (!userIsWhite)
                        boardView.flipBoard();
                }

                @Override
                protected Void doInBackground(JSONObject[] params) {
                    chessGame = PostOffice.JSONToGame(params[0]);
                    return null;
                }

                @Override
                protected void onPostExecute(Void res) {
                    boardView.playGame(gameJSON, chessGame);
                    boardView.setBoard(chessGame.getCurrentBoard());
                    deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
                    deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));
                    boardView.updateValidMoves();
                }
            }.execute(gameJSON);
        }
    }

    private void gameAdvance(String in){
        Game.GameEvent gameResponse = chessGame.advance(in);
        boardView.setBoard(chessGame.getCurrentBoard());
        deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
        deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));

        switch(gameResponse){
            case CHECK:
                Toast.makeText(getApplicationContext(), "You are in check!", Toast.LENGTH_SHORT).show();
            case OK:
                boardView.updateValidMoves();

                switch (opponentType){

                    case "local":
                        boardView.flipBoard();
                        break;

                    case "network":
                        // send move through post office
                        PostOffice.sendMove(in, uuid, chessGame.ID, boardView.getLayout());

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
    }
}
