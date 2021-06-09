package com.github.kaiwinter.nfcsonos;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.github.kaiwinter.nfcsonos.model.FavoriteCache;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;

public class MainViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public MainViewModelFactory(Application application) {
        this.application = application;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.equals(MainViewModel.class)) {
            SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStore(application);
            AccessTokenManager accessTokenManager = new AccessTokenManager(application);
            FavoriteCache favoriteCache = new FavoriteCache(application);
            return (T) new MainViewModel(sharedPreferencesStore, accessTokenManager, favoriteCache);
        }
        return null;
    }
}
