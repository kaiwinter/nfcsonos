package com.github.kaiwinter.nfcsonos.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.github.kaiwinter.nfcsonos.databinding.FragmentSettingsBinding;
import com.github.kaiwinter.nfcsonos.discover.DiscoverActivity;
import com.github.kaiwinter.nfcsonos.login.LoginActivity;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;

/**
 * The settings fragment.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferencesStore sharedPreferencesStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferencesStore = new SharedPreferencesStore(getActivity().getApplicationContext());

        binding = FragmentSettingsBinding.inflate(getLayoutInflater());
        binding.discoverButton.setOnClickListener(__ -> startDiscoverActivity());
        binding.logoutButton.setOnClickListener(__ -> signOut());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return binding.getRoot();
    }

    private void startDiscoverActivity() {
        Intent intent = new Intent(getActivity(), DiscoverActivity.class);
        startActivity(intent);
    }

    private void signOut() {
        sharedPreferencesStore.setTokens(null, null, -1);
        sharedPreferencesStore.setHouseholdAndGroup(null, null);

        Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
    }
}