package com.github.kaiwinter.nfcsonos.activity.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.github.kaiwinter.nfcsonos.AboutFragment;
import com.github.kaiwinter.nfcsonos.MainFragment;
import com.github.kaiwinter.nfcsonos.PairFragment;
import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.model.FavoriteCache;
import com.github.kaiwinter.nfcsonos.rest.GroupVolumeService;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private SharedPreferencesStore sharedPreferencesStore;
    private AccessTokenManager accessTokenManager;
    private FavoriteCache favoriteCache;

    Fragment selectedFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferencesStore = new SharedPreferencesStore(getApplicationContext());
        accessTokenManager = new AccessTokenManager(getApplicationContext());
        favoriteCache = new FavoriteCache(getApplicationContext());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // as soon as the application opens the first
        // fragment should be shown to the user
        // in this case it is algorithm fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MainFragment(sharedPreferencesStore, accessTokenManager, favoriteCache)).commit();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            // By using switch we can easily get
            // the selected fragment
            // by using there id.
            switch (item.getItemId()) {
                case R.id.home:
                    if (selectedFragment instanceof MainFragment) {
                        return true;
                    }
                    selectedFragment = new MainFragment(sharedPreferencesStore, accessTokenManager, favoriteCache);
                    break;
                case R.id.pair:
                    if (selectedFragment instanceof PairFragment) {
                        return true;
                    }
                    selectedFragment = new PairFragment();
                    break;
                case R.id.about:
                    if (selectedFragment instanceof AboutFragment) {
                        return true;
                    }
                    selectedFragment = new AboutFragment();
                    break;
            }
            // It will help to replace the
            // one fragment to other.
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            return true;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {
                setVolumeOnSonosGroup(5);
                Toast.makeText(this, getString(R.string.volume_changed, "+5"), Toast.LENGTH_SHORT).show();
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                setVolumeOnSonosGroup(-5);
                Toast.makeText(this, getString(R.string.volume_changed, "-5"), Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setVolumeOnSonosGroup(int volumeDelta) {
        String accessToken = sharedPreferencesStore.getAccessToken();
        String groupId = sharedPreferencesStore.getGroupId();
        GroupVolumeService service = ServiceFactory.createGroupVolumeService(accessToken);
        Call<Void> call = service.setRelativeVolume(groupId, new GroupVolumeService.VolumeDeltaRequest(volumeDelta));
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
//        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
//        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    public void onPause() {
        super.onPause();

//        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
//        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (selectedFragment instanceof MainFragment) {
            ((MainFragment) selectedFragment).handleIntent(intent);
        } else if (selectedFragment instanceof PairFragment) {
            ((PairFragment) selectedFragment).handleIntent(intent);
        }
    }
}