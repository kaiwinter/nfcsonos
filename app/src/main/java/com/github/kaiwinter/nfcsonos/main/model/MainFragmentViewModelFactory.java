package com.github.kaiwinter.nfcsonos.main.model;

import android.app.Application;

import androidx.annotation.NonNull;
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

    @NonNull
    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.equals(MainFragmentViewModel.class)) {
            SharedPreferencesStore sharedPreferencesStore = new SharedPreferencesStore(application);
            AccessTokenManager accessTokenManager = new AccessTokenManager(application);
            ServiceFactory serviceFactory = new ServiceFactory(ServiceFactory.API_ENDPOINT, accessTokenManager);
            FavoriteCache favoriteCache = new FavoriteCache(application);
            return (T) new MainFragmentViewModel(sharedPreferencesStore, favoriteCache, serviceFactory);
        }
        throw new RuntimeException("Cannot create an instance of " + modelClass);
    }
}
