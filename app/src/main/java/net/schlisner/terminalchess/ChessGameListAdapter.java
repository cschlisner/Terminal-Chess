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
    private List<JSONObject> gamesJSON;
    private String uuid;
    private static Handler h;

    static {
        HandlerThread ht = new HandlerThread("icon layout thread");
        ht.start();
        h = new Handler(ht.getLooper());
    }

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
        TextView userturn = (TextView) rowView.findViewById(R.id.userTurn);
        final JSONObject game = gamesJSON.get(position);
        textView.setText(game.optString("id").substring(0, 12));

        final boolean isw = PostOffice.isWhite(game.optString("white_md5uuid"), uuid);
        userturn.setText((game.optBoolean("w") ^ isw) ? getContext().getString(R.string.waiting_for_opponent)
                : getContext().getString(R.string.waiting_for_player));

        BoardView icon = (BoardView) rowView.findViewById(R.id.boardviewer_listicon);
        try {
            icon.setBoard(new Board(gamesJSON.get(position).optString("layout")));
            icon.setFlipped(!gamesJSON.get(position).optBoolean("w"));
        } catch (Exception e){
            e.printStackTrace();
        }

        return rowView;
    }
}