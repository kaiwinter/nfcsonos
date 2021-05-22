package com.github.kaiwinter.nfcsonos.activity.login;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.activity.main.MainActivity;
import com.github.kaiwinter.nfcsonos.databinding.ActivityLoginBinding;
import com.github.kaiwinter.nfcsonos.rest.LoginService;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.model.AccessToken;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public final class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private static final String AUTHORIZATION_ENDPOINT_URI = "https://api.sonos.com/login/v3/oauth";
    private static final String REDIRECT_URI = "http://localhost/com.github.kaiwinter.nfcsonos";

    private ActivityLoginBinding binding;

    private CustomTabsClient customTabsClient;

    private SharedPreferencesStore sharedPreferencesStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferencesStore = new SharedPreferencesStore(this);

        Log.i(TAG, "Creating CustomTabsServiceConnection");
        CustomTabsServiceConnection customTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.i(TAG, "CustomTabsServiceConnection.onServiceDisconnected");
                customTabsClient = null;
            }

            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                Log.i(TAG, "CustomTabsServiceConnection.onCustomTabsServiceConnected");
                customTabsClient.warmup(0);
                LoginActivity.this.customTabsClient = customTabsClient;
            }
        };

        Log.i(TAG, "CustomTabsClient.bindCustomTabsService");
        CustomTabsClient.bindCustomTabsService(
                this,
                "com.android.chrome",
                customTabsServiceConnection);

        Log.i(TAG, "CustomTabsClient.done");

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // tokenstore.setTokens(null, null, 0);
        if (TextUtils.isEmpty(sharedPreferencesStore.getAccessToken())) {
            return;
        }

        AccessTokenManager accessTokenManager = new AccessTokenManager(this);
        if (accessTokenManager.accessTokenRefreshNeeded()) {
            accessTokenManager.refreshAccessToken(this, this::switchToMainActivity, this::hideLoadingState);
            return;
        }

        switchToMainActivity();
    }

    /**
     * Called from the login button defined in the XML.
     */
    public void loginClicked(View view) {
        displayLoading(getString(R.string.starting_browser_for_login));

        String url = AUTHORIZATION_ENDPOINT_URI
                + "?client_id=" + getString(R.string.client_id)
                + "&response_type=code"
                + "&state=random"
                + "&scope=playback-control-all"
                + "&redirect_uri=" + REDIRECT_URI;

        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(createSession(Uri.parse(url))).build();
        customTabsIntent.intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        try {
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } catch (ActivityNotFoundException e) {
            hideLoadingState(getString(R.string.couldnt_start_browser));
        }
    }

    public CustomTabsSession createSession(Uri possibleUri) {
        if (customTabsClient == null) {
            return null;
        }

        CustomTabsSession session = customTabsClient.newSession(null);
        if (session == null) {
            return null;
        }
        session.mayLaunchUrl(possibleUri, null, null);

        return session;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        Uri data = intent.getData();
        if (data == null) {
            if (binding.loadingContainer.getVisibility() == View.VISIBLE) {
                hideLoadingState(getString(R.string.authorization_cancelled));
            }
            return;
        }
        if (data.getQueryParameterNames().contains("code")) {
            displayLoading(getString(R.string.loading_access_token));

            String code = data.getQueryParameter("code");
            LoginService service = ServiceFactory.createLoginService();

            String basic = getString(R.string.client_id) + ":" + getString(R.string.client_secret);
            String authHeader = "Basic " + Base64.encodeToString(basic.getBytes(), Base64.NO_WRAP);
            Call<AccessToken> call = service.getAccessToken(authHeader, "authorization_code", code, REDIRECT_URI);
            call.enqueue(new Callback<AccessToken>() {
                @Override
                public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {

                    if (response.isSuccessful()) {
                        AccessToken body = response.body();
                        long expiresAt = System.currentTimeMillis() + body.expiresIn * 1000;
                        sharedPreferencesStore.setTokens(body.refreshToken, body.accessToken, expiresAt);
                        switchToMainActivity();
                    } else {
                        String message = ServiceFactory.handleError(LoginActivity.this, response);
                        hideLoadingState(message);
                    }
                }

                @Override
                public void onFailure(Call<AccessToken> call, Throwable t) {
                    hideLoadingState(t.getMessage());
                }
            });

        } else if (data.getQueryParameterNames().contains("error")) {
            String error = data.getQueryParameter("error");
            hideLoadingState(error);
        }
    }

    private void switchToMainActivity() {
        Log.i(TAG, "User is already authenticated, proceeding to token activity");
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void displayLoading(String loadingMessage) {
        runOnUiThread(() -> {
            binding.loadingContainer.setVisibility(View.VISIBLE);
            binding.authContainer.setVisibility(View.GONE);

            binding.loadingDescription.setText(loadingMessage);
        });
    }

    private void hideLoadingState(String errormessage) {
        runOnUiThread(() -> {
            if (!TextUtils.isEmpty(errormessage)) {
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorDescription.setText(errormessage);
            }
            binding.loadingContainer.setVisibility(View.INVISIBLE);
            binding.authContainer.setVisibility(View.VISIBLE);
        });
    }
}
