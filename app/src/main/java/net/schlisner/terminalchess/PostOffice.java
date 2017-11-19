package net.schlisner.terminalchess;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;
import uniChess.Game;
import uniChess.Player;

/**
 * Created by cschl_000 on 5/11/2017.
 *
 * Static class full of utility and networking methods.
 * Manages all communication and interpretation of game data.
 */
class PostOffice {

    // Addresses of our friends
    private static final String Server = "http://138.197.213.251/";
    private static final String Chives = Server + "cgi-bin/Chives.py";
    private static final String Chesster = Server + "cgi-bin/Chesster.py";
    private static final int NETWORK_TIMEOUT_SECS = 5;

    /**
     * Determines which player a user is in a game given the hashes of the player uuids and the users
     * uuid
     * @param wmd5uuid hash of white player's uuid
     * @param uuid uuid of user
     * @return true if hash of uuid matches wmd5uuid false otherwise
     */
    static boolean isWhite(String wmd5uuid, String uuid){
        return MD5(uuid).equals(wmd5uuid);
    }

    private static String MD5(String str) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(str.getBytes());
            StringBuffer sb = new StringBuffer();
            for (byte anArray : array) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException ignored) {
        }
        return null;
    }

    /**
     * Registers a new chess player with the server and returns the corresponding uuid
     *
     * @return UUID of created player
     */
    static String register() throws Exception{
        MailSend registerPlayer = new MailSend();
        String meme = registerPlayer.execute(Chives, "action", "register").get(NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS);
        meme = meme.trim();
        return meme; // should be just the uuid text
    }
    /**
     * Checks player into lobby where it will be matched with another player
     *
     * @return List of all games user is engaged in, by time created
     */
    static void checkIn(String uuid) {
        MailSend checkIn = new MailSend();
        checkIn.execute(Chives, "action", "checkin", "uuid", uuid);
    }


    /**
     * Checks player out of lobby
     *
     * @return List of all games user is engaged in, by time created
     */
    static void checkOut(String uuid) {
        MailSend checkIn = new MailSend();
        checkIn.execute(Chives, "action", "checkout", "uuid", uuid);
    }

    /**
     * Attempts to join open game
     *
     * @return game id of game to join
     *
     */
    static void joinGame(String uuid, String gameid, MailCallback mcb){
        try {
            new MailSend(mcb).execute(Chives, "action", "joingame", "game", gameid, "uuid", uuid)
                        .get(NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Attempts to join open game
     *
     * @return game id of game to join
     *
     */
    static void createGame(String uuid, boolean isPublic, MailCallback mcb){
        try {
            System.out.println(String.valueOf(isPublic));
            new MailSend(mcb).execute(Chives, "action", "creategame", "uuid", uuid, "public", String.valueOf(isPublic))
                    .get(NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }
    static JSONArray listPublicGames(String uuid) throws Exception{
        return listPublicGames(uuid, null);
    }

    static JSONArray listPublicGames(String uuid, MailCallback mcb) throws Exception {
        MailSend publicGames = mcb == null ? new MailSend() : new MailSend(mcb);
        String response = publicGames.execute(Chives, "uuid", uuid, "action", "listpublic").get(NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS);
        return new JSONArray(response);
    }

    /**
     * Lists all game JSON objects in ascending order of time created (newest game first)
     *
     * @return List of all games user is engaged in, by time created
 * @param uuid
     */
    static JSONArray listGamesJSON(String uuid) throws Exception {
        return listGamesJSON(uuid, null);
    }

    /**
     * Lists all game JSON objects in ascending order of time created (newest game first)
     *
     * @return List of all games user is engaged in, by time created
     */
    static JSONArray listGamesJSON(String uuid, MailCallback mcb) throws Exception{
        uuid = uuid.trim();
        MailSend getGameList = mcb == null ? new MailSend() : new MailSend(mcb);
        String response = getGameList.execute(Chives, "action", "retrievegames", "uuid", uuid)
                .get(NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS);
        return new JSONArray(response);
    }

    /**
     * Lists all games in ascending order of time created (newest game first)
     *
     * @return List of all games user is engaged in, by time created
     */
    static List<Game> listGames(String uuid) throws Exception {
        return listGames(uuid, null);
    }

    /**
     * Lists all games in ascending order of time created (newest game first)
     *
     * @return List of all games user is engaged in, by time created
     */
    static List<Game> listGames(String uuid, MailCallback mcb) throws Exception{
        List<Game> gameList = new ArrayList<>();
        MailSend getGameList = mcb == null ? new MailSend() : new MailSend(mcb);
        String response = getGameList.execute(Chives, "action", "retrievegames", "uuid", uuid)
                                        .get(NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS);
        JSONArray gameArray = new JSONArray(response);
        for (int i = 0; i < gameArray.length(); ++i){
            gameList.add(JSONToGame(gameArray.optJSONObject(i)));
        }
        return gameList;
    }

    static Game JSONToGame(JSONObject jsonGame){
        String whiteID = jsonGame.optString("white_md5uuid");
        String blackID = jsonGame.optString("black_md5uuid");

        String gameStr = "";

        JSONArray moves = jsonGame.optJSONArray("moves");

        for (int i = 0; i < moves.length(); ++i)
            gameStr += moves.optString(i)+",";

        Game g;
//        long t1 = System.currentTimeMillis();

        if (!gameStr.equals(""))
            g = new Game(new Player<>(whiteID, Game.Color.WHITE), new Player<>(blackID, Game.Color.BLACK), gameStr);
        else g = new Game(new Player<>(whiteID, Game.Color.WHITE), new Player<>(blackID, Game.Color.BLACK));

//        System.out.println("instantiate game : "+(System.currentTimeMillis() - t1));

        g.ID = jsonGame.optString("id");
        return g;
    }

    /**
     * Make move in a game
    * @param move AN Text of move
     * @param uuid uuid of user
    * @param gameID uuid of game
    * @param layout
     */
    public static void sendMove(String move, String uuid, String gameID, String layout) {
        sendMove(move, uuid, gameID, layout, null);
    }

    /**
     * Make move in a game
     * @param move AN Text of move
     * @param uuid uuid of user
     * @param gameID uuid of game
     */
    static void sendMove(String move, String uuid, String gameID, String layout, MailCallback mcb){
        MailSend moveMail = new MailSend(mcb);
        moveMail.execute(Chesster, "move", move, "uuid", uuid, "game", gameID, "layout", layout);
    }

    static JSONObject refreshGameJSON(String gameID) {
        return refreshGameJSON(gameID,null);
    }
    private static JSONObject refreshGameJSON(String gameID, MailCallback mcb) {
        try {
            MailSend getGame = new MailSend(mcb);
            String response = getGame.execute(Chives, "action", "getgame", "game", gameID)
                    .get(NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS);
            return new JSONObject(response.trim());
        } catch (Exception e){
            return null;
        }
    }

    public static Game refreshGame(String gameID) {
        try {
            MailSend getGame = new MailSend();
            String response = getGame.execute(Chives, "action", "getgame", "game", gameID)
                                    .get(NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS);
            return JSONToGame(new JSONObject(response.trim()));
        } catch (Exception e){
            return null;
        }
    }

    static void leaveGame(String uuid, String gameID) {
        try{
            MailSend leaveGame = new MailSend();
            leaveGame.execute(Chives, "action", "leavegame", "game", gameID, "uuid", uuid);
            System.out.println("leaving game: "+gameID);
        } catch (Exception ignored){
        }
    }

    static void forfeitGame(String uuid, String gameID) {
        try{
            MailSend leaveGame = new MailSend();
            leaveGame.execute(Chives, "action", "forfeit", "game", gameID, "uuid", uuid);
            System.out.println("forfeiting game: "+gameID);
        } catch (Exception ignored){
        }
    }

    static void offerDraw(String uuid, String gameID, MailCallback mailCallback) {
        try{
            MailSend leaveGame = mailCallback == null ? new MailSend() : new MailSend(mailCallback);
            leaveGame.execute(Chives, "action", "offerdraw", "game", gameID, "uuid", uuid);
            System.out.println("offering draw: "+gameID);
        } catch (Exception ignored){
        }
    }

    static void rejectDraw(String uuid, String gameID, MailCallback mailCallback) {
        try{
            MailSend leaveGame = mailCallback == null ? new MailSend() : new MailSend(mailCallback);
            leaveGame.execute(Chives, "action", "rejectdraw", "game", gameID, "uuid", uuid);
            System.out.println("rejecting draw: "+gameID);
        } catch (Exception ignored){
        }
    }

    static void resetDraw(String uuid, String gameID, MailCallback mailCallback) {
        try{
            MailSend leaveGame = mailCallback == null ? new MailSend() : new MailSend(mailCallback);
            leaveGame.execute(Chives, "action", "resetdraw", "game", gameID, "uuid", uuid);
            System.out.println("rejecting draw: "+gameID);
        } catch (Exception ignored){
        }
    }

    static abstract class MailCallback {
        Object[] arguments;
        MailCallback(Object... args){
            arguments = args;
        }
        public abstract void before();
        public abstract void after(String s);
    }

    private static class MailSend extends AsyncTask<String, Void, String> {
        MailCallback callback = null;
        MailSend(MailCallback callback){
            this.callback = callback;
        }
        MailSend(){
        }
        /**
         * Make an HTTP POST request
         *
         * @param params first parameter will be assumed to be the URL, the following parameters will
         *               be treated as key-value pairs to be sent in the request body
         * @return
         */
        private HttpResponse getResponse(String... params) {
            HttpClient httpclient = HttpClients.createDefault();
            try {
                HttpPost httppost = new HttpPost(params[0]);
                List<NameValuePair> nameValuePairs = new ArrayList<>(params.length);
                for (int i = 1; i < params.length; i += 2) {
                    nameValuePairs.add(new BasicNameValuePair(params[i], params[i + 1]));
                }
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//                System.out.println(httppost+" "+httppost.getEntity());
                return httpclient.execute(httppost);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                HttpResponse response = getResponse(params);
                assert response != null;
                HttpEntity responseEntity = response.getEntity();
                return EntityUtils.toString(responseEntity);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (callback != null)
                callback.before();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (callback != null)
                callback.after(s);
        }
    }
}