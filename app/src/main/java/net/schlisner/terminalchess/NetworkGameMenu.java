package net.schlisner.terminalchess;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
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
    CheckBox publicGame;
    EditText gameIDInput;

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
        publicGame = (CheckBox) findViewById(R.id.publicGame);

        gameIDInput = (EditText) findViewById(R.id.gameUUIDET);

        startGameBtn = (Button)findViewById(R.id.startGame);
        startGameBtn.setVisibility(View.INVISIBLE);

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
        updateHandler.post(new Runnable() {
            @Override
            public void run() {
                refreshList();
            }
        });


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
        gameIDInput.setVisibility(View.VISIBLE);
        startGameBtn.setVisibility(View.VISIBLE);
        gameList.setVisibility(View.VISIBLE);

    }

    private JSONObject createdGame = null;
    public void createGame(View v){
        gameIDInput.setInputType(InputType.TYPE_NULL);
        startGameBtn.setVisibility(View.VISIBLE);
        final Activity context = this;
        PostOffice.createGame(uuid, publicGame.isChecked(), new PostOffice.MailCallback() {
            @Override
            public void before() {

            }

            @Override
            public void after(String s) {
                try {
                    createdGame = new JSONObject(s);
                    gameIDInput.setText(createdGame.getString("id"));
                    gameIDInput.selectAll();
                } catch (Exception e){
                    e.printStackTrace();
                    createdGame = null;
                    Toast.makeText(getApplicationContext(), "Could not create game.", Toast.LENGTH_SHORT).show();
                    startGameBtn.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
    public void startGame(View v){
        if (createdGame != null){
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
