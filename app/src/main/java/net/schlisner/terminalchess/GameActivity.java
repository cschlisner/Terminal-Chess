package net.schlisner.terminalchess;

import android.content.Intent;
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

import uniChess.*;

public class GameActivity extends AppCompatActivity {

    uniChess.Game chessGame;

    Player<String> whitePlayer;
    Player<String> blackPlayer;

    String opponentType = "";

    BoardView boardView;
    TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.hide();

        Intent menuIntent = getIntent();
        opponentType = menuIntent.getStringExtra("opponent");
        if (menuIntent.getStringExtra("init_mode").equals("new")){
            switch (opponentType){
                case "local":
                    // both players interact with the game in the same way
                    blackPlayer = new Player<>("BLACK", Game.Color.BLACK);
                    whitePlayer = new Player<>("WHITE", Game.Color.WHITE);
                    break;
                case "network":
                    break;
                case "ai":
                    // decide which color the human player is (i.e. if they make first move)
                    boolean white = (new Random(System.currentTimeMillis())).nextBoolean();
                    Toast.makeText(getApplicationContext(), String.format("You will be playing as %s", white ? "white" : "black"), Toast.LENGTH_SHORT).show();

                    blackPlayer = white ? new Chesster<>("BLACK", Game.Color.BLACK)
                                        : new Player<>("BLACK", Game.Color.BLACK);
                    whitePlayer = white ? new Player<>("WHITE", Game.Color.WHITE)
                                        : new Chesster<>("WHITE", Game.Color.WHITE);

                    //((Chesster)(white?blackPlayer:whitePlayer)).setPrintStream(chessterOutputStream);
                    break;
            }

            chessGame = new uniChess.Game(whitePlayer, blackPlayer);
        }
        else if (menuIntent.getStringExtra("init_mode").equals("resume")){
            // load saved uniChess.GameActivity
            String gameString = "actually_load_a_saved_thing_here";

            blackPlayer = new Player<>("BLACK", Game.Color.BLACK);
            whitePlayer = new Player<>("WHITE", Game.Color.WHITE);
            chessGame = new uniChess.Game(whitePlayer, blackPlayer, gameString);
        }

        else chessGame = new uniChess.Game(whitePlayer, blackPlayer, menuIntent.getStringExtra("FRESH_MEMES"));

        //uniChess.GameActivity.useDarkChars = true;

        setContentView(R.layout.activity_game);

        boardView = (BoardView) findViewById(R.id.boardviewer);
        statusView = (TextView) findViewById(R.id.aiOutTextView);

        boardView.setBoard(chessGame.getCurrentBoard());

        statusView.setText(opponentType);

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
//                            System.out.format("---------  dbg: rank: %s\n", rank);
                            file = (int) Math.floor(y / bV.tileDim);
//                            System.out.format("---------  dbg: file: %s\n", file);
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

    private PrintStream chessterOutputStream = new PrintStream(new ChessterOutputStream(boardView));

    private class ChessterOutputStream extends OutputStream {
        BoardView boardview;
        public ChessterOutputStream(BoardView bV){
            boardview = bV;
        }
        String aiBuffer = "";
        @Override
        public void write(int oneByte) throws IOException {
                aiBuffer += (char)oneByte;
               statusView.setText(aiBuffer);
        }
        public void clear(){
            try {
                write("\\r".getBytes()[0]);
            } catch (Exception e){
            }
        }
    }

    private void gameAdvance(String in){

        Game.GameEvent gameResponse = chessGame.advance(in);
        boardView.setBoard(chessGame.getCurrentBoard());

        switch(gameResponse){
            case CHECK:
                Toast.makeText(getApplicationContext(), "You are in check!", Toast.LENGTH_SHORT).show();
                break;
            case OK:
                switch (opponentType){
                    case "local":
                        boardView.flipBoard();
                        Toast.makeText(getApplicationContext(), (chessGame.getCurrentPlayer().color)+" to move", Toast.LENGTH_SHORT).show();
                        break;
                    case "network":
                        Toast.makeText(getApplicationContext(), "Waiting for opponent...", Toast.LENGTH_SHORT).show();
                        break;
                    case "ai":
                        gameAdvance(((Chesster) chessGame.getCurrentPlayer()).getMove().getANString());
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
