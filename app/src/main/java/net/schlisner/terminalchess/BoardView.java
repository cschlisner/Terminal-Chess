package net.schlisner.terminalchess;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import org.w3c.dom.Attr;

import uniChess.Board;

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

    public BoardView(Context context, AttributeSet attr){
        super(context, attr);

        paint.setColor(Color.BLACK);
        paint.setTextSize(50.0f);
    }

    public void setBoard(Board b){
        this.gameBoard = b;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float tileWidth = (float)getWidth() / 8.0f;
        float tileHeight = (float)getHeight() / 8.0f;

        System.out.println("w : "+tileWidth);
        System.out.println("h : "+tileHeight);

        for (int i = 0; i < 8; ++i){
            for (int j = 0; j < 8; ++j){
                Board.Tile tile = gameBoard.getTile(i,j);
                paint.setStyle( (tile.color.equals(uniChess.Game.Color.WHITE)) ? Paint.Style.FILL : Paint.Style.STROKE);
                canvas.drawRect(i*tileWidth, j*tileHeight, (i+1)*tileWidth, (j+1)*tileHeight, paint);
                paint.setColor(Color.LTGRAY);
                canvas.drawText((tile.getOccupator()!=null ? tile.getOccupator().getSymbol() : ""), (i*tileWidth)+(tileWidth/2), ((j+1)*tileHeight)-(tileHeight/2), paint);
                paint.setColor(Color.BLACK);
            }
        }
    }


}
