package com.github.kaiwinter.nfcsonos.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.databinding.FragmentSettingsBinding;
import com.github.kaiwinter.nfcsonos.discover.DiscoverActivity;
import com.github.kaiwinter.nfcsonos.login.LoginActivity;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;

import de.psdev.licensesdialog.LicenseResolver;
import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.License;

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
        binding.thirdPartyNotices.setOnClickListener(__ -> startLicensesDialog());
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

    private void startLicensesDialog() {
        LicenseResolver.registerLicense(createLicense("mixkit", R.raw.mixkit_license_summary, ""));
        LicenseResolver.registerLicense(createLicense("freepik", -1, "http://www.freepik.com"));
        LicenseResolver.registerLicense(createLicense("flaticon", -1, "https://www.flaticon.com/authors/flat-icons"));
        new LicensesDialog.Builder(getActivity())
                .setNotices(R.raw.notices)
                .build().show();
    }

    private void signOut() {
        sharedPreferencesStore.setTokens(null, null, -1);
        sharedPreferencesStore.setHouseholdAndGroup(null, null);

        Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
    }

    public static License createLicense(String name, int summaryFile, String summaryText) {
        return new License() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String readSummaryTextFromResources(Context context) {
                if (summaryFile == -1) {
                    return summaryText;
                }
                return getContent(context, summaryFile);
            }

            @Override
            public String readFullTextFromResources(Context context) {
                return "";
            }

            @Override
            public String getVersion() {
                return "";
            }

            @Override
            public String getUrl() {
                return "";
            }
        };
    }
}