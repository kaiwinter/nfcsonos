package com.github.kaiwinter.nfcsonos.login;

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

import com.github.kaiwinter.nfcsonos.BuildConfig;
import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.databinding.ActivityLoginBinding;
import com.github.kaiwinter.nfcsonos.main.MainActivity;
import com.github.kaiwinter.nfcsonos.rest.LoginService;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.model.APIError;
import com.github.kaiwinter.nfcsonos.rest.model.AccessToken;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;
import com.github.kaiwinter.nfcsonos.util.UserMessage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public final class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private static final String AUTHORIZATION_ENDPOINT_URI = "https://api.sonos.com/login/v3/oauth";
    private static final String REDIRECT_URI = "https://vorlesungsfrei.de/nfcsonos-redirect.php";

    private ActivityLoginBinding binding;

    private SharedPreferencesStore sharedPreferencesStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferencesStore = new SharedPreferencesStore(this);
        sharedPreferencesStore.setTokens(null, null, -1);
        sharedPreferencesStore.setHouseholdAndGroup(null, null, null);

        CustomTabsServiceConnection customTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }

            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                customTabsClient.warmup(0);
            }
        };

        CustomTabsClient.bindCustomTabsService(
                this,
                "com.android.chrome",
                customTabsServiceConnection);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        binding.startAuth.setOnClickListener(__ -> loginClicked());
        setContentView(binding.getRoot());

        if (TextUtils.isEmpty(sharedPreferencesStore.getAccessToken())) {
            return;
        }

        AccessTokenManager accessTokenManager = new AccessTokenManager(this);
        if (accessTokenManager.accessTokenRefreshNeeded()) {
            accessTokenManager.refreshAccessToken(this::switchToMainActivity, this::hideLoadingState);
            return;
        }

        switchToMainActivity();
    }

    /**
     * Called from the login button defined in the XML.
     */
    public void loginClicked() {
        displayLoading(getString(R.string.starting_browser_for_login));

        String url = AUTHORIZATION_ENDPOINT_URI
                + "?client_id=" + BuildConfig.CLIENT_ID
                + "&response_type=code"
                + "&state=random"
                + "&scope=playback-control-all"
                + "&redirect_uri=" + REDIRECT_URI;

        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        customTabsIntent.intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } catch (ActivityNotFoundException e) {
            UserMessage userMessage = UserMessage.create(R.string.couldnt_start_browser);
            hideLoadingState(userMessage);
        }
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
                UserMessage userMessage = UserMessage.create(R.string.authorization_cancelled);
                hideLoadingState(userMessage);
            }
            return;
        }
        if (data.getQueryParameterNames().contains("code")) {
            displayLoading(getString(R.string.loading_access_token));

            String code = data.getQueryParameter("code");
            LoginService service = ServiceFactory.createLoginService();

            String basic = BuildConfig.CLIENT_ID + ":" + BuildConfig.CLIENT_SECRET;
            String authHeader = "Basic " + Base64.encodeToString(basic.getBytes(), Base64.NO_WRAP);
            Call<AccessToken> call = service.getAccessToken(authHeader, "authorization_code", code, REDIRECT_URI);
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {

                    if (response.isSuccessful()) {
                        AccessToken body = response.body();
                        long expiresAt = System.currentTimeMillis() + body.expiresIn * 1000;
                        sharedPreferencesStore.setTokens(body.refreshToken, body.accessToken, expiresAt);
                        switchToMainActivity();
                    } else {
                        APIError apiError = ServiceFactory.parseError(response);
                        UserMessage userMessage = UserMessage.create(apiError);
                        hideLoadingState(userMessage);
                    }
                }

                @Override
                public void onFailure(Call<AccessToken> call, Throwable t) {
                    UserMessage userMessage = UserMessage.create(t.getMessage());
                    hideLoadingState(userMessage);
                }
            });

        } else if (data.getQueryParameterNames().contains("error")) {
            String error = data.getQueryParameter("error");
            UserMessage userMessage = UserMessage.create(error);
            hideLoadingState(userMessage);
        }
    }

    private void switchToMainActivity() {
        Log.i(TAG, "User is authenticated, proceeding to token activity");
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

    private void hideLoadingState(UserMessage userMessage) {
        String message = userMessage.getMessage(this);
        runOnUiThread(() -> {
            if (!TextUtils.isEmpty(message)) {
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorDescription.setText(message);
            }
            binding.loadingContainer.setVisibility(View.INVISIBLE);
            binding.authContainer.setVisibility(View.VISIBLE);
        });
    }
}
