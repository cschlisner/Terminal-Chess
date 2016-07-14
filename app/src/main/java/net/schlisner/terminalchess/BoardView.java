package net.schlisner.terminalchess;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
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

    public float tileDim;

    public BoardView(Context context, AttributeSet attr){
        super(context, attr);
        initTiles();
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
    }

    TileDisplay[][] tileDisplays = new TileDisplay[8][8];

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        tileDim = ((float)getWidth() / 8.0f);

        for (int i = 0; i < 8; ++i){
            for (int j = 0; j < 8; ++j) {
                tileDisplays[i][j].draw(canvas, tileDim, (i*tileDim), (j*tileDim));
            }
        }
    }

    public List<TileDisplay> selectedTiles = new ArrayList<>();
    private Location selectedLoc;

    /**
     * Selects a tile display and all valid moves originating from the linked tile.
     * Will return the tile that has been selected if the tile has a piece which has
     * valid moves OR a tile with valid moves has already been selected
     * and the selected tile is one of the valid moves.
     *
     * @param file file of the tile (x coordinate)
     * @param rank rank of the tile (y coordinate)
     * @param player player making the selection
     * @return Move that was selected
     */
    public Move selectTile(int rank, int file, Player player){
        if (file > 7 || rank > 7 || rank < 0 || file < 0)
            return null;


        TileDisplay selectedTile = tileDisplays[rank][file];

        for (TileDisplay[] tda : tileDisplays) {
            for (TileDisplay td : tda) {
                td.selected = false;
                td.capture = false;
            }
        }

        if (selectedTile.tile.available(player.color) && (!selectedTiles.isEmpty() && !selectedTiles.contains(selectedTile)))
            return null;

        selectedTile.selected = !selectedTile.selected;

        if (selectedTiles.contains(selectedTile)){
            invalidate();
            selectedTiles.clear();
            return new Move(selectedLoc, selectedTile.tile.getLocale(), gameBoard);
        }

        if (selectedTile.selected) {
            selectedLoc = selectedTile.tile.getLocale();
            selectedTiles.clear();
            for (Move m : gameBoard.getLegalMoves(player)) {
                if (m.origin.equals(selectedTile.tile.getLocale())) {
                    tileDisplays[m.destination.x][7 - m.destination.y].selected = true;

                    if (tileDisplays[m.destination.x][7 - m.destination.y].tile.getOccupator() != null)
                        tileDisplays[m.destination.x][7 - m.destination.y].capture = true;

                    selectedTiles.add(tileDisplays[m.destination.x][7 - m.destination.y]);
                }
            }
        }
        invalidate();
        return null;
    }

}
