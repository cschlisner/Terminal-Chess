package net.schlisner.terminalchess;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONObject;

/**
 * Created by cschl_000 on 5/29/2017.
 *
 * This service is for in-game updating of the game.
 * Upon invoking this, the game
 */

public class GameUpdateService extends IntentService {

    static final public String GAME_RESULT = "net.schlisner.TerminalChess.GameUpdateService.REQUEST_PROCESSED";

    static final public String GAME_UPDATE = "net.schlisner.TerminalChess.GameUpdateService.GAME_UPDATE";

    LocalBroadcastManager broadcaster;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public GameUpdateService() {
        super("GameUpdateService");
        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            String JSONString = intent.getStringExtra("gameJSON");
            JSONObject gameJSON = new JSONObject(JSONString);

            gameJSON = PostOffice.refreshGameJSON(gameJSON.getString("id"));

            sendResult(gameJSON.toString());

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendResult(String gameJSON) {
        Intent intent = new Intent(GAME_RESULT);
        if(gameJSON != null)
            intent.putExtra(GAME_UPDATE, gameJSON);
        broadcaster.sendBroadcast(intent);
    }
}
