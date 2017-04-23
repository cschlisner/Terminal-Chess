package net.schlisner.terminalchess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import uniChess.*;
import uniChess.Game;

/**
 * Created by cschl_000 on 7/4/2016.
 */
public class TileDisplay {

    public Piece virtualOccupator;
    public Board.Tile tile;

    private List<TileDisplay> validDestinations = new ArrayList<>();

    // BoardView of which this TileDisplay belongs

    private Paint tilePaint = new Paint();
    private Paint piecePaint = new Paint();
    private int fillColor, pieceColor, chessBoardHighlight, chessBoardCapture;
    public List<TileDisplay> selectedTiles = new ArrayList<>();


    public boolean selected, capture;


    public TileDisplay(Context context, Board.Tile tile){
        this.tile = tile;

        tilePaint.setStyle(Paint.Style.STROKE);
        tilePaint.setAntiAlias(true);
        piecePaint.setAntiAlias(true);

        fillColor = (tile.color.equals(Game.Color.BLACK)) ? ContextCompat.getColor(context, R.color.chessBoardDark): ContextCompat.getColor(context, R.color.chessBoardLight);

        chessBoardHighlight = ContextCompat.getColor(context, R.color.chessBoardHighlight);
        chessBoardCapture = ContextCompat.getColor(context, R.color.chessBoardCapture);

        if (tile.getOccupator() != null){
            pieceColor = (tile.getOccupator().color.equals(Game.Color.BLACK)) ? Color.WHITE : Color.WHITE;
        }


    }

    public void select(){
        this.selected = true;

        for (TileDisplay td : validDestinations) {
            td.selected = true;
            if (td.tile.getOccupator() != null)
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

    public void draw(Canvas canvas, float dimensions, float x, float y){
        piecePaint.setTextAlign(Paint.Align.LEFT);
        piecePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        tilePaint.setStyle(Paint.Style.STROKE);
        tilePaint.setStrokeWidth(3.0f);
        tilePaint.setColor(Color.WHITE);
        piecePaint.setTextSize(dimensions-(0.1f*dimensions));

        float sw = tilePaint.getStrokeWidth()+11;

        canvas.drawRect(x, y, x+dimensions, y+dimensions, tilePaint);
        piecePaint.setColor(pieceColor);
        if (selected) {
            tilePaint.setColor((tile.getOccupator() != null) ? (capture ? chessBoardCapture : Color.LTGRAY) : chessBoardHighlight);
            tilePaint.setStrokeWidth(5);
            canvas.drawRect(x + sw-2, y + sw-2, x + dimensions - sw+2, y + dimensions - sw+2, tilePaint);
        }
        if (tile.color.equals(Game.Color.BLACK)) {
            tilePaint.setColor(fillColor);
            tilePaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(x + sw, y + sw, x + dimensions - sw, y + dimensions - sw, tilePaint);
        }
        if (tile.getOccupator()!=null) {
            canvas.drawText(tile.getOccupator().getSymbol(), x+(0.16f*dimensions), y+dimensions-(0.2f*dimensions), piecePaint);
        }
        else if (virtualOccupator != null){
            canvas.drawText(virtualOccupator.getSymbol(), x+(0.16f*dimensions), y+dimensions-(0.2f*dimensions), piecePaint);

        }
    }
}
