package uniChess;

import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import java.lang.Thread;


/**
*   An object representing a Simulated Player in a chess game. 
*/
public class C3P0 <T> extends Player <T> {
    private int searchDepth = 1;

    private Game game;

    public C3P0(T id, Game.Color c){
        super(id, c);
    }

    public void registerGame(Game g){
        this.game = g;
    }

    public Game getGame(){
        return this.game;
    }

    /**
    *   Returns the best possible legal move for the bot based on individual 
    *   tactics and strategy (logarithmic sum of average tactical value of future moves).
    *
    *   @return the best move
    */
    public String getMove(){
        double best_mm = -10000;
        Move best = null;
        long t1 = System.currentTimeMillis();
        List<Move> legal = game.getCurrentBoard().getLegalMoves(this);
//        System.out.println("Legal:\n"+legal);
        for (Move m : legal){
//            System.out.println("Analyzing: "+m);
            double v = minimax(m, searchDepth, Double.MIN_VALUE, Double.MAX_VALUE, this.color);
            if (best == null || v > best_mm) {
                best = m;
                best_mm = v;
            }
        }

//        System.out.println("Best move: "+best);
        long t2 = System.currentTimeMillis();
        System.out.println("Move Generated in: "+(t2-t1)+"ms");
        return best.getANString();
    }

    private double minimax(Move move, int depth, double alpha, double beta, Game.Color color){
        if (depth == 0){
            return evaluate(move);
        }

        List<Move> responseMoves = move.getSimulation().getOpponentLegalMoves(color);

        if (color.equals(this.color)){
            double v = Double.MIN_VALUE;
            for (Move opponentMove : responseMoves){
                v = Math.max(minimax((opponentMove), depth-1, alpha, beta, Game.getOpposite(color)), v);
                alpha = Math.max(alpha, v);
                if (alpha >= beta) break;
            }
            return v;
        }
        else {
            double v = Double.MAX_VALUE;
            for (Move opponentMove : responseMoves){
                v = Math.min(minimax((opponentMove), depth-1, alpha, beta, Game.getOpposite(color)), v);
                beta = Math.min(beta, v);
                if (alpha >= beta) break;
            }
            return v;
        }
    }

    private double evaluate(Move move){
        //return new Random().nextInt(10);/*
        if (move.CHECKMATE) return Double.MAX_VALUE;
        Board sim = move.getSimulation();
        return (sim.getMaterialCount(this.color) / sim.getMaterialCount(Game.getOpposite(this.color)));//*/
    }

}