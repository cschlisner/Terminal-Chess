package net.schlisner.terminalchess;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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
import java.util.concurrent.Exchanger;
import java.util.concurrent.ThreadFactory;

import uniChess.*;

public class GameActivity extends AppCompatActivity {

    Game chessGame;
    private JSONObject gameJSON;

    Runnable gameUpdateTask;
    Handler updateHandler;
    HandlerThread mHandlerThread = new HandlerThread("HandlerThread");

    String uuid;

    boolean userIsWhite=true, waitingForOpponent, returnToMenu, netMove;
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
        if (menuIntent.getBooleanExtra("startFromLobby", false))
            returnToMenu = true;

        opponentType = menuIntent.getStringExtra("opponent");
        uuid = menuIntent.getStringExtra("uuid");
        gameJSONString = menuIntent.getStringExtra("gameJSON");

        try {
            gameJSON = new JSONObject(gameJSONString);
            System.out.println("Starting Game: "+gameJSON.getString("id"));
            boardView.setLayout(gameJSON);
        } catch (Exception e){}

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
                                System.out.println("Advancing: "+selection);
                                gameAdvance(selection.getANString(), false);
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
                    if (gameJSON == null)
                        return;
                    System.out.println(gameJSON.getJSONArray("moves"));
                    waitingForOpponent =  gameJSON.getBoolean("w") ^ userIsWhite;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusBarOpponent.setText(waitingForOpponent ? getString(R.string.waiting_for_player) : "");
                            statusBarUser.setText(waitingForOpponent ? "" : getString(R.string.waiting_for_player));
                        }
                    });

                    if (waitingForOpponent)
                        updateHandler.postDelayed(this, 1000);
                    else {
                        System.out.println("Running last input");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    gameAdvance(gameJSON.getJSONArray("moves").getString(gameJSON.getJSONArray("moves").length() - 1), true);
                                } catch (Exception e){}
                            }
                        });
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

        mHandlerThread.start();
        updateHandler = new Handler(mHandlerThread.getLooper());
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
    public void onBackPressed(){
        if (returnToMenu) {
            Intent i = new Intent(getApplicationContext(), MenuActivity.class);
            startActivity(i);
        }
        else super.onBackPressed();
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
                    waitingForOpponent = false;
                    boardView.updateValidMoves();
                    break;
                case "network":
                    try {
                        userIsWhite = PostOffice.isWhite(gameJSON.getString("white_md5uuid"), uuid);
                        waitingForOpponent =  gameJSON.getBoolean("w") ^ userIsWhite;
                        System.out.println("UserIsWhite: "+userIsWhite+" waitingForOpponent: "+waitingForOpponent);

                        statusBarOpponent.setText(waitingForOpponent ? getString(R.string.waiting_for_player) : "");
                        statusBarUser.setText(waitingForOpponent ? "" : getString(R.string.waiting_for_player));

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
                                    updateHandler.postDelayed(gameUpdateTask, 1000);
                                }
                            }
                        }.execute(gameJSON);
                    } catch (Exception e){
                        Toast.makeText(getApplicationContext(), String.format("Could not initialize game. Your opponent is likely hacking."), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case "ai":
                    userIsWhite = (new Random(System.currentTimeMillis())).nextBoolean();

                    Toast.makeText(getApplicationContext(), String.format("You will be playing as %s", userIsWhite ? "white" : "black"), Toast.LENGTH_SHORT).show();
                    playerTwo = userIsWhite ? new Chesster<>("BLACK", Game.Color.BLACK)
                            : new Player<>("BLACK", Game.Color.BLACK);
                    playerOne = userIsWhite ? new Player<>("WHITE", Game.Color.WHITE)
                            : new Chesster<>("WHITE", Game.Color.WHITE);
                    chessGame = new Game(playerOne, playerTwo);

                    boardView.updateValidMoves();

                    break;
            }
            if (!userIsWhite)
                boardView.flipBoard();
        }
    }

    private void gameAdvance(String in, boolean netMove){
        System.out.println("Input: "+in);
        boardView.invalidate();
        Game.GameEvent gameResponse = chessGame.advance(in);
        System.out.println("Game response: "+gameResponse);
        boardView.setBoard(chessGame.getCurrentBoard());
        deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
        deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));
        switch(gameResponse){
            case CHECK:
                Toast.makeText(getApplicationContext(), "Check!", Toast.LENGTH_SHORT).show();
            case OK:
                boardView.updateValidMoves();

                switch (opponentType){

                    case "local":
                        deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(!userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
                        deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(!userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));
                        boardView.flipBoard();
                        userIsWhite = !userIsWhite;
                        break;

                    case "network":
                        if (!netMove)
                            sendMove(in);
                        break;

                    case "ai":
                        // user just made move
                        if (userIsWhite ^ chessGame.getCurrentPlayer().color.equals(Game.Color.WHITE)){
                            // get response move from ai
                            gameAdvance(((Chesster)chessGame.getCurrentPlayer()).getMove().getANString(), false);
                        }
                        break;
                }
                break;

            case CHECKMATE:
                Toast.makeText(getApplicationContext(), "Checkmate!", Toast.LENGTH_SHORT).show();
                if (!netMove)
                    sendMove(in);
                saveGameAndExit(in);
                break;

            case STALEMATE:
                Toast.makeText(getApplicationContext(), "Stalemate! Game ends in draw!", Toast.LENGTH_SHORT).show();
                if (!netMove)
                    sendMove(in);
                saveGameAndExit(in);
                break;

            case DRAW:
                Toast.makeText(getApplicationContext(), "Draw!", Toast.LENGTH_SHORT).show();
                if (!netMove)
                    sendMove(in);
                saveGameAndExit(in);
                break;

            case INVALID:
                System.out.println(chessGame.getCurrentBoard());
                break;

            default:
                saveGameAndExit(in);
                break;
        }
    }

    private void saveGameAndExit(String in){
        // Save game
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String completedGames = sharedPref.getString("completedGames", null);
        JSONArray completedGameArray = null;
        try {
            completedGameArray = completedGames != null ? new JSONArray(completedGames) : new JSONArray();
            gameJSON.getJSONArray("moves").put(in);
            completedGameArray.put(gameJSON);
        }catch (Exception e){e.printStackTrace();}
        SharedPreferences.Editor e =  sharedPref.edit();
        e.putString("completedGames", completedGameArray.toString());
        e.apply();
        Intent i = new Intent(getApplicationContext(), MenuActivity.class);
        startActivity(i);
        finish();
    }

    private void sendMove(String move){
        System.out.println("Sending move");
        PostOffice.sendMove(move, uuid, chessGame.ID, boardView.getLayout(), new PostOffice.MailCallback() {
            @Override
            public void before() {
            }
            @Override
            public void after(String s) {
                System.out.println(s);
                updateHandler.postDelayed(gameUpdateTask, 1000);
            }
        });
    }
}
