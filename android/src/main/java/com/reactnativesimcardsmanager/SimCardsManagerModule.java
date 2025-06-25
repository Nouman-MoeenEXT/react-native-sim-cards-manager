package com.reactnativesimcardsmanager;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.euicc.EuiccManager;
import com.facebook.react.bridge.*;
import android.app.PendingIntent;
import android.content.Intent;
import android.telephony.euicc.DownloadableSubscription;
import android.content.IntentFilter;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import android.util.Log;

import java.util.List;

@ReactModule(name = SimCardsManagerModule.NAME)
public class SimCardsManagerModule extends ReactContextBaseJavaModule {
  public static final String NAME = "SimCardsManager";
  private String ACTION_DOWNLOAD_SUBSCRIPTION = "download_subscription";
  private ReactContext mReactContext;
  private EsimModule mEsimModule;

  public SimCardsManagerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mReactContext = reactContext;
    mEsimModule = new EsimModule(reactContext);
    Log.d("SimCardsManager", "SimCardsManagerModule initialized"); // Logcat log

  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
  @ReactMethod
  public void getSimCardsNative(Promise promise) {
        Log.d("SimCardsManager", "getSimCardsNative started"); // Logcat log

    WritableArray simCardsList = new WritableNativeArray();

    TelephonyManager telManager = (TelephonyManager) mReactContext.getSystemService(Context.TELEPHONY_SERVICE);
    try {
      SubscriptionManager manager = (SubscriptionManager) mReactContext
          .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                Log.d("SimCardsManager", "Fetching active subscriptions"); // Logcat log

      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
        int activeSubscriptionInfoCount = manager.getActiveSubscriptionInfoCount();
        int activeSubscriptionInfoCountMax = manager.getActiveSubscriptionInfoCountMax();

        List<SubscriptionInfo> subscriptionInfos = manager.getActiveSubscriptionInfoList();
        Log.d("SimCardsManager", "Found " + subscriptionInfos.size() + " active subscriptions"); // Logcat log

        for (SubscriptionInfo subInfo : subscriptionInfos) {
          WritableMap simCard = Arguments.createMap();

          String number = "";
          if(android.os.Build.VERSION.SDK_INT >= 33) {
            number = manager.getPhoneNumber(subInfo.getSubscriptionId());
          } else {
            number = subInfo.getNumber();
          }

          CharSequence carrierName = subInfo.getCarrierName();
          String countryIso = subInfo.getCountryIso();
          int dataRoaming = subInfo.getDataRoaming(); // 1 is enabled; 0 is disabled
          CharSequence displayName = subInfo.getDisplayName();
          String iccId = subInfo.getIccId();
          int mcc = subInfo.getMcc();
          int mnc = subInfo.getMnc();
          int simSlotIndex = subInfo.getSimSlotIndex();
          int subscriptionId = subInfo.getSubscriptionId();
          int networkRoaming = telManager.isNetworkRoaming() ? 1 : 0;

          // Safely handle potential null values
          String safeCarrierName = (carrierName != null) ? carrierName.toString() : "Unknown Carrier";
          String safeDisplayName = (displayName != null) ? displayName.toString() : "Unknown Display Name";

          // Store values in simCard
          simCard.putString("carrierName", safeCarrierName);
          simCard.putString("displayName", safeDisplayName);
          simCard.putString("isoCountryCode", countryIso);
          simCard.putInt("mobileCountryCode", mcc);
          simCard.putInt("mobileNetworkCode", mnc);
          simCard.putInt("isNetworkRoaming", networkRoaming);
          simCard.putInt("isDataRoaming", dataRoaming);
          simCard.putInt("simSlotIndex", simSlotIndex);
          simCard.putString("phoneNumber", number);
          simCard.putString("simSerialNumber", iccId);
          simCard.putInt("subscriptionId", subscriptionId);

          simCardsList.pushMap(simCard);
        }
      } else {
        promise.reject("0", "This functionality is not supported before Android 5.1 (22)");
      }
    } catch (Exception e) {
            Log.e("SimCardsManager", "Error fetching SIM cards", e); // Logcat error log

      promise.reject("1", "Something goes wrong to fetch simcards: " + e.getLocalizedMessage());
    }
    promise.resolve(simCardsList);
  }

  @RequiresApi(api = Build.VERSION_CODES.P)
  @ReactMethod
  public void sendPhoneCall(String phoneNumberString, int simSlotIndex) {
    Uri uri = Uri.parse("tel:" + phoneNumberString.trim());
    TelecomManager telecomManager =(TelecomManager) mReactContext.getSystemService(Context.TELECOM_SERVICE);
    List<PhoneAccountHandle> list = telecomManager.getCallCapablePhoneAccounts();

    PhoneAccountHandle accountHandle = null;
    if (list != null) {
      accountHandle = list.get(Math.min(simSlotIndex, list.size()));
    }

    if (accountHandle != null) {
      Bundle extras = new Bundle();
      extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,accountHandle);
      telecomManager.placeCall(uri, extras);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.P)
  @ReactMethod
  public void isEsimSupported(Promise promise) {
        Log.d("SimCardsManager", "Checking if eSIM is supported"); // Logcat log

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && mEsimModule.getMgr() != null) {
            Log.d("SimCardsManager", "eSIM support: "); // Logcat log

      promise.resolve(mEsimModule.getMgr().isEnabled());
    } else {
            Log.e("SimCardsManager", "eSIM is not supported on this device"); // Logcat error log

      promise.resolve(false);
    }
    return;
  }

  @RequiresApi(api = Build.VERSION_CODES.P)
  private void handleResolvableError(Promise promise, Intent intent) {
        Log.d("SimCardsManager", "Resolving eSIM error"); // Logcat log

    try {
      // Resolvable error, attempt to resolve it by a user action
      // FIXME: review logic of resolve functions
      int resolutionRequestCode = 3;
      PendingIntent callbackIntent = PendingIntent.getBroadcast(
        mReactContext,
        resolutionRequestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
      );

      if (callbackIntent != null) {
        mEsimModule.getMgr().startResolutionActivity(mReactContext.getCurrentActivity(), resolutionRequestCode, intent, callbackIntent);
        Log.d("SimCardsManager", "Started resolution activity"); // Logcat log
      } else {
        Log.e("SimCardsManager", "No resolution intent available"); // Logcat error log
        promise.reject("NO_RESOLUTION_INTENT", "No resolution intent available.");
      }
    } catch (Exception e) {
            Log.e("SimCardsManager", "Error resolving eSIM error", e); // Logcat error log

      promise.reject("3", "EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR - Can't setup eSim due to Activity error "
          + e.getLocalizedMessage());
    }
  }

  private boolean checkCarrierPrivileges() {
    TelephonyManager telManager = (TelephonyManager) mReactContext.getSystemService(Context.TELEPHONY_SERVICE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
      return telManager.hasCarrierPrivileges();
    } else {
      return false;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.P)
  @ReactMethod
  public void setupEsim(ReadableMap config, Promise promise) {
        Log.d("Starting eSim Installation"); // Logcat log

    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
      promise.reject("0", "EuiccManager is not available or before Android 9 (API 28)");
      return;
    }

    if (mEsimModule.getMgr() != null && !mEsimModule.getMgr().isEnabled()) {
      promise.reject("1", "The device doesn't support a cellular plan (EuiccManager is not available)");
      return;
    }

//    if (!checkCarrierPrivileges()) {
//      promise.reject("1", "No carrier privileges detected");
//      return;
//    }

    BroadcastReceiver receiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {
        boolean rejected = false;
        String code = "";
        String error = "";
       Log.d("SimCardsManager", "In On receive method"); 
        if (!ACTION_DOWNLOAD_SUBSCRIPTION.equals(intent.getAction())) {
          rejected = true;
          code = "3";
          error = "Can't setup eSim due to wrong Intent:" + intent.getAction() + " instead of "
            + ACTION_DOWNLOAD_SUBSCRIPTION;
        }
        int resultCode = getResultCode();
      Log.d("eSimManager", "Starting eSim Installation: ");
        if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR && mEsimModule.getMgr() != null) {
          handleResolvableError(promise, intent);
        } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK) {
          promise.resolve(true);
        } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR) {
          // Embedded Subscription Error
          rejected = true;
          code = "2";
          error = "EMBEDDED_SUBSCRIPTION_RESULT_ERROR - Can't add an Esim subscription";
        } else {
          // Unknown Error
          rejected = true;
          code = "3";
          error = "Can't add an Esim subscription due to unknown error, resultCode is:" + String.valueOf(resultCode);
        }
        // Unregister receiver
        if (rejected) {
                    Log.e("SimCardsManager", error); // Logcat error log
          promise.reject(code, error);
          mReactContext.unregisterReceiver(this);
        }
      }
    };

   // Changes for registering receiver for Android 14
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      mReactContext.registerReceiver(receiver, new IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION), Context.RECEIVER_NOT_EXPORTED);
    } else {
      mReactContext.registerReceiver(
              receiver,
              new IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION),
              null,
              null);
    }

    DownloadableSubscription sub = DownloadableSubscription.forActivationCode(
        /* Passed from react side */
        config.getString("confirmationCode"));

    Intent intent = new Intent(ACTION_DOWNLOAD_SUBSCRIPTION).setPackage(mReactContext.getPackageName());
    PendingIntent callbackIntent = PendingIntent.getBroadcast(
        mReactContext,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
    );

    mEsimModule.getMgr().downloadSubscription(sub, true, callbackIntent);
  }
}
