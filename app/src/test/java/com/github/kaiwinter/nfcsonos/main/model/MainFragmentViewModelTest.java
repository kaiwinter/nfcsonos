package com.github.kaiwinter.nfcsonos.main.model;

import android.content.Intent;
import android.view.View;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;
import com.github.kaiwinter.nfcsonos.util.SingleLiveEvent;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MainFragmentViewModel}.
 */
public class MainFragmentViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule();

    /**
     * Tests the redirect to the {@link com.github.kaiwinter.nfcsonos.login.LoginActivity} if there is no access token.
     */
    @Test
    public void notLoggedIn_switchToLoginActivity() {
        SharedPreferencesStore sharedPreferences = when(mock(SharedPreferencesStore.class).getAccessToken()).thenReturn("").getMock();
        MainFragmentViewModel viewModel = new MainFragmentViewModel(sharedPreferences, null, null, null);

        viewModel.navigateToLoginActivity = mock(SingleLiveEvent.class);
        viewModel.createInitialState(new Intent(), null);

        verify(viewModel.navigateToLoginActivity).call();
    }

    /**
     * Tests the redirect to the {@link com.github.kaiwinter.nfcsonos.discover.DiscoverActivity} if no houshold/group selection exists.
     */
    @Test
    public void noHouseholdSelection_switchToDiscoverActivity() {
        SharedPreferencesStore sharedPreferences = when(mock(SharedPreferencesStore.class).getAccessToken()).thenReturn("123").getMock();
        MainFragmentViewModel viewModel = new MainFragmentViewModel(sharedPreferences, null, null, null);

        viewModel.navigateToDiscoverActivity = mock(SingleLiveEvent.class);
        viewModel.createInitialState(new Intent(), null);

        verify(viewModel.navigateToDiscoverActivity).call();
    }

    @Test
    public void pause_200() {
        runWithMockWebServer("/pause_200.json", 200, () -> {
            SharedPreferencesStore sharedPreferences = mock(SharedPreferencesStore.class);
            when(sharedPreferences.getAccessToken()).thenReturn("access-token").getMock();
            when(sharedPreferences.getGroupId()).thenReturn("group-id").getMock();
            AccessTokenManager accessTokenManager = when(mock(AccessTokenManager.class).accessTokenRefreshNeeded()).thenReturn(false).getMock();
            ServiceFactory serviceFactory = new ServiceFactory("http://localhost:8080");
            MainFragmentViewModel viewModel = new MainFragmentViewModel(sharedPreferences, accessTokenManager, null, serviceFactory);

            viewModel.pause();
            assertEquals(View.VISIBLE, viewModel.loadingContainerVisibility.getValue().intValue());

            await().atMost(10, TimeUnit.SECONDS).until(() -> viewModel.playButtonVisibility.getValue() != View.GONE);

            // No error message
            assertNull(viewModel.errorMessageMutableLiveData.getValue());

            // Loading container is INVISIBLE
            assertEquals(View.INVISIBLE, viewModel.loadingContainerVisibility.getValue().intValue());
        });
    }

    private void runWithMockWebServer(String file, int httpCode, Runnable runnable) {
        try {
            InputStream inputStream = MainFragmentViewModelTest.class.getResourceAsStream(file);
            Buffer buffer = new Buffer().readFrom(inputStream);

            MockWebServer mockWebServer = new MockWebServer();
            mockWebServer.enqueue(new MockResponse().setBody(buffer).setResponseCode(httpCode));
            mockWebServer.start(8080);
            runnable.run();
            mockWebServer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
