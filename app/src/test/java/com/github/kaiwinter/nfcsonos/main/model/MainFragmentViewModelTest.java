package com.github.kaiwinter.nfcsonos.main.model;

import android.content.Intent;

import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;
import com.github.kaiwinter.nfcsonos.util.SingleLiveEvent;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MainFragmentViewModel}.
 */
public class MainFragmentViewModelTest {

    /**
     * Tests the redirect to the {@link com.github.kaiwinter.nfcsonos.login.LoginActivity} if there is no access token.
     */
    @Test
    public void notLoggedIn_switchToLoginActivity() {
        SharedPreferencesStore sharedPreferences = when(mock(SharedPreferencesStore.class).getAccessToken()).thenReturn("").getMock();
        MainFragmentViewModel viewModel = new MainFragmentViewModel(sharedPreferences, null, null);

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
        MainFragmentViewModel viewModel = new MainFragmentViewModel(sharedPreferences, null, null);

        viewModel.navigateToDiscoverActivity = mock(SingleLiveEvent.class);
        viewModel.createInitialState(new Intent(), null);

        verify(viewModel.navigateToDiscoverActivity).call();
    }
}