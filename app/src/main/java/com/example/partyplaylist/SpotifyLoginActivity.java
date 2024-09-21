package com.example.partyplaylist;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpotifyLoginActivity extends AppCompatActivity {

    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String CLIENT_ID = "9e55757a811a432c88d740c04711f5a0";
    private static final String CLIENT_SECRET = "d38dcbead2224dc9a50b6a978e83c295"; // Add your client secret here
    private static final String REDIRECT_URI = "http://localhost/callback";
    private static final String RESPONSE_TYPE = "code";
    private static final String SCOPE = "user-read-email user-library-read user-read-private user-library-modify user-top-read user-read-recently-played user-follow-read user-follow-modify user-read-playback-state user-modify-playback-state playlist-read-private playlist-read-collaborative playlist-modify-public playlist-modify-private app-remote-control streaming user-read-playback-position user-read-currently-playing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spotifylogin);

        EditText email = findViewById(R.id.editTextTextEmailAddress);
        EditText password = findViewById(R.id.editTextTextPassword);
        Button loginButton = findViewById(R.id.button7);
        Button loginWithoutPasswordButton = findViewById(R.id.button8);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAuthFlow();
            }
        });

        loginWithoutPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle login without password (if applicable)
            }
        });

        // Handle any incoming intents
        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void startAuthFlow() {
        String url = AUTH_URL + "?client_id=" + CLIENT_ID +
                "&response_type=" + RESPONSE_TYPE +
                "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                "&scope=" + Uri.encode(SCOPE);
        Log.d("SpotifyAuth", "Auth URL: " + url); // Add this line
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void saveAccessToken(String token) {
        SharedPreferences prefs = getSharedPreferences("spotify_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("access_token", token);
        boolean success = editor.commit(); // Use commit to check if saving was successful
        if (success) {
            Log.d("SpotifyAuth", "Access token saved: " + token);
        } else {
            Log.e("SpotifyAuth", "Failed to save access token.");
        }
    }

    private void saveRefreshToken(String token) {
        SharedPreferences prefs = getSharedPreferences("spotify_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("refresh_token", token);
        boolean success = editor.commit(); // Use commit to check if saving was successful
        if (success) {
            Log.d("SpotifyAuth", "Refresh token saved: " + token);
        } else {
            Log.e("SpotifyAuth", "Failed to save refresh token.");
        }
    }

    private void handleIntent(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            String code = uri.getQueryParameter("code");
            if (code != null) {
                Log.d("SpotifyAuth", "Authorization code received: " + code);
                fetchAccessToken(code);
            } else {
                Log.e("SpotifyAuth", "Authorization code not found in the URI.");
            }
        } else {
            Log.e("SpotifyAuth", "Redirect URI does not match or URI is null.");
        }
    }

    private void fetchAccessToken(String code) {
        OkHttpClient client = new OkHttpClient();
        String tokenUrl = "https://accounts.spotify.com/api/token";
        String authHeader = "Basic " + Base64.encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(), Base64.NO_WRAP);

        RequestBody body = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .build();

        Request request = new Request.Builder()
                .url(tokenUrl)
                .post(body)
                .addHeader("Authorization", authHeader)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.e("SpotifyAuth", "Token request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();

                    // Parse the response and get the access token and refresh token
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        Log.d("SpotifyAuth", "Response Body: " + responseBody);

                        String accessToken = jsonResponse.optString("access_token");
                        String refreshToken = jsonResponse.optString("refresh_token");

                        if (accessToken != null && !accessToken.isEmpty()) {
                            saveAccessToken(accessToken);

                            // Save the refresh token if present
                            if (refreshToken != null && !refreshToken.isEmpty()) {
                                Log.d("refreshtoken",refreshToken.toString());
                                saveRefreshToken(refreshToken);
                            }

                            // Redirect to the next activity with the access token
                            Intent intent = new Intent(SpotifyLoginActivity.this, HomePageActivity.class);
                            intent.putExtra("ACCESS_TOKEN", accessToken);
                            startActivity(intent);
                        } else {
                            Log.e("SpotifyAuth", "Access token is null or empty.");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("SpotifyAuth", "Error parsing JSON response: " + e.getMessage());
                    }
                } else {
                    // Handle response error
                    Log.e("SpotifyAuth", "Error response: " + response.message());
                }
            }
        });
    }
}
