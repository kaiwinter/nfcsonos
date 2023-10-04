package com.github.kaiwinter.nfcsonos.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.discover.DiscoverActivity;
import com.github.kaiwinter.nfcsonos.login.LoginActivity;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;

import de.psdev.licensesdialog.LicenseResolver;
import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.License;

/**
 * The settings fragment.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStore(getActivity());

        setPreferencesFromResource(R.xml.preferences, rootKey);
        Preference logout = findPreference("logout");
        logout.setIntent(signOutIntent());

        Preference householdAndGroupSelection = findPreference("householdAndGroupSelection");
        householdAndGroupSelection.setIntent(new Intent(getActivity(), DiscoverActivity.class));
        householdAndGroupSelection.setSummaryProvider(preference ->
                getString(R.string.household_and_group_selection_summary,
                        sharedPreferencesStore.getHouseholdId().substring(0, 20),
                        sharedPreferencesStore.getGroupId().substring(0, 20)));

        Preference thirdParty = findPreference("thirdParty");
        thirdParty.setOnPreferenceClickListener(preference -> {
            startLicensesDialog();
            return true;
        });
    }

    private void startLicensesDialog() {
        LicenseResolver.registerLicense(createLicense("mixkit", R.raw.mixkit_license_summary, ""));
        LicenseResolver.registerLicense(createLicense("freepik", -1, "http://www.freepik.com"));
        LicenseResolver.registerLicense(createLicense("flaticon", -1, "https://www.flaticon.com/authors/flat-icons"));
        new LicensesDialog.Builder(getActivity())
                .setNotices(R.raw.notices)
                .build().show();
    }

    private Intent signOutIntent() {
        Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        return loginIntent;
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