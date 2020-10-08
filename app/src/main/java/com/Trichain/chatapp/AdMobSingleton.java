package com.Trichain.chatapp;

import android.app.Activity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class AdMobSingleton {
    private static InterstitialAd sInterstitialAd;
    private static AdMobSingleton instance;
    private static boolean sShowOnLoad;
    private static boolean sIsUpdate;
    private static MyAdListener myAdListener;

    public interface MyAdListener {
        void onAdLoaded();

        void onAdFailed();

        void onAdClosed();
    }

    private AdMobSingleton(Activity activity) {
        sInterstitialAd = newInterstitialAd(activity);
    }

    public static synchronized AdMobSingleton getInstance(Activity activity) {
        if (instance == null) {
            instance = new AdMobSingleton(activity);
        }

        return instance;

    }

    public static void updateAd(Activity activity) {
        sIsUpdate = true;
        new AdMobSingleton(activity);
    }

    private InterstitialAd newInterstitialAd(Activity activity) {
        InterstitialAd interstitialAd = new InterstitialAd(activity.getApplicationContext());
        interstitialAd.setAdUnitId(activity.getApplication().getString(R.string.interstitial_ad_unit_id)); // AdMob interstitial ad unit id
        return interstitialAd;
    }

    public void loadInterstitial(final Activity activity) {
        AdRequest adRequest = new AdRequest.Builder().build();
        sInterstitialAd.loadAd(adRequest);
        sInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                myAdListener.onAdLoaded();
                if (sShowOnLoad) {
                    if (!sIsUpdate) {
                        showInterstitial();
                    }
                    sIsUpdate = false;
                }
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                myAdListener.onAdFailed();
            }

            @Override
            public void onAdClosed() {
                myAdListener.onAdClosed();
                updateAd(activity);
            }
        });
    }

    private static void showInterstitial() {
        if (sInterstitialAd != null && sInterstitialAd.isLoaded()) {
            sInterstitialAd.show();
        }
    }

    public void setShowOnLoad(boolean showOnLoad) {
        AdMobSingleton.sShowOnLoad = showOnLoad;
    }

    public void setMyAdListener(MyAdListener adListener) {
        myAdListener = adListener;
    }
}
