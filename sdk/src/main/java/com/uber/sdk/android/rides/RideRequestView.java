/*
 * Copyright (c) 2016 Uber Technologies, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.uber.sdk.android.rides;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.uber.sdk.android.rides.auth.AccessToken;
import com.uber.sdk.android.rides.auth.AccessTokenManager;

import java.util.HashMap;
import java.util.Map;

/**
 * The Uber Ride Request View: an embeddable view that provides the end-to-end Uber experience.
 * The primary way to interact with this view after construction is to call the load() function.
 */
public class RideRequestView extends LinearLayout {

    private static final String USER_AGENT_RIDE_VIEW = "rides-android-v0.3.0-ride_request_view";
    @Nullable private AccessToken mAccessToken;
    @NonNull @VisibleForTesting RideParameters mRideParameters = new RideParameters.Builder().build();
    @Nullable private RideRequestViewCallback mRideRequestViewCallback;
    private WebView mWebView;

    public RideRequestView(Context context) {
        this(context, null);
    }

    public RideRequestView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        init(context);
    }

    /**
     * Stops all current loading and brings up a blank page.
     */
    public void cancelLoad() {
        mWebView.stopLoading();
        mWebView.loadUrl("about:blank");
    }

    /**
     * Gets the {@link AccessToken} being used to authorize the {@link RideRequestView}.
     */
    @Nullable
    public AccessToken getAccessToken() {
        return mAccessToken;
    }

    /**
     * Loads the ride widget.
     * Requires that the {@link AccessToken} has been retrieved.
     */
    public void load() {
        if (mAccessToken == null) {
            if (mRideRequestViewCallback != null) {
                mRideRequestViewCallback.onErrorReceived(RideRequestViewError.NO_ACCESS_TOKEN);
            }
            return;
        }

        mWebView.loadUrl(buildUrlFromRideParameters(mRideParameters), RideRequestView.getHeaders(mAccessToken));
    }

    /**
     * Set a custom {@link AccessToken} to use for authenticating into the Ride Widget.
     *
     * @param accessToken the {@link AccessToken} to use for authorization
     */
    public void setAccessToken(@Nullable AccessToken accessToken) {
        mAccessToken = accessToken;
    }

    /**
     * Configure parameters for the Ride Request Control.
     *
     * @param rideParameters the {@link RideParameters} to use for presetting values
     */
    public void setRideParameters(@NonNull RideParameters rideParameters) {
        mRideParameters = rideParameters;
    }

    /**
     * Sets the callback for events occurring in the Ride Request Control such as errors.
     *
     * @param rideRequestViewCallback the {@link RideRequestViewCallback}
     */
    public void setRideRequestViewCallback(@NonNull RideRequestViewCallback rideRequestViewCallback) {
        mRideRequestViewCallback = rideRequestViewCallback;
    }

    /**
     * Builds a URL with necessary query parameters to load in the {@link WebView}.
     *
     * @param rideParameters the {@link RideParameters} to build into the query
     * @return the URL {@link String} to load in the {@link WebView}
     */
    @NonNull
    @VisibleForTesting static String buildUrlFromRideParameters(@NonNull RideParameters rideParameters) {
        final String ENDPOINT = "components";
        final String ENVIRONMENT_KEY = "env";
        final String HTTPS = "https";
        final String PATH = "rides/";
        final String SANDBOX = "sandbox";

        Uri.Builder builder = new Uri.Builder();
        builder.scheme(HTTPS)
                .authority(ENDPOINT + "." + UberSdk.getRegion().domain)
                .appendEncodedPath(PATH);

        if (rideParameters.getUserAgent() == null) {
            rideParameters.setUserAgent(USER_AGENT_RIDE_VIEW);
        }

        RequestDeeplink deeplink = new RequestDeeplink.Builder().setRideParameters(rideParameters).build();
        Uri uri = deeplink.getUri();
        builder.encodedQuery(uri.getEncodedQuery());

        if (UberSdk.isSandboxMode()) {
            builder.appendQueryParameter(ENVIRONMENT_KEY, SANDBOX);
        }

        return builder.build().toString();
    }

    /**
     * Creates a {@link Map} of the headers needed to pass to the {@link WebView}.
     *
     * @param accessToken the {@link AccessToken} to use for the Authorization header.
     * @return a {@link Map} containing headers to pass to {@link WebView}.
     */
    @NonNull
    @VisibleForTesting static Map<String, String> getHeaders(@NonNull AccessToken accessToken) {
        final String AUTH_HEADER = "Authorization";
        final String BEARER = "Bearer";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put(AUTH_HEADER, BEARER + " " + accessToken.getToken());
        return headers;
    }

    /**
     * Initialize the layout, properties, and inner web view.
     */
    private void init(@NonNull Context context) {
        mAccessToken = new AccessTokenManager(context).getAccessToken();

        inflate(getContext(), R.layout.ub__ride_request_view, this);
        mWebView = (WebView) findViewById(R.id.ub__ride_request_webview);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setGeolocationEnabled(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mWebView.setWebChromeClient(new RideRequestWebChromeClient());
        mWebView.setWebViewClient(new RideRequestWebViewClient(new RideRequestWebViewClientCallback() {
            @Override
            public void onErrorParsed(@NonNull RideRequestViewError error) {
                if (mRideRequestViewCallback != null) {
                    mRideRequestViewCallback.onErrorReceived(error);
                }
            }
        }));
    }

    /**
     * Interface for {@link RideRequestWebViewClient} to communicate with this view.
     */
    @VisibleForTesting
    interface RideRequestWebViewClientCallback {

        /**
         * Called when an error is received in the redirect URL of the {@link WebView}.
         *
         * @param error the {@link RideRequestViewError} that occurred.
         */
        void onErrorParsed(@NonNull RideRequestViewError error);
    }

    /**
     * The {@link WebViewClient} that listens for errors in the URL of the {@link WebView}.
     */
    @VisibleForTesting
    static class RideRequestWebViewClient extends WebViewClient {

        private static final String ERROR_KEY = "error";
        private static final String REDIRECT_URL = "uberconnect://oauth";

        @NonNull private RideRequestWebViewClientCallback mRideRequestWebViewClientCallback;

        /**
         * Construct the web view client to listen for and report back errors through a callback.
         *
         * @param callback the {@link com.uber.sdk.android.rides.RideRequestView.RideRequestWebViewClientCallback}
         */
        @VisibleForTesting
        RideRequestWebViewClient(@NonNull RideRequestWebViewClientCallback callback) {
            mRideRequestWebViewClientCallback = callback;
        }

        @Override
        public void onReceivedError(
                WebView view, WebResourceRequest request, WebResourceError error) {
            mRideRequestWebViewClientCallback.onErrorParsed(RideRequestViewError.CONNECTIVITY_ISSUE);
        }

        @Override
        public void onReceivedHttpError(
                WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            mRideRequestWebViewClientCallback.onErrorParsed(RideRequestViewError.CONNECTIVITY_ISSUE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.toLowerCase().contains(REDIRECT_URL)) {
                Uri uri = Uri.parse(url);

                Uri fragmentUri = new Uri.Builder().encodedQuery(uri.getFragment()).build();
                String errorValue = fragmentUri.getQueryParameter(ERROR_KEY);
                RideRequestViewError error = RideRequestViewError.UNKNOWN;
                if (errorValue != null) {
                    try {
                        error = RideRequestViewError.valueOf(errorValue.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        error = RideRequestViewError.UNKNOWN;
                    }
                }
                mRideRequestWebViewClientCallback.onErrorParsed(error);
                return true;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    /**
     * The {@link WebChromeClient} used for overriding the {@link GeolocationPermissions} prompt.
     */
    private class RideRequestWebChromeClient extends WebChromeClient {

        /**
         * The default implementation does nothing, so permission is never obtained and passed to Javascript.
         * Overriding to always gain permission as {@link RideRequestView} assumes the app has already gained
         * location permissions.
         */
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }
    }
}
