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
import com.github.kaiwinter.nfcsonos.activity.main.TokenActivity;
import com.github.kaiwinter.nfcsonos.databinding.ActivityLoginBinding;
import com.github.kaiwinter.nfcsonos.rest.APIError;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.login.LoginService;
import com.github.kaiwinter.nfcsonos.rest.login.model.AccessToken;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesTokenStore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public final class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private ActivityLoginBinding binding;

    private CustomTabsClient customTabsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "Creating CustomTabsServiceConnection");
        CustomTabsServiceConnection customTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.e(TAG, "CustomTabsServiceConnection.onServiceDisconnected");
                customTabsClient = null;
            }

            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                Log.e(TAG, "CustomTabsServiceConnection.onCustomTabsServiceConnected");
                customTabsClient.warmup(0);
                LoginActivity.this.customTabsClient = customTabsClient;
            }
        };

        Log.e(TAG, "CustomTabsClient.bindCustomTabsService");
        CustomTabsClient.bindCustomTabsService(
                this,
                "com.android.chrome",
                customTabsServiceConnection);

        Log.e(TAG, "CustomTabsClient.done");

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferencesTokenStore tokenstore = new SharedPreferencesTokenStore(this);
        // tokenstore.setTokens(null, null, 0);
        if (TextUtils.isEmpty(tokenstore.getAccessToken())) {
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
        displayLoading("Starting browser for login");

        String url = getString(R.string.authorization_endpoint_uri)
                + "?client_id=" + getString(R.string.client_id)
                + "&response_type=code"
                + "&state=random"
                + "&scope=" + getString(R.string.authorization_scope)
                + "&redirect_uri=" + getString(R.string.redirect_uri);

        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(createSession(Uri.parse(url))).build();
        customTabsIntent.intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        try {
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } catch (ActivityNotFoundException e) {
            hideLoadingState("The login process expects Chrome to be installed. Please install (or activate) Chrome.");
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
                hideLoadingState("Authorization cancelled");
            }
            return;
        }
        if (data.getQueryParameterNames().contains("code")) {
            displayLoading("Lade Access Token");

            String code = data.getQueryParameter("code");
            LoginService service = ServiceFactory.createLoginService();

            String basic = getString(R.string.client_id) + ":" + getString(R.string.client_secret);
            String authHeader = "Basic " + Base64.encodeToString(basic.getBytes(), Base64.NO_WRAP);
            Call<AccessToken> call = service.getAccessToken(authHeader, "authorization_code", code, getString(R.string.redirect_uri));
            call.enqueue(new Callback<AccessToken>() {
                @Override
                public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {

                    if (response.code() == 200) {
                        SharedPreferencesTokenStore tokenstore = new SharedPreferencesTokenStore(LoginActivity.this);
                        AccessToken body = response.body();
                        long expiresAt = System.currentTimeMillis() + body.expiresIn * 1000;
                        tokenstore.setTokens(body.refreshToken, body.accessToken, expiresAt);
                        switchToMainActivity();
                    } else {
                        APIError apiError = ServiceFactory.parseError(response);
                        String message = getString(R.string.login_error, apiError.error + " (" + response.code() + ", " + apiError.errorDescription + ")");
                        hideLoadingState(message);

                    }
                    hideLoadingState(null);
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
        startActivity(new Intent(this, TokenActivity.class));
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
