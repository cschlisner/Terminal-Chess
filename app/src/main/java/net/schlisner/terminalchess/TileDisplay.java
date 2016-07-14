package net.schlisner.terminalchess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;

import uniChess.*;
import uniChess.Game;

/**
 * Created by cschl_000 on 7/4/2016.
 */
public class TileDisplay {

    public Board.Tile tile;
    private Paint tilePaint = new Paint();
    private Paint piecePaint = new Paint();
    private int fillColor, pieceColor, chessBoardHighlight, chessBoardCapture;

    public boolean selected, capture;


    public TileDisplay(Context context, Board.Tile tile){
        this.tile = tile;

        tilePaint.setStyle(Paint.Style.STROKE);
        tilePaint.setAntiAlias(true);
        piecePaint.setAntiAlias(true);

        fillColor = (tile.color.equals(Game.Color.BLACK)) ? ContextCompat.getColor(context, R.color.chessBoardDark): ContextCompat.getColor(context, R.color.chessBoardLight);

        chessBoardHighlight = ContextCompat.getColor(context, R.color.chessBoardHighlight);
        chessBoardCapture = ContextCompat.getColor(context, R.color.chessBoardCapture);

        if (tile.getOccupator() != null)
           pieceColor = (tile.getOccupator().color.equals(Game.Color.BLACK)) ? Color.WHITE : Color.WHITE;
    }


    public void draw(Canvas canvas, float dimensions, float x, float y){
        piecePaint.setTextSize(dimensions-(0.1f*dimensions));
        piecePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        tilePaint.setColor(Color.WHITE);
        tilePaint.setStyle(Paint.Style.STROKE);
        tilePaint.setStrokeWidth(3.0f);
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
    }
}
