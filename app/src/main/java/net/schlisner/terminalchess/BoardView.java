package net.schlisner.terminalchess;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;
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
    Paint borderPaint = new Paint();


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
        borderPaint.setColor(ContextCompat.getColor(getContext(), R.color.chessBoardBorder));
        canvas.drawRect(0,0, canvas.getWidth(), 2f, borderPaint);
        tileDim = (((float)getWidth()-4) / 8.0f);
        for (int i = 0; i < 8; ++i){
            for (int j = 0; j < 8; ++j) {
                tileDisplays[flipped ? 7-i : i ][ flipped ? 7-j : j].draw(canvas, tileDim, (i*tileDim), 10+(j*tileDim));
            }
        }
        canvas.drawRect(0, canvas.getHeight()-2f, canvas.getWidth(), canvas.getHeight(), borderPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int deviceWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int deviceHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

        int desiredHeight = (int)((float)deviceHeight * (2.0f/3.0f));
        desiredHeight = (desiredHeight > deviceWidth) ? deviceWidth : desiredHeight;
        int desiredWidth = desiredHeight;

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }

        //MUST CALL THIS
        setMeasuredDimension(width, height);
    }

    public void setMonochrome(boolean isIcon){
        for (TileDisplay[] tda : tileDisplays)
            for (TileDisplay td : tda)
                td.monochrome = isIcon;
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

        if (currentlySelected != null && currentlySelected.getValidDestinations().contains(selectedTile)){
            selectedTile.virtualOccupator = currentlySelected.tile.getOccupator().getSymbol();
//            currentlySelected.tile.setOccupator(null);
            currentlySelected.deselect();

            invalidate();

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

    /**
     * Set the piece positions of this board according to the layout defined in a JSONObject
     * representation of a game
     * @param jsonGame the game of which to set the layout to
     */
    public void setLayout(JSONObject jsonGame) {
        // layout stored in a "layout" field as 64 characters - one for each tile starting at
        // a8 and going through each rank (horizontally) to h1
        try {
            String boardlayout = jsonGame.getString("layout");
            for (int i = 0; i < 64; ++i){
                int y = i/8, x = i <=7 ? i : i - 8 * y;
                if (boardlayout.charAt(i) != '.')
                    gameBoard.getTile(x,7-y).setOccupator(Piece.synthesizePiece(boardlayout.charAt(i)));
                else gameBoard.getTile(x,7-y).setOccupator(null);
            }
            initTiles();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Gets the piece positions of this board
     * representation of a game
     */
    public String getLayout() {
        // layout stored in a "layout" field as 64 characters - one for each tile starting at
        // a8 and going through each rank (horizontally) to h1
        String boardlayout = "";
        for (int i = 0; i < 64; ++i){
            int y = i/8, x = i <=7 ? i : i - 8 * y;
            if (gameBoard.getTile(x,7-y).getOccupator() != null)
                boardlayout += gameBoard.getTile(x,7-y).getOccupator().getSymbol(false);
            else boardlayout += ".";
        }
        return boardlayout;
    }

}
