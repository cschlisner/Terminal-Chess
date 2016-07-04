package net.schlisner.terminalchess;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import uniChess.*;

public class Game extends AppCompatActivity {

    uniChess.Game chessGame;
    Player<String> whitePlayer = new Player<>("WHITE", uniChess.Game.Color.WHITE);
    Player<String> blackPlayer = new Player<>("BLACK", uniChess.Game.Color.WHITE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent menuIntent = getIntent();

        if (menuIntent.getStringExtra("FRESH_MEMES").equals("new"))
            chessGame = new uniChess.Game(whitePlayer, blackPlayer);

        else chessGame = new uniChess.Game(whitePlayer, blackPlayer, menuIntent.getStringExtra("FRESH_MEMES"));

        setContentView(R.layout.activity_game);
    }

    private void gameAdvance(){

        displayBoard(chessGame.getCurrentBoard());

        while (true){
            uniChess.Game.GameEvent gameResponse = chessGame.advance("ayylmao");

            switch(gameResponse){

                case OK:
                    break;
                case AMBIGUOUS:
                    System.out.println("Ambiguous Move.");
                    break;
                case INVALID:
                    System.out.println("Invalid Move.");
                    break;
                case ILLEGAL:
                    System.out.println("Illegal Move.");
                    break;
                case CHECK:
                    System.out.println("You are in check!");
                    break;
                case CHECKMATE:
                    System.out.println("Checkmate. "+chessGame.getDormantPlayer().getID()+" wins!");
                    System.out.println(chessGame.getGameString());
                    System.exit(0);
                    break;
                case STALEMATE:
                    System.out.println("Stalemate. "+chessGame.getDormantPlayer().getID()+" wins!");
                    break;
                case DRAW:
                    System.out.println("Draw!");
                    break;

            }
        }
    }

    private void displayBoard(Board board){

    }
}
