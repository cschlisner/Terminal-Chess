package net.schlisner.terminalchess;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;

import uniChess.*;

public class GameActivity extends AppCompatActivity {

    Game chessGame;
    private JSONObject gameJSON;

    Runnable gameUpdateTask;
    Handler updateHandler;

    String uuid;

    boolean userIsWhite, waitingForOpponent;
    String opponentType = "";

    String gameJSONString;
    BoardView boardView;
    TextView deathRowOpponent;
    TextView statusBarOpponent;
    TextView deathRowUser;
    TextView statusBarUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Fullscreen everything
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.hide();

        setContentView(R.layout.activity_game);

        boardView = (BoardView) findViewById(R.id.boardviewer);

        deathRowOpponent = (TextView) findViewById(R.id.deathRowOpponent);
        statusBarOpponent = (TextView) findViewById(R.id.statusBarOpponent);

        deathRowUser = (TextView) findViewById(R.id.deathRowUser);
        statusBarUser = (TextView) findViewById(R.id.statusBarUser);

        Intent menuIntent = getIntent();

        opponentType = menuIntent.getStringExtra("opponent");
        uuid = menuIntent.getStringExtra("uuid");
        gameJSONString = menuIntent.getStringExtra("gameJSON");
        try {
            gameJSON = new JSONObject(gameJSONString);
            boardView.setLayout(gameJSON);
        } catch (Exception e){}
        userIsWhite = (new Random(System.currentTimeMillis())).nextBoolean();

        boardView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (!waitingForOpponent && v instanceof BoardView) {
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

        gameUpdateTask = new Runnable() {
            @Override
            public void run() {
                try {
                    gameJSON = PostOffice.refreshGameJSON(gameJSON.getString("id"));
                    waitingForOpponent =  gameJSON.getBoolean("w") ^ userIsWhite;
                    statusBarOpponent.setText(waitingForOpponent ? "Waiting for input..." : "");
                    statusBarUser.setText(waitingForOpponent ? "" : "Waiting for input...");
                    new AsyncTask<JSONObject, Void, Void>() {
                        @Override
                        protected Void doInBackground(JSONObject[] params) {
                            chessGame = PostOffice.JSONToGame(params[0]);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void res) {
                            boardView.setBoard(chessGame.getCurrentBoard());
                            deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
                            deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));
                            boardView.updateValidMoves();
                        }
                    }.execute(gameJSON);
                    if (waitingForOpponent)
                        updateHandler.postDelayed(this, 1000);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

        updateHandler = new Handler();
    }

    @Override
    public void onStart(){
        super.onStart();

    }

    @Override
    public void onPause(){
        super.onPause();
        waitingForOpponent = false;
        updateHandler.removeCallbacks(gameUpdateTask);
    }

    @Override
    public void onResume(){
        super.onResume();
        if (chessGame == null){
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
                        userIsWhite = PostOffice.isWhite(gameJSON.getString("white_md5uuid"), uuid);
                        waitingForOpponent =  gameJSON.getBoolean("w") ^ userIsWhite;
                        statusBarOpponent.setText(waitingForOpponent ? "Waiting for input..." : "");
                        statusBarUser.setText(waitingForOpponent ? "" : "Waiting for input...");

                        new AsyncTask<JSONObject, Void, Void>() {
                            @Override
                            protected Void doInBackground(JSONObject[] params) {
                                chessGame = PostOffice.JSONToGame(params[0]);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void res) {
                                boardView.setBoard(chessGame.getCurrentBoard());
                                deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
                                deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));
                                boardView.updateValidMoves();

                                if (waitingForOpponent){
                                    System.out.println("Scheduled updater in Resume");
                                    updateHandler.postDelayed(gameUpdateTask, 1000);
                                }
                            }
                        }.execute(gameJSON);
                    } catch (Exception e){
                        Toast.makeText(getApplicationContext(), String.format("Could not initialize game. Your opponent is likely hacking."), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case "ai":
                    Toast.makeText(getApplicationContext(), String.format("You will be playing as %s", userIsWhite ? "white" : "black"), Toast.LENGTH_SHORT).show();
                    playerTwo = userIsWhite ? new Chesster<>("BLACK", Game.Color.BLACK)
                            : new Player<>("BLACK", Game.Color.BLACK);
                    playerOne = userIsWhite ? new Player<>("WHITE", Game.Color.WHITE)
                            : new Chesster<>("WHITE", Game.Color.WHITE);
                    chessGame = new Game(playerOne, playerTwo);
                    break;
            }
            if (!userIsWhite)
                boardView.flipBoard();
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
                        // send move through post office if the user entered a move
                        if (!waitingForOpponent) {
                            PostOffice.sendMove(in, uuid, chessGame.ID, boardView.getLayout(), new PostOffice.MailCallback() {
                                @Override
                                public void before() {

                                }

                                @Override
                                public void after(String s) {
                                    waitingForOpponent = true;
//                                    System.out.println("Scheduled update task in advance()");
                                    updateHandler.postDelayed(gameUpdateTask, 1000);
                                }
                            });
                        }
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
