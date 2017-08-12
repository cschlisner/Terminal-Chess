package net.schlisner.terminalchess;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Random;

import uniChess.*;

public class GameActivity extends AppCompatActivity {

    private Game chessGame;
    private JSONObject gameJSON;

    Runnable recieveNetMove = new Runnable() {
        @Override
        public void run() {
            try {
                if (stop_updates) return;
                gameJSON = PostOffice.refreshGameJSON(gameJSON.getString("id"));
                if (gameJSON == null)
                    return;
                System.out.println(gameJSON.getJSONArray("moves"));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusBarOpponent.setText(!userTurn() ? getString(R.string.waiting_for_player) : getString(R.string.waiting_for_opponent));
                        statusBarUser.setText(!userTurn() ? getString(R.string.waiting_for_opponent) : getString(R.string.waiting_for_player));
                    }
                });

                if (!userTurn())
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

    View.OnTouchListener boardTL = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (!!userTurn() && v instanceof BoardView) {
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
    };

    Handler updateHandler;
    HandlerThread mHandlerThread = new HandlerThread("HandlerThread");
    SharedPreferences sharedPref;

    String uuid;

    boolean userIsWhite=true, returnToMenu, netMove;
    String opponentType = "";

    String gameJSONString;
    BoardView boardView;
    TextView deathRowOpponent;
    TextView statusBarOpponent;
    TextView deathRowUser;
    TextView statusBarUser;
    ProgressBar pb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Fullscreen everything
        super.onCreate(savedInstanceState);
        System.out.println("In onCreate()");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.hide();

        setContentView(R.layout.activity_game);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        pb = (ProgressBar) findViewById(R.id.gameProgressbar);
        pb.setVisibility(View.GONE);

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
            System.out.println("Starting Game: "+ gameJSON.getString("id"));
            //boardView.setLayout(gameJSON);
        } catch (Exception e){
            e.printStackTrace();
        }
        sharedPref = this.getSharedPreferences("Dank Memes(c)", Context.MODE_PRIVATE);
        if (menuIntent.getBooleanExtra("startFromLobby", false)) {
            returnToMenu = true;
            String games = sharedPref.getString("savedGames", null);
            if (games != null){
                try {
                    JSONArray gameArr = new JSONArray(games);
                    gameArr.put(gameJSON);
                    SharedPreferences.Editor e = sharedPref.edit();
                    e.putString("savedGames", gameArr.toString());
                    e.apply();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }


        mHandlerThread.start();
        updateHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    public void onStart(){
        super.onStart();
        System.out.println("In onStart()");

    }

    boolean stop_updates;
    @Override
    public void onPause(){
        super.onPause();
        updateHandler.removeCallbacks(recieveNetMove);
        stop_updates = true;
        ChessUpdater.setAlarm(this);
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
//        Intent i = new Intent(this, ResumeGameActivity.class);
//        startActivity(i);
//        finish();
    }

    @Override
    public void onResume(){
        super.onResume();

        stop_updates = false;

        boardView.setOnTouchListener(boardTL);
        System.out.format("In onresume(): %s %s", chessGame==null, opponentType);
        
        if (chessGame != null && opponentType.equals("network") && !userTurn())
            updateHandler.post(recieveNetMove);

        
        if (chessGame == null) {
            System.out.println("Initializing game");
            Player<String> playerOne;
            Player<String> playerTwo;
            switch (opponentType) {
                case "local":
                    playerOne = new Player<>("WHITE", Game.Color.WHITE);
                    playerTwo = new Player<>("BLACK", Game.Color.BLACK);
                    chessGame = new Game(playerOne, playerTwo);
                    boardView.updateValidMoves();
                    break;
                case "network":
                    try {
                        userIsWhite = PostOffice.isWhite(gameJSON.getString("white_md5uuid"), uuid);
                        System.out.println("UserIsWhite: " + userIsWhite + " !userTurn(): " + !userTurn());

                        statusBarOpponent.setText(!userTurn() ? getString(R.string.waiting_for_player) : getString(R.string.waiting_for_opponent));
                        statusBarUser.setText(!userTurn() ? getString(R.string.waiting_for_opponent) : getString(R.string.waiting_for_player));


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
                                if (!userTurn()) {
                                    updateHandler.postDelayed(recieveNetMove, 1000);
                                }
                                System.out.println("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIINNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNIIIIIIIIIIIIIIIIIIIIIIISSSSSSSSSSSSSSSSSHHHHHHHHHHHHHHHHHHHH");
                            }
                        }.execute(gameJSON);
                    } catch (Exception e) {
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

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            boardView.updateValidMoves();
                        }
                    });
                    break;
            }
        }
        boardView.setFlipped(!userIsWhite);
        ChessUpdater.cancelAlarm(this);
    }

    private void gameAdvance(String in, boolean netMove){
        System.out.println("Input: "+in);

        Game.GameEvent gameResponse = chessGame.advance(in);
        System.out.println("Game response: "+gameResponse);

        boardView.animateMove(chessGame.getLastMove(), chessGame.getCurrentBoard());

        deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
        deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));
        switch(gameResponse){
            case CHECK:
                Toast.makeText(getApplicationContext(), "Check!", Toast.LENGTH_SHORT).show();
            case OK:

                switch (opponentType){

                    case "local":
                        deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(!userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
                        deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(!userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));
                        boardView.flipBoard();
                        userIsWhite = !userIsWhite;
                        break;

                    case "network":
                        saveMove(in);
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
        String completedGames = sharedPref.getString("completedGames", null);
        JSONArray completedGameArray = null;
        try {
            completedGameArray = completedGames != null ? new JSONArray(completedGames) : new JSONArray();
            gameJSON.getJSONArray("moves").put(in);
            completedGameArray.put(gameJSON);
            PostOffice.leaveGame(uuid, gameJSON.getString("id"));
        }catch (Exception e){e.printStackTrace();}
        SharedPreferences.Editor e =  sharedPref.edit();
        e.putString("completedGames", completedGameArray.toString());
        e.apply();


        Intent i = new Intent(getApplicationContext(), MenuActivity.class);
        startActivity(i);
        finish();
    }

    private void sendMove(final String move){
        System.out.println("Sending move");
        PostOffice.sendMove(move, uuid, chessGame.ID, boardView.getLayout(), new PostOffice.MailCallback() {
            @Override
            public void before() {
            }
            @Override
            public void after(String s) {
                updateHandler.postDelayed(recieveNetMove, 1000);
            }
        });
    }

    private void saveMove(String move){
        String games = sharedPref.getString("savedGames", null);
        if (games != null){
            try {
                JSONArray gameArr = new JSONArray(games);
                for (int i = 0; i < gameArr.length(); ++i){

                    if (gameArr.optJSONObject(i).getString("id").equals(gameJSON.getString("id")))
                        gameArr.optJSONObject(i).getJSONArray("moves").put(move);
                }
                SharedPreferences.Editor e = sharedPref.edit();
                e.putString("savedGames", gameArr.toString());
                e.apply();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private boolean userTurn(){
        try {
            return gameJSON.getBoolean("w") == userIsWhite;
        } catch (JSONException jse){
            jse.printStackTrace();
            return new Random().nextBoolean();
        }
    }
}
