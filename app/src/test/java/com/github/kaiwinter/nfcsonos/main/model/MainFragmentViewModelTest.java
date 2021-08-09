package com.github.kaiwinter.nfcsonos.main.model;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.view.View;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.core.util.Consumer;

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
        SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStoreMockBuilder().build();
        MainFragmentViewModel viewModel = new MainFragmentViewModel(sharedPreferencesStore, null, null, null);

        viewModel.navigateToLoginActivity = mock(SingleLiveEvent.class);
        viewModel.createInitialState(new Intent(), null);

        verify(viewModel.navigateToLoginActivity).call();
    }

    /**
     * Tests the redirect to the {@link com.github.kaiwinter.nfcsonos.discover.DiscoverActivity} if no houshold/group selection exists.
     */
    @Test
    public void noHouseholdSelection_switchToDiscoverActivity() {
        SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStoreMockBuilder().withAccessToken().build();
        MainFragmentViewModel viewModel = new MainFragmentViewModel(sharedPreferencesStore, null, null, null);

        viewModel.navigateToDiscoverActivity = mock(SingleLiveEvent.class);
        viewModel.createInitialState(new Intent(), null);

        verify(viewModel.navigateToDiscoverActivity).call();
    }

    /**
     * Tests the case that the household ID isn't valid anymore. This happens after the Sonos was unplugged.
     */
    @Test
    public void householdGone_switchToDiscoverActivity() {
        runWithMockWebServer("/410_gone.json", 410, port -> {
            SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStoreMockBuilder().withAccessToken().withGroupId().withHouseholdId().build();
            AccessTokenManager accessTokenManager = when(mock(AccessTokenManager.class).accessTokenRefreshNeeded()).thenReturn(false).getMock();
            ServiceFactory serviceFactory = new ServiceFactory("http://localhost:" + port);
            MainFragmentViewModel viewModel = new MainFragmentViewModel(sharedPreferencesStore, accessTokenManager, null, serviceFactory);

            viewModel.navigateToDiscoverActivity = mock(SingleLiveEvent.class);
            viewModel.createInitialState(mock(Intent.class), null);
            verify(viewModel.navigateToDiscoverActivity, timeout(100000)).postValue(any(RetryAction.class));
        });
    }

    @Test
    public void pause_200() {
        runWithMockWebServer("/pause_200.json", 200, port -> {
            SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStoreMockBuilder().withAccessToken().withGroupId().build();
            AccessTokenManager accessTokenManager = when(mock(AccessTokenManager.class).accessTokenRefreshNeeded()).thenReturn(false).getMock();
            ServiceFactory serviceFactory = new ServiceFactory("http://localhost:" + port);
            MainFragmentViewModel viewModel = new MainFragmentViewModel(sharedPreferencesStore, accessTokenManager, null, serviceFactory);

            viewModel.pause();
            assertEquals(View.VISIBLE, viewModel.loadingContainerVisibility.getValue().intValue());

            await().atMost(10, TimeUnit.SECONDS).until(() -> viewModel.playButtonVisibility.getValue() != View.GONE);

            // No error message
            assertNull(viewModel.errorMessageMutableLiveData.getValue());

            // Loading container is INVISIBLE
            assertEquals(View.INVISIBLE, viewModel.loadingContainerVisibility.getValue().intValue());
        });
    }

    private void runWithMockWebServer(String file, int httpCode, Consumer<Integer> runnable) {
        try {
            InputStream inputStream = MainFragmentViewModelTest.class.getResourceAsStream(file);
            Buffer buffer = new Buffer().readFrom(inputStream);

            MockWebServer mockWebServer = new MockWebServer();
            mockWebServer.enqueue(new MockResponse().setBody(buffer).setResponseCode(httpCode).setHeader("Content-Type", "application/json"));
            mockWebServer.start();
            runnable.accept(mockWebServer.getPort());
            mockWebServer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class SharedPreferencesStoreMockBuilder {
        boolean withAccessToken;
        boolean withGroupId;
        boolean withHouseholdId;

        SharedPreferencesStoreMockBuilder withAccessToken() {
            this.withAccessToken = true;
            return this;
        }

        SharedPreferencesStoreMockBuilder withGroupId() {
            this.withGroupId = true;
            return this;
        }

        SharedPreferencesStoreMockBuilder withHouseholdId() {
            this.withHouseholdId = true;
            return this;
        }

        SharedPreferencesStore build() {
            SharedPreferencesStore sharedPreferences = mock(SharedPreferencesStore.class);
            if (withAccessToken) {
                when(sharedPreferences.getAccessToken()).thenReturn("access-token").getMock();
            }
            if (withGroupId) {
                when(sharedPreferences.getGroupId()).thenReturn("group-id").getMock();
            }
            if (withHouseholdId) {
                when(sharedPreferences.getHouseholdId()).thenReturn("household-id").getMock();
            }
            return sharedPreferences;
        }
    }
}
