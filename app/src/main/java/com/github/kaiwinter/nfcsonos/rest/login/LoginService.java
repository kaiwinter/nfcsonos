package com.github.kaiwinter.nfcsonos.rest.login;

import com.github.kaiwinter.nfcsonos.rest.login.model.AccessToken;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface LoginService {

    /**
     * Gets an access token after with the acquired authorization code.
     *
     * @param authorization authorization Header: "Basic Base64Of([CLIENT_ID]:[CLIENT_SECRET])"
     * @param grantType     fixed string "authorization_code"
     * @param code          the previously acquired authorization code
     * @param redirectUri   redirect URI which is captured by the app
     * @return the access token call
     * @see <a href="https://developer.sonos.com/reference/authorization-api/create-token/">https://developer.sonos.com/reference/authorization-api/create-token/</a>
     */
    @FormUrlEncoded
    @POST("/login/v3/oauth/access")
    Call<AccessToken> getAccessToken(@Header("Authorization") String authorization,
                                     @Field("grant_type") String grantType,
                                     @Field("code") String code,
                                     @Field("redirect_uri") String redirectUri);

    /**
     * Refresh an access token by using the refresh token.
     *
     * @param authorization authorization Header: "Basic Base64Of([CLIENT_ID]:[CLIENT_SECRET])"
     * @param refreshToken  the previously acquired refresh token
     * @param grantType     fixed string "refresh_token"
     * @return the access token Call
     * @see <a href="https://developer.sonos.com/reference/authorization-api/refresh-token/">https://developer.sonos.com/reference/authorization-api/refresh-token/</a>
     */
    @FormUrlEncoded
    @POST("/login/v3/oauth/access")
    Call<AccessToken> refreshToken(@Header("Authorization") String authorization,
                                   @Field("refresh_token") String refreshToken,
                                   @Field("grant_type") String grantType);
}
