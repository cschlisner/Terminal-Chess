package net.schlisner.terminalchess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.List;

import uniChess.Board;
import uniChess.Game;
import uniChess.Location;
import uniChess.Piece;

/**
 * Created by cschl_000 on 7/4/2016.
 */
public class TileDisplay {

    public boolean animating=false;
    public float cy = -1, cx = -1;

    private BoardView parent;
    private Location location;
    private int color;

    public byte piece;

    public boolean monochrome = true;

    public int tileColor = Color.WHITE;

    private List<TileDisplay> validDestinations = new ArrayList<>();

    // BoardView of which this TileDisplay belongs

    private Paint tilePaint = new Paint();
    private TextPaint piecePaint = new TextPaint();
    private int fillColor, pieceColor, chessBoardHighlight, chessBoardCapture;
    public List<TileDisplay> selectedTiles = new ArrayList<>();


    public boolean selected, capture;


    public TileDisplay(Context context, BoardView parent, Location location){
        this.parent = parent;
        this.location = location;
        this.color = ((location.x+location.y)%2==0)?uniChess.Color.BLACK:uniChess.Color.WHITE;

        tilePaint.setStyle(Paint.Style.STROKE);
        tilePaint.setAntiAlias(true);
        piecePaint.setAntiAlias(true);
        tileColor = ContextCompat.getColor(context, R.color.chessBoardLight);
        fillColor = (color == Color.BLACK) ? ContextCompat.getColor(context, R.color.chessBoardDark): ContextCompat.getColor(context, R.color.chessBoardLight);
        chessBoardHighlight = ContextCompat.getColor(context, R.color.chessBoardHighlight);
        chessBoardCapture = ContextCompat.getColor(context, R.color.chessBoardCapture);

        piecePaint.setTypeface(FontManager.getTypeFace());
        piecePaint.setColor(Color.WHITE);
        if (getOccupator() != Piece.NONE){
            pieceColor = ContextCompat.getColor(context, Piece.isb(getOccupator()) ? R.color.chessPiecesDark : R.color.chessPiecesLight);
            piecePaint.setColor(pieceColor);
        }
    }

    public byte getOccupator(){
        return parent.gameBoard.getTile(location);
    }

    public Location getLocale(){
        return location;
    }

    public void select(){
        this.selected = true;

        for (TileDisplay td : validDestinations) {
            td.selected = true;
            if (td.getOccupator() != Piece.NONE)
                td.capture = true;
        }
    }

    public void deselect(){
        this.selected = false;
        this.capture = false;
        for (TileDisplay td : validDestinations) {
            td.capture = false;
            td.selected = false;
        }
    }

    public void setValidDestinations(List<TileDisplay> tileList){
        validDestinations.clear();
        validDestinations.addAll(tileList);
    }

    public List<TileDisplay> getValidDestinations(){
        return validDestinations;
    }

    public void setCharAlpha(int a){
        piecePaint.setAlpha(a);
    }

    /**
     * Draw the information for this TileDisplay's linked tile.
     * @param canvas canvas to draw on
     * @param dimensions dimensions of this (square) tile
     * @param x position of upper left x of tile in canvas
     * @param y position of upper left y of tile in canvas
     */
    public void draw(Canvas canvas, float dimensions, float x, float y){
//        canvas.drawText(String.format("%s%s",this.tile.getLocale().x,this.tile.getLocale().y), x+(0.16f*dimensions), y+dimensions-(0.2f*dimensions), piecePaint);
        tilePaint.setColor(tileColor);
//        piecePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        tilePaint.setStyle(Paint.Style.STROKE);
        tilePaint.setStrokeWidth(3.0f);

        piecePaint.setTextSize(dimensions-(0.1f*dimensions));
        piecePaint.setTextAlign(Paint.Align.LEFT);
        piecePaint.setTypeface(FontManager.getTypeFace());
//        if (tile != null && tile.getOccupator() != null)
//            piecePaint.setColor(tile.getOccupator().passable ? Color.RED : pieceColor);

        float sw = tilePaint.getStrokeWidth()+11;
//        System.out.println("drawing piecetf="+piecePaint.getTypeface());

        // grid lines
        if (!monochrome) {
//            System.out.println("drawing grid color");
            canvas.drawRect(x, y, x+dimensions, y+dimensions, tilePaint);
        }

        // selection lines if applicable
        if (!monochrome && selected) {
            tilePaint.setColor((getOccupator() != Piece.NONE) ? (capture ? chessBoardCapture : Color.LTGRAY) : chessBoardHighlight);
            tilePaint.setStrokeWidth(5);
            canvas.drawRect(x + sw-2, y + sw-2, x + dimensions - sw+2, y + dimensions - sw+2, tilePaint);
        }

        // tile color to create outlined effect
        if (!monochrome) {
//            System.out.println("drawing tile color");
            tilePaint.setColor(fillColor);
            tilePaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(x + sw, y + sw, x + dimensions - sw, y + dimensions - sw, tilePaint);
        }

        if (!animating){
            cx = x+(0.06f*dimensions);
            cy = y+dimensions-(0.2f*dimensions);
        }

        if (getOccupator() != Piece.NONE) {
            canvas.drawText(Piece.symbol(getOccupator()), cx, cy, piecePaint);
        }
    }
}
