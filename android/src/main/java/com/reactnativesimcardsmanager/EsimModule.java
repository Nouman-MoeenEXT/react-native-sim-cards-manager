package com.reactnativesimcardsmanager;

import static android.content.Context.EUICC_SERVICE;

import android.os.Build;
import android.telephony.euicc.EuiccManager;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ReactContext;
import android.util.Log;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class EsimModule {
    private static final String TAG = "EsimModule";

    @RequiresApi(api = Build.VERSION_CODES.P)
    private EuiccManager mgr;
    private final ReactContext mReactContext;

    EsimModule(ReactContext reactContext) {
        mReactContext = reactContext;
        Log.d(TAG, "EsimModule initialized"); // Log when the EsimModule is initialized
        FirebaseCrashlytics.getInstance().log("EsimModule initialized");
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public EuiccManager getMgr() {
        Log.d(TAG, "getMgr() called"); // Log when getMgr() is called
        FirebaseCrashlytics.getInstance().log("getMgr() called");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.e(TAG, "EuiccManager not supported on Android versions before P (API 28)"); // Log for unsupported versions
            FirebaseCrashlytics.getInstance().log("EuiccManager not supported on Android versions before P (API 28)");
            return null;
        }

        if (mgr != null) {
            Log.d(TAG, "EuiccManager already initialized"); // Log if it's already initialized
            FirebaseCrashlytics.getInstance().log("EuiccManager already initialized");
            return mgr;
        }

        mgr = (EuiccManager) mReactContext.getSystemService(EUICC_SERVICE);
        
        if (mgr == null) {
            Log.e(TAG, "EuiccManager is null, unable to access eSIM functionality"); // Log if the EuiccManager is null
            FirebaseCrashlytics.getInstance().log("EuiccManager is null, unable to access eSIM functionality");
        } else {
            Log.d(TAG, "EuiccManager initialized successfully"); // Log successful initialization
            FirebaseCrashlytics.getInstance().log("EuiccManager initialized successfully");
        }

        return mgr;
    }
}
