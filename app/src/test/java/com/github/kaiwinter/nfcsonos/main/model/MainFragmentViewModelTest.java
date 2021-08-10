package com.github.kaiwinter.nfcsonos.main.model;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
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

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
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
     * Tests the case that the group ID isn't valid anymore. This happens after the Sonos was unplugged.
     * In this test there is no groupCoordinatorId in the SharedPreference which can be used to automatically
     * find the new group ID. As a result the user should be redirected to the DiscoveryActivity.
     */
    @Test
    public void groupGone_switchToDiscoverActivity() {
        runWithMockWebServer(port -> {
            SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStoreMockBuilder().withAccessToken().withGroupId().withHouseholdId().build();
            AccessTokenManager accessTokenManager = when(mock(AccessTokenManager.class).accessTokenRefreshNeeded()).thenReturn(false).getMock();
            ServiceFactory serviceFactory = new ServiceFactory("http://localhost:" + port);
            MainFragmentViewModel viewModel = new MainFragmentViewModel(sharedPreferencesStore, accessTokenManager, null, serviceFactory);

            viewModel.navigateToDiscoverActivity = mock(SingleLiveEvent.class);
            viewModel.createInitialState(mock(Intent.class), null);
            verify(viewModel.navigateToDiscoverActivity, timeout(3_000)).postValue(any(RetryAction.class));
        }, new Dispatcher() {
            @NonNull
            @Override
            public MockResponse dispatch(@NonNull RecordedRequest recordedRequest) {
                if (recordedRequest.getPath().endsWith("/group-id/playbackMetadata")) {
                    return new MockResponse().setBody(fileAsBuffer("/410_gone.json")).setResponseCode(410).setHeader("Content-Type", "application/json");
                }
                return new MockResponse();
            }
        });
    }

    /**
     * Tests the case that the group ID isn't valid anymore. This happens after the Sonos was unplugged.
     * In this test there is a groupCoordinatorId in the SharedPreference which is used to automatically
     * find the new group ID. As a result the groupCoordinatorId should be used to find the new Group ID.
     */
    @Test
    public void groupGone_LookupGroupId() {
        runWithMockWebServer(port -> {
            SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStoreMockBuilder().withAccessToken().withGroupId().withHouseholdId().withGroupCoordinatorId().build();
            AccessTokenManager accessTokenManager = when(mock(AccessTokenManager.class).accessTokenRefreshNeeded()).thenReturn(false).getMock();
            ServiceFactory serviceFactory = new ServiceFactory("http://localhost:" + port);
            MainFragmentViewModel viewModel = new MainFragmentViewModel(sharedPreferencesStore, accessTokenManager, null, serviceFactory);

            viewModel.navigateToDiscoverActivity = mock(SingleLiveEvent.class);
            viewModel.createInitialState(mock(Intent.class), null);
            verify(sharedPreferencesStore, timeout(3_000)).setHouseholdAndGroup(eq("household-id"), eq("RINCON_123456:NEWID"), eq("RINCON_123456"));
        }, new Dispatcher() {
            @NonNull
            @Override
            public MockResponse dispatch(@NonNull RecordedRequest recordedRequest) {
                if (recordedRequest.getPath().endsWith("/group-id/playbackMetadata")) {
                    return new MockResponse().setBody(fileAsBuffer("/410_gone.json")).setResponseCode(410).setHeader("Content-Type", "application/json");

                } else if (recordedRequest.getPath().endsWith("groups")) {
                    return new MockResponse().setBody(fileAsBuffer("/groups_200.json")).setResponseCode(200).setHeader("Content-Type", "application/json");
                }
                return new MockResponse();
            }
        });
    }

    private Buffer fileAsBuffer(String file) {
        InputStream inputStream = MainFragmentViewModelTest.class.getResourceAsStream(file);
        try {
            return new Buffer().readFrom(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void pause_200() {
        runWithMockWebServer(port -> {
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
        }, new Dispatcher() {
            @NonNull
            @Override
            public MockResponse dispatch(@NonNull RecordedRequest recordedRequest) {
                if (recordedRequest.getPath().endsWith("/playback/pause")) {
                    return new MockResponse().setBody(fileAsBuffer("/pause_200.json")).setResponseCode(200).setHeader("Content-Type", "application/json");
                }
                return new MockResponse();
            }
        });
    }

    /**
     * Runs the <code>runnable</code> after the MockWebServer is started. The <code>responsesWithCodes</code> are enqueued as MockWebServer responses.
     *
     * @param runnable   gets run after the MockWebServer is started
     * @param dispatcher
     */
    private void runWithMockWebServer(Consumer<Integer> runnable, Dispatcher dispatcher) {
        try (MockWebServer mockWebServer = new MockWebServer()) {
            mockWebServer.setDispatcher(dispatcher);
            mockWebServer.start();
            runnable.accept(mockWebServer.getPort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class SharedPreferencesStoreMockBuilder {
        boolean withAccessToken;
        boolean withGroupId;
        boolean withHouseholdId;
        boolean withGroupCoordinatorId;

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

        SharedPreferencesStoreMockBuilder withGroupCoordinatorId() {
            this.withGroupCoordinatorId = true;
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
            if (withGroupCoordinatorId) {
                when(sharedPreferences.getGroupCoordinatorId()).thenReturn("RINCON_123456").getMock();
            }
            return sharedPreferences;
        }
    }
}
