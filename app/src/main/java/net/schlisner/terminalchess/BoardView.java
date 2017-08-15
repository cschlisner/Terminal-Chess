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
import android.view.ViewTreeObserver;

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

    private float tileDim = -1;

    public BoardView(Context context, AttributeSet attr){
        super(context, attr);

        setBoard(new Board());

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
//                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                tileDim = (((float)getWidth()-4) / 8.0f);
            }
        });
    }

    public void setBoard(Board b) {
        setBoard(b,false);
    }

    public void setBoard(Board b, boolean updatemoves){
        this.gameBoard = b;
        initTiles();

        FontManager.setFont(getContext(), "seguisym.ttf");
        if (updatemoves) {
            updateValidMoves();
        }

        invalidate();
    }

    private void initTiles() {
        initTiles(false);
    }

    private void initTiles(boolean monochrome){
//        System.out.format("init_tiles:"+getWidth()+"\n");
        for (int i = 0; i < 8; ++i){
            for (int j = 0; j < 8; ++j){
                tileDisplays[i][j] = new TileDisplay(getContext(), gameBoard.getTile(i,7-j));
                tileDisplays[i][j].monochrome = monochrome;
            }
        }
        currentlySelected = tileDisplays[0][0];
//        forceLayout();
    }

    // tile display that is being animated should always be drawn last
    TileDisplay animTile;

    /**
     * Animates move transition on board
 * @param move move to animate
 * @param board
     */
    public void animateMove(final Move move, final Board board) {
        animateMove(move, board, false, null);
    }

    /**
     * Animates move transition on board
     * @param move move to animate
     */
    public void animateMove(final Move move, final Board board, final boolean flip, final PostOffice.MailCallback mcb){
        BoardView.this.invalidate();

        for (int i = 0; i < 8; ++i){
            for (int j = 0; j < 8; ++j){
                int y = flipped ? 7-i : i;
                int x = flipped ? 7-j : j;
                tileDisplays[y][x].cx = (i*tileDim)+0.06f*tileDim;
                tileDisplays[y][x].cy = (j*tileDim)+tileDim-(0.2f*tileDim);
            }
        }

        final TileDisplay origin = getTileDisplay(move.origin);
        final TileDisplay destination = getTileDisplay(move.destination);

        System.out.format("Animating: [%s] %s:(%s,%s) %s:(%s,%s) %s\n", move, origin.toString(), origin.cx, origin.cy, destination.toString(), destination.cx, destination.cy, tileDim);


        final float distx = destination.cx-origin.cx;
        final float disty = destination.cy-origin.cy;

        final float ox = origin.cx;
        final float oy = origin.cy;

        origin.animating = true;
        animTile = origin;

        final ValueAnimator animation = ValueAnimator.ofFloat(0, 1000);

        animation.setDuration((long)Math.E * 1000);

        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float progress = valueAnimator.getAnimatedFraction();

//                System.out.format("Animating: %s %s %s %s\n", move, progress, origin.cx, origin.cy);
                origin.cy = oy + progress*disty;
                origin.cx = ox + progress*distx;
                destination.setCharAlpha((int)((1.0-progress)*255f));
                BoardView.this.invalidate();

                if (progress == 1.0){
                    origin.animating = false;
                    setBoard(board, true);
                    if (flip)
                        flipBoard();
                    if (mcb != null)
                        mcb.after("");
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
//        System.out.println("selecting: "+location);
//        System.out.format("returning: [%s][%s]\n", location.x, 7-location.y);
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
                int y = flipped ? 7-i : i;
                int x = flipped ? 7-j : j;
                TileDisplay dr = tileDisplays[y][x];
//                System.out.format("Drawing Tile: %s\n", dr.toString());
                if (dr.animating) {
                    r = i;
                    f = j;
                    animTile = dr;
                }
                else dr.draw(canvas, tileDim, (i*tileDim), 5+(j*tileDim));
//                else dr.draw(canvas, )
            }
        }
        canvas.drawRect(0, canvas.getHeight()-2f, canvas.getWidth(), canvas.getHeight(), borderPaint);
        if (animTile != null)
            animTile.draw(canvas, tileDim, (r*tileDim), 5+(f*tileDim));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        System.out.println("onmeasure");
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
}
