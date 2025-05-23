package com.github.kaiwinter.nfcsonos.main;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.databinding.ActivityMainBinding;
import com.github.kaiwinter.nfcsonos.main.model.MainFragmentViewModel;
import com.github.kaiwinter.nfcsonos.main.model.MainFragmentViewModelFactory;

public class MainActivity extends AppCompatActivity {

    public static final String NFC_SCANNED_INTENT = "NFC_SCANNED_INTENT";

    private MainFragmentViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        viewModel = new ViewModelProvider(this, new MainFragmentViewModelFactory(getApplication())).get(MainFragmentViewModel.class);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {
                viewModel.setVolumeOnSonosGroup(5);
                Toast.makeText(this, getString(R.string.volume_changed, "+5"), Toast.LENGTH_SHORT).show();
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                viewModel.setVolumeOnSonosGroup(-5);
                Toast.makeText(this, getString(R.string.volume_changed, "-5"), Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onResume() {
        super.onResume();

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    // Called if a NFC tag is scanned while the app is on foreground
    // If the app is on background MainFragment.onCreate is called
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        if (fragment == null) {
            return;
        }
        FragmentManager childFragmentManager = fragment.getChildFragmentManager();
        Fragment currentFragment = childFragmentManager.getPrimaryNavigationFragment();

        if (currentFragment instanceof MainFragment mainFragment) {
            mainFragment.handleNfcIntent(intent);
        } else if (currentFragment instanceof PairFragment pairFragment) {
            if (pairFragment.isPairingActive()) {
                pairFragment.handleNfcIntent(intent);
            } else {
                passIntentToMainFragment(intent);
            }
        } else {
            passIntentToMainFragment(intent);
        }
    }

    /**
     * Navigates to the MainFragment and delivers the intent within a Bundle.
     *
     * @param intent the intent to pass to the MainFragment
     */
    private void passIntentToMainFragment(Intent intent) {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        Bundle bundle = new Bundle();
        bundle.putParcelable(NFC_SCANNED_INTENT, intent);
        NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.fragment_main, true).build();
        navController.navigate(R.id.fragment_main, bundle, navOptions);
    }
}