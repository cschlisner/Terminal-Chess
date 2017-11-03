package net.schlisner.terminalchess;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import uniChess.C3P0;
import uniChess.Chesster;
import uniChess.Game;
import uniChess.Move;
import uniChess.Player;

public class GameActivity extends AppCompatActivity {

    public static final String OPPONENT_NETWORK = "network";
    public static final String OPPONENT_LOCAL = "local";
    public static final String OPPONENT_AI = "ai";

    private static final int DRAW_STATUS_REJECT = -1;
    private static final int DRAW_STATUS_NONE = 0;
    private static final int DRAW_STATUS_OFFER = 1;

    private Game chessGame;
    private static JSONObject gameJSON;

    Handler updateHandler;
    HandlerThread mHandlerThread = new HandlerThread("HandlerThread");
    SharedPreferences sharedPref;

    String uuid;

    boolean userIsWhite=true, returnToMenu, opponentDraw;
    String opponentType = "";

    String gameJSONString;
    BoardView boardView;
    TextView deathRowOpponent;
    TextView deathRowUser;
    TextView statusBarUser;
    TextView drawStatusOpponent;
    TextView drawStatusUser;
    LinearLayout drawStatusLayout, drawControlLayout;
    ProgressBar pb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Fullscreen everything
        super.onCreate(savedInstanceState);
        System.out.println("GameActivity: In onCreate()");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.hide();

        setContentView(R.layout.activity_game);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        pb = (ProgressBar) findViewById(R.id.gameProgressbar);
        pb.setVisibility(View.INVISIBLE);

        boardView = (BoardView) findViewById(R.id.boardviewer);

        deathRowOpponent = (TextView) findViewById(R.id.deathRowOpponent);

        deathRowUser = (TextView) findViewById(R.id.deathRowUser);
        statusBarUser = (TextView) findViewById(R.id.statusBarUser);

        drawStatusUser = (TextView) findViewById(R.id.drawStatusUser);
        drawStatusUser.setTypeface(FontManager.getTypeFace());
        drawStatusUser.setOnClickListener(drawCL);
        drawStatusOpponent = (TextView) findViewById(R.id.drawStatusOpponent);
        drawStatusUser.setTypeface(FontManager.getTypeFace());
        drawStatusOpponent.setVisibility(View.INVISIBLE);
        statusBarUser.setVisibility(View.INVISIBLE);
        drawStatusUser.setVisibility(View.INVISIBLE);

        drawStatusLayout = (LinearLayout)findViewById(R.id.drawStatusLayout);
        drawControlLayout = (LinearLayout)findViewById(R.id.drawControlLayout);

        boardView.setOnTouchListener(boardTL);


        TextView gameUUIDView = (TextView) findViewById(R.id.inGameUUID);
        gameUUIDView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("simple text", ((TextView)v).getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "Game ID copied to clipboard", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        Intent menuIntent = getIntent();

        opponentType = menuIntent.getStringExtra("opponent");
        uuid = menuIntent.getStringExtra("uuid");
        sharedPref = this.getSharedPreferences("Dank Memes(c)", Context.MODE_PRIVATE);

        if (opponentType.equals(OPPONENT_NETWORK)) {
            gameJSONString = menuIntent.getStringExtra("gameJSON");

            try {
//                all of the logic within this Activity assumes that gameJSON is the most recent state of the game
                gameJSON = new JSONObject(gameJSONString);
                statusBarUser.setText(!userIsWhite^gameJSON.optBoolean("w") ? getString(R.string.waiting_for_opponent) : getString(R.string.waiting_for_player));
                drawStatusUser.setVisibility(!userIsWhite^gameJSON.optBoolean("w") ? View.INVISIBLE : View.VISIBLE);
                System.out.println("Starting Game: " + gameJSON.toString(4));
                gameUUIDView.setText(gameJSON.getString("id"));
                System.out.println("Saving Game: " + gameJSON.getString("id"));
                saveGame();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (menuIntent.getBooleanExtra("startFromExt", false))
                returnToMenu = true;

            mHandlerThread.start();
            updateHandler = new Handler(mHandlerThread.getLooper());
        }
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
        if (updateHandler != null) {
            updateHandler.removeCallbacks(recieveNetMove);
            updateHandler.removeCallbacks(drawListener);
        }
        stop_updates = true;
        ChessUpdater.setAlarm(this);
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        if (returnToMenu){
            Intent i = new Intent(this, MenuActivity.class);
            startActivity(i);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        System.out.println("GameActivity: In onResume()");


        stop_updates = false;

        deathRowOpponent.setTypeface(FontManager.getTypeFace());
        deathRowUser.setTypeface(FontManager.getTypeFace());
        drawStatusOpponent.setTypeface(FontManager.getTypeFace());
        drawStatusOpponent.setTypeface(FontManager.getTypeFace());

        if (chessGame != null && opponentType.equals(OPPONENT_NETWORK) && !userTurn())
            updateHandler.post(recieveNetMove);

        
        if (chessGame == null) {
            System.out.println("Initializing game");
            Player<String> playerOne;
            Player<String> playerTwo;
            switch (opponentType) {

                case OPPONENT_LOCAL:

                    playerOne = new Player<>("WHITE", Game.Color.WHITE);
                    playerTwo = new Player<>("BLACK", Game.Color.BLACK);
                    chessGame = new Game(playerOne, playerTwo);
                    boardView.updateValidMoves();
                    break;

                case OPPONENT_NETWORK:
                    statusBarUser.setVisibility(View.VISIBLE);
                    drawStatusUser.setVisibility(View.VISIBLE);
                    try {
                        userIsWhite = PostOffice.isWhite(gameJSON.getString("white_md5uuid"), uuid);
//                        System.out.println("UserIsWhite: " + userIsWhite + " !userTurn(): " + !userTurn());


                        new AsyncTask<JSONObject, Void, Void>() {
                            @Override
                            protected Void doInBackground(JSONObject[] params) {

                                chessGame = PostOffice.JSONToGame(params[0]);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void res) {
                                if (chessGame.getBoardList().size() > 1) {
                                    boardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                        @Override
                                        public void onGlobalLayout() {
                                            boardView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                            boardView.setBoard(chessGame.getBoardList().get(chessGame.getBoardList().size() - 2));
                                            boardView.animateMove(chessGame.getLastMove(), chessGame.getCurrentBoard());
                                        }
                                    });

                                }
                                else {
                                    boardView.setBoard(chessGame.getCurrentBoard());
                                    boardView.updateValidMoves();
                                }
                                statusBarUser.setText(!userTurn() ? getString(R.string.waiting_for_opponent) : getString(R.string.waiting_for_player));
                                drawStatusUser.setVisibility(!userTurn() ? View.INVISIBLE : View.VISIBLE);

                                deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
                                deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));


                                if (!userTurn()) {
                                    updateHandler.postDelayed(recieveNetMove, (long)Math.E*1100);
                                }
                                else if (getDrawStatusUser() == DRAW_STATUS_OFFER) {
                                        drawStatusUser.setVisibility(View.VISIBLE);
                                        drawStatusUser.setText(getString(R.string.draw_offered));
                                        statusBarUser.setText(getString(R.string.waiting_for_opponent));
                                        boardView.setOnTouchListener(null);
                                        updateHandler.post(drawListener);
                                }
                            }
                        }.execute(gameJSON);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), String.format("Could not initialize game. Your opponent is likely hacking."), Toast.LENGTH_SHORT).show();
                    }
                    break;

                case OPPONENT_AI:
                    userIsWhite = (new Random(System.currentTimeMillis())).nextBoolean();

                    Toast.makeText(getApplicationContext(), String.format("You will be playing as %s", userIsWhite ? "white" : "black"), Toast.LENGTH_SHORT).show();
                    playerTwo = userIsWhite ? new C3P0<>("BLACK", Game.Color.BLACK)
                            : new Player<>("BLACK", Game.Color.BLACK);
                    playerOne = userIsWhite ? new Player<>("WHITE", Game.Color.WHITE)
                            : new C3P0<>("WHITE", Game.Color.WHITE);
                    chessGame = new Game(playerOne, playerTwo);
                    boardView.updateValidMoves();

                    if (!userIsWhite)
                        gameAdvance(((C3P0)playerOne).getMove(), false);

                    break;
            }
        }
        boardView.setFlipped(!userIsWhite);
        ChessUpdater.cancelAlarm(this);
    }

    public void onClickAcceptDraw(View v){
        PostOffice.offerDraw(uuid, gameJSON.optString("id"), new PostOffice.MailCallback() {
            @Override
            public void before() {

            }

            @Override
            public void after(String s) {
                try {
                    gameJSON = new JSONObject(s);
                    Toast.makeText(getApplicationContext(), "Draw Accepted.", Toast.LENGTH_SHORT).show();
                    completeGameAndExit(DRAW);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "No response from server.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void onClickRejectDraw(View v){
        PostOffice.rejectDraw(uuid, gameJSON.optString("id"), new PostOffice.MailCallback() {
            @Override
            public void before() {
            }

            @Override
            public void after(String s) {
                try {
                    System.out.println("RECIEVED: "+s);
                    gameJSON = new JSONObject(s);
                    Toast.makeText(getApplicationContext(), "Draw Rejected.", Toast.LENGTH_SHORT).show();
                    drawControlLayout.setVisibility(View.GONE);
                    drawStatusLayout.setVisibility(View.INVISIBLE);
                    statusBarUser.setText(getString(R.string.waiting_for_opponent));
                    updateHandler.post(recieveNetMove);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "No response from server.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    Runnable recieveNetMove = new Runnable() {
        @Override
        public void run() {
            try {
                if (stop_updates) return;
                gameJSON = PostOffice.refreshGameJSON(gameJSON.getString("id"));
                if (gameJSON == null)
                    return;

                System.out.println("listening...");

                if (gameJSON.get(userIsWhite ? "black_md5uuid"  : "white_md5uuid").equals("F")){
                    AlertDialog.Builder adbuilder = new AlertDialog.Builder(GameActivity.this);
                    adbuilder.setTitle("Opponent has forfeit game.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    completeGameAndExit(WIN);
                                }
                            });
                    AlertDialog ad = adbuilder.create();
                    ad.show();
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusBarUser.setText(!userTurn() ? getString(R.string.waiting_for_opponent) : getString(R.string.waiting_for_player));
                        drawStatusUser.setVisibility(!userTurn() ? View.INVISIBLE : View.VISIBLE);

                    }
                });

                if (getDrawStatusOpponent() == DRAW_STATUS_NONE || (getDrawStatusOpponent() == DRAW_STATUS_OFFER && getDrawStatusUser() == DRAW_STATUS_REJECT)){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawStatusOpponent.setVisibility(View.INVISIBLE);
                        }
                    });
                }
                else if (getDrawStatusOpponent() == DRAW_STATUS_OFFER){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawStatusOpponent.setVisibility(View.VISIBLE);
                        }
                    });
                    opponentDraw = true;
                    if (getDrawStatusUser() == DRAW_STATUS_OFFER){
                        Toast.makeText(getApplicationContext(), "Draw accepted.", Toast.LENGTH_SHORT).show();
                        PostOffice.offerDraw(uuid, gameJSON.optString("id"), new PostOffice.MailCallback() {
                            @Override
                            public void before() {
                            }

                            @Override
                            public void after(String s) {
                                try {
                                    completeGameAndExit(DRAW);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(getApplicationContext(), "Oh Christ", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Opponent has offered draw.", Toast.LENGTH_SHORT).show();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                drawStatusLayout.setVisibility(View.GONE);
                                drawControlLayout.setVisibility(View.VISIBLE);
                                statusBarUser.setText(getString(R.string.waiting_for_player));
                            }
                        });
                    }
                    return;
                }
                if (gameJSON.getBoolean("w") != userIsWhite)
                    updateHandler.postDelayed(this, 1000);
                else {
                    System.out.println("Running last input");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                gameAdvance(gameJSON.getJSONArray("moves").getString(gameJSON.getJSONArray("moves").length() - 1), true);
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    Runnable drawListener = new Runnable() {
        @Override
        public void run() {
            try {
                if (stop_updates) return;
                gameJSON = PostOffice.refreshGameJSON(gameJSON.getString("id"));
                if (gameJSON == null)
                    return;

                if (gameJSON.get(userIsWhite ? "black_md5uuid" : "white_md5uuid").equals("F")) {
                    AlertDialog.Builder adbuilder = new AlertDialog.Builder(GameActivity.this);
                    adbuilder.setTitle("Opponent has forfeit game.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    completeGameAndExit(WIN);
                                }
                            });
                    AlertDialog ad = adbuilder.create();
                    ad.show();
                    return;
                }

                System.out.println(gameJSON.getJSONArray("moves"));

                if (getDrawStatusOpponent() == DRAW_STATUS_OFFER) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawStatusOpponent.setVisibility(View.VISIBLE);
                        }
                    });
                    opponentDraw = true;
                    Toast.makeText(getApplicationContext(), "Draw accepted.", Toast.LENGTH_SHORT).show();
                    PostOffice.resetDraw(uuid, gameJSON.optString("id"), new PostOffice.MailCallback() {
                        @Override
                        public void before() {
                        }

                        @Override
                        public void after(String s) {
                            try {
                                completeGameAndExit(DRAW);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(getApplicationContext(), "Oh Christ", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else if (getDrawStatusOpponent() == DRAW_STATUS_REJECT){
                    PostOffice.resetDraw(uuid, gameJSON.getString("id"), new PostOffice.MailCallback() {
                        @Override
                        public void before() {

                        }

                        @Override
                        public void after(String s) {
                            Toast.makeText(getApplicationContext(), "Draw rejected.", Toast.LENGTH_SHORT).show();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    statusBarUser.setText(getString(R.string.waiting_for_player));
                                    drawStatusUser.setVisibility(View.INVISIBLE);
                                }
                            });
                            boardView.setOnTouchListener(boardTL);
                        }
                    });
                }
                else updateHandler.postDelayed(this, 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    View.OnTouchListener boardTL = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (chessGame.getCurrentPlayer().color.equals(Game.Color.WHITE) == userIsWhite && v instanceof BoardView) {
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
            }
            return true;
        }
    };

    View.OnClickListener drawCL = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (!opponentDraw) {
                AlertDialog.Builder adbuilder = new AlertDialog.Builder(GameActivity.this);
                adbuilder.setTitle("Offer Draw?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    PostOffice.offerDraw(uuid, gameJSON.getString("id"), new PostOffice.MailCallback(){
                                        @Override
                                        public void before() {
                                        }
                                        @Override
                                        public void after(String s) {
                                            System.out.println("RECIEVED: "+s);
                                            ((TextView) v).setText(getText(R.string.draw_offered));
                                            statusBarUser.setText(getString(R.string.waiting_for_opponent));
                                            boardView.setOnTouchListener(null);
                                            updateHandler.postDelayed(drawListener, 1000);
                                            try {
                                                gameJSON = new JSONObject(s);
                                                saveGame();
                                            } catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    Toast.makeText(getApplicationContext(), "Network took too long to meme", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                AlertDialog ad = adbuilder.create();
                ad.show();
            }
            else {
                Toast.makeText(getApplicationContext(), "Draw accepted.", Toast.LENGTH_SHORT).show();
                PostOffice.offerDraw(uuid, gameJSON.optString("id"), new PostOffice.MailCallback() {
                    @Override
                    public void before() {
                    }

                    @Override
                    public void after(String s) {
                        try {
                            completeGameAndExit(DRAW);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Oh Christ", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    };

    private void gameAdvance(final String in, final boolean netMove){
        System.out.println("Input: "+in);

        final Game.GameEvent gameResponse = chessGame.advance(in);

        System.out.println("Game response: "+gameResponse);

        boardView.animateMove(chessGame.getLastMove(), chessGame.getCurrentBoard(), opponentType.equals(OPPONENT_LOCAL), new PostOffice.MailCallback(){
            @Override
            public void before() {}
            @Override
            public void after(String s) {
                deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
                deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));
                SharedPreferences.Editor e = sharedPref.edit();
                switch(gameResponse){
                    case CHECK:
                        Toast.makeText(getApplicationContext(), "Check!", Toast.LENGTH_SHORT).show();
                    case OK:

                        switch (opponentType){

                            case OPPONENT_LOCAL:
                                deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(!userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
                                deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(!userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));
                                userIsWhite = !userIsWhite;
                                break;

                            case OPPONENT_NETWORK:
                                if (opponentDraw)
                                    drawStatusOpponent.setVisibility(View.VISIBLE);

                                statusBarUser.setText(!userTurn() ? getString(R.string.waiting_for_opponent) : getString(R.string.waiting_for_player));
                                drawStatusUser.setVisibility(!userTurn() ? View.INVISIBLE : View.VISIBLE);

                                saveMove(in);
                                if (!netMove) {
                                    sendMove(in);
                                }
                                boardView.setOnTouchListener(boardTL);
                                break;

                            case OPPONENT_AI:
                                // user just made move
                                if (userIsWhite ^ chessGame.getCurrentPlayer().color.equals(Game.Color.WHITE)){
                                    // get response move from ai
                                    gameAdvance((chessGame.getCurrentPlayer()).getMove(), false);
                                }
                                break;
                        }
                        break;

                    case CHECKMATE:
                        Toast.makeText(getApplicationContext(), "Checkmate!", Toast.LENGTH_LONG).show();
                        if (opponentType.equals(OPPONENT_NETWORK)) {
                            if (!netMove)
                                sendMove(in);
                            completeGameAndExit(userTurn() ? LOSS : WIN, in);
                        }
                        else {
                            Intent i = new Intent(getApplicationContext(), MenuActivity.class);
                            startActivity(i);
                            finish();
                        }
                        break;

                    case STALEMATE:
                        Toast.makeText(getApplicationContext(), "Stalemate! Game ends in draw!", Toast.LENGTH_LONG).show();
                        if (opponentType.equals(OPPONENT_NETWORK)) {
                            if (!netMove)
                                sendMove(in);
                            completeGameAndExit(DRAW, in);
                        }
                        else {
                            Intent i = new Intent(getApplicationContext(), MenuActivity.class);
                            startActivity(i);
                            finish();
                        }
                        break;

                    case DRAW:
                        Toast.makeText(getApplicationContext(), "Draw!", Toast.LENGTH_LONG).show();
                        if (opponentType.equals(OPPONENT_NETWORK)) {
                            if (!netMove)
                                sendMove(in);
                            completeGameAndExit(DRAW, in);
                        }
                        else {
                            Intent i = new Intent(getApplicationContext(), MenuActivity.class);
                            startActivity(i);
                            finish();
                        }
                        break;

                    case INVALID:
                        // todo: file bug report from UI
                        Toast.makeText(getApplicationContext(), "Something is very wrong...", Toast.LENGTH_LONG).show();
                        System.out.println(chessGame.getCurrentBoard());
                        break;
                }
            }
        });
    }


    // TODO: make network player container objects - extend player
    private int getDrawStatusUser(){
        try {
            if (gameJSON != null)
                return gameJSON.getInt(userIsWhite ? "white_draw" : "black_draw");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getDrawStatusOpponent(){
        try {
            if (gameJSON != null)
                return gameJSON.getInt(userIsWhite ? "black_draw" : "white_draw");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void completeGameAndExit(int score){
        String completedGames = sharedPref.getString("completedGames", null);
        JSONArray completedGameArray = null;
        try {
            completedGameArray = completedGames != null ? new JSONArray(completedGames) : new JSONArray();
            completedGameArray.put(gameJSON);
            PostOffice.leaveGame(uuid, gameJSON.getString("id"));
        }catch (Exception e){e.printStackTrace();}
        SharedPreferences.Editor e =  sharedPref.edit();

        String sc = "";
        switch (score){
            case WIN:
                sc = "score_w";
                break;
            case LOSS:
                sc = "score_l";
                break;
            case DRAW:
                sc = "score_d";
                break;
        }
        e.putInt(sc, sharedPref.getInt(sc, 0)+1);

        e.putString("completedGames", completedGameArray.toString());
        e.apply();
        Intent i = new Intent(getApplicationContext(), MenuActivity.class);
        startActivity(i);
        finish();
    }

    private static final int WIN = 1;
    private static final int LOSS = -1;
    private static final int DRAW = 0;

    private void completeGameAndExit(int score, String in){
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

        String sc = "";
        switch (score){
            case WIN:
                sc = "score_w";
                break;
            case LOSS:
                sc = "score_l";
                break;
            case DRAW:
                sc = "score_d";
                break;
        }
        e.putInt(sc, sharedPref.getInt(sc, 0)+1);

    }

    private void sendMove(final String move){
        System.out.println("Sending move");
        PostOffice.sendMove(move, uuid, chessGame.ID, chessGame.getCurrentBoard().getLayout(), new PostOffice.MailCallback() {
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
                SharedPreferences.Editor e = sharedPref.edit();
                JSONArray gameArr = new JSONArray(games);
                for (int i = 0; i < gameArr.length(); ++i){
                    if (gameArr.optJSONObject(i).getString("id").equals(gameJSON.getString("id"))) {
                        gameArr.optJSONObject(i).getJSONArray("moves").put(move);
                    }
                }
                e.putString("savedGames", gameArr.toString());
                e.apply();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void saveGame(){
        sharedPref = this.getSharedPreferences("Dank Memes(c)", Context.MODE_PRIVATE);
        String games = sharedPref.getString("savedGames", null);
        if (games != null){
            try {
                SharedPreferences.Editor e = sharedPref.edit();
                JSONArray gameArr = new JSONArray(games);
                boolean inSaved = false;
                for (int i = 0; i < gameArr.length(); ++i) {
                    if (gameArr.optJSONObject(i).getString("id").equals(gameJSON.getString("id"))) {
                        inSaved = true;
                        gameArr.remove(i);
                        gameArr.put(i, gameJSON);
                        break;
                    }
                }
                if (!inSaved)
                    gameArr.put(gameJSON);
                e.putString("savedGames", gameArr.toString());
                e.apply();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private JSONObject getGameJSON(){
        if (gameJSON != null)
            return gameJSON;
        if (sharedPref == null)
            sharedPref = getSharedPreferences("Dank Memes(c)", MODE_PRIVATE);
        String gameStr = sharedPref.getString("lastActiveGame", null);
        try {
            gameJSON = new JSONObject(gameStr);
            return gameJSON;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private boolean userTurn(){
        return chessGame.getCurrentPlayer().color.equals(Game.Color.WHITE) == userIsWhite;
    }
}
