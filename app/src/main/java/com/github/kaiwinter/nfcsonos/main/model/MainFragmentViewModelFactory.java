package com.github.kaiwinter.nfcsonos.main.model;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;

public class MainFragmentViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public MainFragmentViewModelFactory(Application application) {
        this.application = application;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.equals(MainFragmentViewModel.class)) {
            SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStore(application);
            AccessTokenManager accessTokenManager = new AccessTokenManager(application);
            ServiceFactory serviceFactory = new ServiceFactory(ServiceFactory.API_ENDPOINT);
            FavoriteCache favoriteCache = new FavoriteCache(application, serviceFactory);
            return (T) new MainFragmentViewModel(sharedPreferencesStore, accessTokenManager, favoriteCache, serviceFactory);
        }
        return null;
    }
}
