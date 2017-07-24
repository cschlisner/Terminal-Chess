package net.schlisner.terminalchess;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;
import org.w3c.dom.Text;

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
        final View rowView = inflater.inflate(R.layout.rowlayout, parent, false);

        TextView textView = (TextView) rowView.findViewById(R.id.gameIDTextView);
        final JSONObject game = gamesJSON.get(position);
        textView.setText(game.optString("id").substring(0, 12));

        HandlerThread ht = new HandlerThread(textView.getText().toString());
        ht.start();
        Handler h = new Handler(ht.getLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                BoardView icon = (BoardView) rowView.findViewById(R.id.icon);
                TextView userturn = (TextView) rowView.findViewById(R.id.userTurn);

                try {
                    boolean isw = PostOffice.isWhite(game.getString("white_md5uuid"), uuid);
                    if (!isw) {
                        icon.flipBoard();
                    }
                    userturn.setText((game.getBoolean("w") ^ isw) ? "" : "!!");
                } catch (Exception e){}
                icon.setLayout(game);
                icon.setMonochrome(true);
            }
        });

        return rowView;
    }
}