package net.schlisner.terminalchess;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.List;

import uniChess.Board;
import uniChess.Game;

/**
 * Created by cschl_000 on 5/18/2017.
 */

class ChessGameListAdapter extends ArrayAdapter<JSONObject> {
    List<JSONObject> gamesJSON;
    String uuid;
    public ChessGameListAdapter(Context c, List<JSONObject> glist, String uuid){
        super(c, -1, glist);
        gamesJSON = glist;
        this.uuid = uuid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) this.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.gameIDTextView);
        BoardView icon = (BoardView) rowView.findViewById(R.id.icon);
        final JSONObject game = gamesJSON.get(position);
        try {
            if (!PostOffice.isWhite(game.getString("white_md5uuid"), uuid))
                icon.flipBoard();
        } catch (Exception e){}
        textView.setText(game.optString("id"));
        icon.setLayout(game);
        icon.setMonochrome(true);
        return rowView;
    }
}