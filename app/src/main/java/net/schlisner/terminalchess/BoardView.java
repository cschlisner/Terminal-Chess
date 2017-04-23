package net.schlisner.terminalchess;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.w3c.dom.Attr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uniChess.*;

/**
 * TODO: document your custom view class.
 */
public class BoardView extends View {

    int paddingLeft = getPaddingLeft();
    int paddingTop = getPaddingTop();
    int paddingRight = getPaddingRight();
    int paddingBottom = getPaddingBottom();

    int contentWidth = getWidth() - paddingLeft - paddingRight;
    int contentHeight = getHeight() - paddingTop - paddingBottom;

    Board gameBoard = new Board();

    Paint paint = new Paint();

    private float tileDim;

    public BoardView(Context context, AttributeSet attr){
        super(context, attr);
        setBoard(new Board());
    }

    public void setBoard(Board b){
        this.gameBoard = b;
        initTiles();
        invalidate();
    }

    public void initTiles(){
        for (int i = 0; i < 8; ++i){
            for (int j = 0; j < 8; ++j){
               tileDisplays[i][j] = new TileDisplay(getContext(), gameBoard.getTile(i,7-j));
            }
        }
        currentlySelected = tileDisplays[0][0];
    }

    TileDisplay[][] tileDisplays = new TileDisplay[8][8];

    public void flipBoard(){
        flipped = !flipped;
    }
    private boolean flipped = false;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        tileDim = ((float)getWidth() / 8.0f);
        for (int i = 0; i < 8; ++i){
            for (int j = 0; j < 8; ++j) {
                tileDisplays[flipped ? 7-i : i ][ flipped ? 7-j : j].draw(canvas, tileDim, (i*tileDim), (j*tileDim));
            }
        }
    }

    public void updateValidMoves(){
        List<TileDisplay> validDestinations = new ArrayList<>();
        for (TileDisplay[] tda : tileDisplays){
            for (TileDisplay td : tda){
                td.virtualOccupator = null;
                if (td.tile.getOccupator() != null){
                    validDestinations.clear();
                    for (Move move : gameBoard.getLegalMoves(td.tile.getOccupator().color))
                        if (move.origin.equals(td.tile.getLocale()))
                            validDestinations.add(tileDisplays[move.destination.x][7-move.destination.y]);
                    td.setValidDestinations(validDestinations);
                }
            }
        }
    }

    private TileDisplay currentlySelected;
    /**
     * Selects a tile display and all valid moves originating from the linked tile.
     * Will return the tile that has been selected if the tile has a piece which has
     * valid moves OR a tile with valid moves has already been selected
     * and the selected tile is one of the valid moves.
     *
     * @param x (x coordinate) on View
     * @param y (y coordinate) on VIew
     * @param player player making the selection
     * @return Move that was selected
     */
    public Move selectTile(int x, int y, Player player){
        int rank = (int) Math.floor(x / tileDim);
        int file = (int) Math.floor(y / tileDim);
        if (file > 7 || rank > 7 || rank < 0 || file < 0)
            return null;

        rank = flipped ? 7-rank : rank;
        file = flipped ? 7-file : file;

        TileDisplay selectedTile = tileDisplays[rank][file];

        if (currentlySelected != null &&currentlySelected.getValidDestinations().contains(selectedTile)){
            return new Move(currentlySelected.tile.getLocale(), selectedTile.tile.getLocale(), gameBoard);
        }

        if (currentlySelected == null || selectedTile.tile.available(player.color))
            return null;


        currentlySelected.deselect();
        selectedTile.select();
        currentlySelected = selectedTile;

        invalidate();
        return null;
    }
}
