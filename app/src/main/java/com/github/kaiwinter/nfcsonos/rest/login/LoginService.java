package com.github.kaiwinter.nfcsonos.rest.login;

import com.github.kaiwinter.nfcsonos.rest.login.model.AccessToken;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface LoginService {
    @FormUrlEncoded
    @POST("/login/v3/oauth/access")
    Call<AccessToken> getAccessToken(@Header("Authorization") String authorization,
                                     @Field("grant_type") String grantType,
                                     @Field("code") String code,
                                     @Field("redirect_uri") String redirectUri);

    @FormUrlEncoded
    @POST("/oauth2/token")
    Call<AccessToken> refreshToken(@Field("client_id") String clientId,
                                   @Field("client_secret") String clientSecret,
                                   @Field("grant_type") String grantType,
                                   @Field("refresh_token") String refreshToken);
}
