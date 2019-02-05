package com.criticalblue.demo;

import android.util.Log;

import com.criticalblue.attestationlibrary.ApproovAttestation;
import com.criticalblue.attestationlibrary.TokenInterface;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor to add an Approov
 * token to API requests.
 *
 * Created by barryo on 21/08/17.
 */

final class ApproovTokenInterceptor implements Interceptor {

    final static String TAG = "APPROOV_INTERCEPTOR";

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request originalRequest = chain.request();

        // Fetch an Approov Token using the SDK
        TokenInterface.ApproovResults approovResults = ApproovAttestation.shared()
                .fetchApproovTokenAndWait(originalRequest.url().host());

        // Always check the return status of the fetch call.
        String token;
        if (approovResults.getResult() == ApproovAttestation.AttestationResult.SUCCESS) {
            Log.i(TAG, "Received a token from the Approov SDK.");
            token = approovResults.getToken();
            Log.i(TAG, token);

        } else {
            // A fail here means that the SDK could not reach the Approov servers
            // before timing out. Set the token field to a known value to communicate
            // this state (rather than leaving empty or excluding from the header)
            Log.w(TAG, "Approov SDK token fetch failed");
            token = "NOTOKEN";
        }

        Request approovTokenRequest = originalRequest.newBuilder()
                .header("Approov-Token", token)
                .build();

        return chain.proceed(approovTokenRequest);
    }

}
