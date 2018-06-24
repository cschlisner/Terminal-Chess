package uniChess;

import java.util.List;


/**
*   An object representing a Simulated Player in a chess game. 
*/
public class C3P0 <T> extends Player <T> {
    private int searchDepth = 3;

    private Game game;

    public C3P0(T id, int c){
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
            double v = negamax(m.getSimulation(), searchDepth, -1000000.0, 100000000.0, this.color);
            if (best == null || v > best_mm) {
                best = m;
                best_mm = v;
            }
        }

        System.out.println("Best move: "+best.getANString());
        long t2 = System.currentTimeMillis();
        System.out.println("Move Generated in: "+(t2-t1)+"ms");
        return best.getANString();
    }

    private double negamax(Board sim, int depth, double alpha, double beta, int color){
        // $color to move on board $sim
        if (depth == 0){
            double opMaterial = -1 * Math.pow(sim.getMaterialCount(Color.opposite(color)), 2.0);

            return color == Color.BLACK? opMaterial : -1 * opMaterial;
        }

        List<Move> childMoves = sim.getLegalMoves(color);

        double bestValue = -1000000000000000.0;
        long t0 = System.currentTimeMillis();
        for (Move move : childMoves){
            double v = -negamax(move.getSimulation(), depth-1, -beta, -alpha, -color);
            bestValue = Math.max(bestValue, v);
            alpha = Math.max(alpha, v);
            // the best move at this point for $color is worse than a different move for -$color
            if (alpha >= beta) break;
        }
//        System.out.println("Depth "+depth+" calculated in "+(System.currentTimeMillis()-t0));

        return bestValue;
    }

}