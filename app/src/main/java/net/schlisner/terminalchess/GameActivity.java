package net.schlisner.terminalchess;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    PostOffice po = new PostOffice();

    Player<String> playerOne;
    Player<String> playerTwo;

    String uuid;
    boolean userIsWhite;

    String opponentType = "";

    BoardView boardView;
    TextView deathRowOpponent;
    TextView deathRowUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        boolean waitForNetMove = false;
        switch (opponentType){
            case "local":
                // both players interact with the game in the same way
                playerOne = new Player<>("WHITE", Game.Color.WHITE);
                playerTwo = new Player<>("BLACK", Game.Color.BLACK);
                break;
            case "network":
                try {
                    uuid = menuIntent.getStringExtra("uuid");
                    String gameJSONString = menuIntent.getStringExtra("gameJSON");
                    JSONObject gameJSON = new JSONObject(gameJSONString);
                    white = PostOffice.isWhite(gameJSON.getString("white_md5uuid"), uuid);
                    waitForNetMove = white == !gameJSON.getBoolean("w");

                    if (gameJSON.getJSONArray("moves").length() == 0)
                        Toast.makeText(getApplicationContext(), String.format("You will be playing as %s", white ? "white" : "black"), Toast.LENGTH_SHORT).show();

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
                break;
        }

        boardView.setBoard(chessGame.getCurrentBoard());
        deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(white ? Game.Color.BLACK : Game.Color.WHITE));
        deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(white ? Game.Color.WHITE : Game.Color.BLACK));
        boardView.updateValidMoves();
        boardView.w = chessGame.getCurrentPlayer().color.equals(Game.Color.WHITE);


        userIsWhite = white;

        if (!white)
            boardView.flipBoard();

        boardView.setOnTouchListener(new View.OnTouchListener() {
            int rank, file;
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                BoardView bV = (BoardView) v;
                if (opponentType.equals("network") && (userIsWhite ^ bV.w))
                    getNetOpponentMove(chessGame.ID);
                if (v instanceof BoardView) {

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

//        if (waitForNetMove) {
//            getNetOpponentMove(chessGame.ID);
//        }
    }

    private boolean isUserTurn(){
        return !(userIsWhite ^ chessGame.getCurrentPlayer().color.equals(Game.Color.WHITE));
    }

    // probably want to convert this to a service eventually
    private long timeout = 10000;
    private void getNetOpponentMove(String gameID){
        chessGame = PostOffice.refreshGame(gameID);
        if (chessGame != null) {
            boardView.setBoard(chessGame.getCurrentBoard());
            boardView.w = chessGame.getCurrentPlayer().color.equals(Game.Color.WHITE);
            boardView.updateValidMoves();
            deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
            deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));
        }
    }

    private void gameAdvance(String in){
        Game.GameEvent gameResponse = chessGame.advance(in);
        boardView.setBoard(chessGame.getCurrentBoard());
        boardView.w = chessGame.getCurrentPlayer().color.equals(Game.Color.WHITE);
        deathRowUser.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.BLACK : Game.Color.WHITE));
        deathRowOpponent.setText(chessGame.getCurrentBoard().displayDeathRow(userIsWhite ? Game.Color.WHITE : Game.Color.BLACK));

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
                        // send move through post office
                        po.sendMove(in, uuid, chessGame.ID);
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
