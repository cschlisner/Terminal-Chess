package net.schlisner.terminalchess;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NetworkGameMenu extends AppCompatActivity {
    String uuid;
    SharedPreferences sharedPref;

    Button joinGameBtn;
    Button createGameBtn;
    Button startGameBtn;
    TextView gameID;
    EditText gameIDInput;
    CheckBox publicGame;

    ListView gameList;
    List<JSONObject> gamesJSONList;
    ChessGameListAdapter cgla;

    HandlerThread ht;
    Handler updateHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.hide();
        setContentView(R.layout.activity_network_game_menu);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        sharedPref = this.getSharedPreferences("Dank Memes(c)", MODE_PRIVATE);
        uuid = sharedPref.getString("uuid", "");

        joinGameBtn = (Button)findViewById(R.id.joinGame);
        createGameBtn = (Button)findViewById(R.id.createGameBtn);

        gameID = (TextView) findViewById(R.id.gameUUIDTV);
        gameID.setVisibility(View.INVISIBLE);

        gameID.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("simple text", ((TextView)v).getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "Game ID copied to clipboard", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        gameIDInput = (EditText) findViewById(R.id.gameUUIDET);
        gameIDInput.setVisibility(View.INVISIBLE);

        startGameBtn = (Button)findViewById(R.id.startGame);
        startGameBtn.setVisibility(View.INVISIBLE);

        publicGame = (CheckBox) findViewById(R.id.checkBoxPublic);

        gameList = (ListView)findViewById(R.id.lobby_gameListView);

        gameList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                joinGame(joinGameBtn);
                // why you have to be mad?
                JSONObject geme = (JSONObject)gameList.getAdapter().getItem(position);
                gameIDInput.setText(geme.optString("id"));
            }
        });

        gamesJSONList = new ArrayList<>();
        cgla = new ChessGameListAdapter(getApplicationContext(), gamesJSONList, uuid);
        gameList.setAdapter(cgla);

        ht = new HandlerThread("networkupdate");
        ht.start();
        updateHandler = new Handler(ht.getLooper());

    }

    @Override
    public void onResume(){
        super.onResume();
        ChessUpdater.cancelAlarm(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        ChessUpdater.setAlarm(this);
    }

    public void instantMatch(View v){
        // launch lobby
        Intent i = new Intent(this, NetworkLobbyActivity.class).putExtra("uuid", uuid);
        startActivity(i);
        finish();
    }

    public void joinGame(View v){
        gameIDInput.setText("");
        gameID.setVisibility(View.INVISIBLE);
        gameIDInput.setVisibility(View.VISIBLE);
        startGameBtn.setVisibility(View.VISIBLE);
        gameList.setVisibility(View.VISIBLE);

        updateHandler.post(new Runnable() {
            @Override
            public void run() {
                refreshList();
            }
        });
    }

    private JSONObject createdGame;
    public void createGame(View v){
        gameID.setVisibility(View.VISIBLE);
        gameIDInput.setVisibility(View.INVISIBLE);
        startGameBtn.setVisibility(View.VISIBLE);
        gameList.setVisibility(View.INVISIBLE);
        final Activity context = this;
        PostOffice.createGame(uuid, publicGame.isChecked(), new PostOffice.MailCallback() {
            @Override
            public void before() {

            }

            @Override
            public void after(String s) {
                try {
                    createdGame = new JSONObject(s);
                    gameID.setText(createdGame.getString("id"));
                    Toast.makeText(getApplicationContext(), "Long-press Game ID to copy", Toast.LENGTH_SHORT).show();
                } catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Could not create game.", Toast.LENGTH_SHORT).show();
                    startGameBtn.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
    public void startGame(View v){
        if (gameID.getVisibility() == View.VISIBLE && createdGame != null){
            Intent i = new Intent(this, GameActivity.class);
            i.putExtra("uuid", uuid);
            i.putExtra("opponent", GameActivity.OPPONENT_NETWORK);
            i.putExtra("gameJSON", createdGame.toString());
            i.putExtra("startFromExt", true);
            startActivity(i);
            finish();
        }
        else {
            String gameUUID = gameIDInput.getText().toString();

            if (gameUUID.isEmpty()){
                Toast.makeText(this, "Enter Valid Game ID", Toast.LENGTH_SHORT).show();
                return;
            }
            final Activity context = this;
            PostOffice.joinGame(uuid, gameUUID, new PostOffice.MailCallback() {
                @Override
                public void before() {

                }

                @Override
                public void after(String s) {
                    try {
                        JSONObject join = new JSONObject(s);
                        if (s.equals("ERR_GAME_FULL")) {
                            Toast.makeText(getApplicationContext(), "Game is full.", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Intent i = new Intent(context, GameActivity.class);
                            i.putExtra("uuid", uuid);
                            i.putExtra("opponent", GameActivity.OPPONENT_NETWORK);
                            i.putExtra("gameJSON", s);
                            i.putExtra("startFromExt", true);
                            startActivity(i);
                            context.finish();
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Could not join game.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void refreshList(){
        try {
            PostOffice.listPublicGames(uuid, new PostOffice.MailCallback() {
                @Override
                public void before() {

                }

                @Override
                public void after(String response) {
                    try {
                        JSONArray gamesJSON = new JSONArray(response);
                        System.out.println(response);

                        cgla.clear();
                        for (int i = 0; i < gamesJSON.length(); ++i)
                            cgla.add(gamesJSON.getJSONObject(i));
                        cgla.notifyDataSetChanged();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
