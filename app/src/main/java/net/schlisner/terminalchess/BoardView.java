package net.schlisner.terminalchess;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.ActionBarOverlayLayout;
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

    int boardUpdates = 0;
    public void setBoard(Board b) {
        setBoard(b,false);
    }

    public void setBoard(Board b, boolean updatemoves){
        ++boardUpdates;
        this.gameBoard = b;
        initTiles();

        FontManager.setFont(getContext(), "seguisym.ttf");
        if (updatemoves) {
            updateValidMoves();
        }

        invalidate();
    }

    public void initTiles() {
        initTiles(false);
    }

    public void initTiles(boolean monochrome){
        for (int i = 0; i < 8; ++i){
            for (int j = 0; j < 8; ++j){
                tileDisplays[i][j] = new TileDisplay(getContext(), gameBoard.getTile(i,7-j));
                tileDisplays[i][j].monochrome = monochrome;
            }
        }
        currentlySelected = tileDisplays[0][0];
    }

    // tile display that is being animated should always be drawn last
    TileDisplay animTile;
    /**
     * Animates move transition on board
     * @param move move to animate
     */
    public void animateMove(final Move move, final Board board){
        final TileDisplay origin = getTileDisplay(move.origin);
        final TileDisplay destination = getTileDisplay(move.destination);

        final float distx = destination.cx-origin.cx;
        final float disty = destination.cy-origin.cy;

        final float ox = origin.cx;
        final float oy = origin.cy;

        origin.animating = true;
        animTile = origin;

        final ValueAnimator animation = ValueAnimator.ofFloat(0, 1000);

        animation.setDuration(2500);

        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float progress = valueAnimator.getAnimatedFraction();
                // move attacking piece to destination tile,
                // if there is an attacked piece in the destination tile
                // lower the alpha value of the piece by an amount proportional
                // to the distance between the attacking piece and the destination tile.

                // the attacking piece will move to the destination tile while the attacked piece
                // fades from view and possibly changes color.
                System.out.format("Animating: %s %s %s\n", progress, origin.cx, origin.cy);
                origin.cy = oy + progress*disty;
                origin.cx = ox + progress*distx;
                destination.setCharAlpha((int)((1.0-progress)*255f));
                BoardView.this.invalidate();

                if (progress == 1.0){
                    origin.animating = false;
                    setBoard(board, true);
                }

            }
        });

        animation.start();

        invalidate();
    }

    TileDisplay[][] tileDisplays = new TileDisplay[8][8];

    public void flipBoard(){
        flipped = !flipped;
    }
    public void setFlipped(boolean flip){
        flipped = flip;
    }

    private boolean flipped = false;

    /**
     *	Returns the TileDisplay at a certain location, with the bottom left corner of the board
     *	defined as (0,0) and the top right defined as (7,7).
     * @param location
     * @return
     */
    private TileDisplay getTileDisplay(Location location){
        System.out.println("selecting: "+location);
        System.out.format("returning: [%s][%s]\n", location.x, 7-location.y);
        return tileDisplays[location.x][7-location.y];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        borderPaint.setColor(ContextCompat.getColor(getContext(), R.color.chessBoardBorder));
        canvas.drawRect(0,0, canvas.getWidth(), 2f, borderPaint);
        tileDim = (((float)getWidth()-4) / 8.0f);
        TileDisplay animTile = null;
        int r=0, f=0;
        for (int i = 0; i < 8; ++i){
            for (int j = 0; j < 8; ++j) {
                TileDisplay dr = tileDisplays[flipped ? 7-i : i ][ flipped ? 7-j : j];
                if (dr.animating) {
                    r = i;
                    f = j;
                    animTile = dr;
                }
                else dr.draw(canvas, tileDim, (i*tileDim), 10+(j*tileDim));
            }
        }
        canvas.drawRect(0, canvas.getHeight()-2f, canvas.getWidth(), canvas.getHeight(), borderPaint);
        if (animTile != null)
            animTile.draw(canvas, tileDim, (r*tileDim), 10+(f*tileDim));
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

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

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
        System.out.println("Setting layout...");
        try {
            String boardlayout = jsonGame.getString("layout");
            for (int i = 0; i < 64; ++i){
                int y = i/8, x = i <=7 ? i : i - 8 * y;
                if (boardlayout.charAt(i) != '.')
                    tileDisplays[x][7-y].tile.setOccupator(Piece.synthesizePiece(boardlayout.charAt(i)));
                else tileDisplays[x][7-y].tile.setOccupator(null);
            }
            postInvalidate();
//            initTiles();
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
