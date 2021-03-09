package com.github.kaiwinter.nfcsonos.rest.discover;

import com.github.kaiwinter.nfcsonos.rest.discover.model.Groups;
import com.github.kaiwinter.nfcsonos.rest.discover.model.Households;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * After the user has authorized, you should get the list of households available in the account and get a list of groups that you can control.
 */
public interface DiscoverService {

    /**
     * Loads households which are associated with the logged in account.
     *
     * @return households of the current account
     */
    @GET("/control/api/v1/households")
    Call<Households> getHouseholds();

    /**
     * Loads groups of the household of the logged in account.
     *
     * @param household the household of the wanted groups
     * @return the groups of the household
     */
    @GET("/control/api/v1/households/{household}/groups")
    Call<Groups> getGroups(@Path("household") String household);
}
