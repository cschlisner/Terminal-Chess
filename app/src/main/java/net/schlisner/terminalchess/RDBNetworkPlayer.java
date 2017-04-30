package net.schlisner.terminalchess;

import uniChess.Game;
import uniChess.Player;
import uniChess.INetPlayer;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.RethinkDBConnection;
import com.rethinkdb.model.MapObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.UUID;

/**
 * Created by cschl_000 on 4/24/2017.
 */

public class RDBNetworkPlayer<T> extends INetPlayer<T> {
    RethinkDBConnection conn;
    public static final RethinkDB r = RethinkDB.r;
    public RDBNetworkPlayer(T id, Game.Color c){
        super(id, c);
        String hostname = "138.197.213.251";
        int port = 28015;
        String database = "chess";
        (new NetworkThread<String>(){
            @Override
            protected void processResult(String result) { }
            @Override
            protected String backgroundTask(String... params) {
                conn = r.connect(params[0], Integer.valueOf(params[1]));
                conn.use(params[2]);
                return "";
            }
        }).execute(hostname, String.valueOf(port), database);
    }

    @Override
    public String getMoveAN() {
        final StringBuilder player_to_move = new StringBuilder();
        NetworkThread<Object> nt = new NetworkThread() {
            @Override
            protected void processResult(String result) {

            }

            @Override
            protected String backgroundTask(Object... params) {
                String p2m = String.valueOf(r.db("chess").table("games").get("test").field("player_to_move").run(conn));
                while (p2m.equals(((RDBNetworkPlayer)params[0]).color.toString())){
                    p2m = String.valueOf(r.db("chess").table("games").get("test").field("player_to_move").run(conn));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
                params[0].notify();
                return p2m;
            }
        };
        nt.execute(this);
        try {
            this.wait();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        final StringBuilder newestMove = new StringBuilder();
        nt = new NetworkThread() {
            @Override
            protected void processResult(String result) {
            }
            @Override
            protected String backgroundTask(Object... params) {
                List<String> results = (List<String>)r.db("chess").table("games").get("test").field("moves").run(conn);
                String res = String.valueOf(results.get(results.size()-1));
                newestMove.append(res);
                params[0].notify();
                return res;
            }
        };
        nt.execute(this);
        try {
            this.wait();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        System.out.println("recieved: " + newestMove);
        return newestMove.toString();
    }

    @Override
    public void sendMoveAN(String in) {
        //r.db("chess").table("games").get("test").update(new MapObject().with("moves", r.row().field("moves").insertAt(0, "new0"))).run(conn);
    }
}
