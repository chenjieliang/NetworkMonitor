package com.cjl.networkmonitor.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author chenjieliang
 */
public class PhoneUtil {


    /* 判断是否包含SIM卡
    *
    * @return 状态
    */
    public static boolean hasSimCard(Context context) {
        TelephonyManager telMgr = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        boolean result = true;
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                result = false; // 没有SIM卡
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                result = false;
                break;
        }
        return result;
    }

    public static boolean hasSimCard(int cardIndex,Context context) {
        boolean isSimCardExist = false;
        try {
            TelephonyManager telMgr = (TelephonyManager)
                    context.getSystemService(Context.TELEPHONY_SERVICE);
            Method method = TelephonyManager.class.getMethod("getSimState", new Class[]{int.class});
            int simState = (Integer) method.invoke(telMgr, new Object[]{Integer.valueOf(cardIndex)});
            if (TelephonyManager.SIM_STATE_READY == simState) {
                isSimCardExist = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isSimCardExist;
    }

/*    private static int[] getSubId(int slotId) {
        Method declaredMethod;
        int[] subArr = null;
        try {
            declaredMethod = Class.forName("android.telephony.SubscriptionManager").getDeclaredMethod("getSubId", new Class[]{Integer.TYPE});
            declaredMethod.setAccessible(true);
            subArr = (int[]) declaredMethod.invoke(mSubscriptionManager, slotId);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            declaredMethod = null;
        } catch (IllegalArgumentException e2) {
            e2.printStackTrace();
            declaredMethod = null;
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
            declaredMethod = null;
        } catch (ClassCastException e4) {
            e4.printStackTrace();
            declaredMethod = null;
        } catch (IllegalAccessException e5) {
            e5.printStackTrace();
            declaredMethod = null;
        } catch (InvocationTargetException e6) {
            e6.printStackTrace();
            declaredMethod = null;
        }
        if (declaredMethod == null) {
            subArr = null;
        }
        MLog.d("getSubId = " + subArr[0]);
        return subArr;
    }*/


    public static void setMobileDataStatus(Context context,Boolean enableMobileData){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        // 允许流量，阻止wifi
        wifiManager.setWifiEnabled(!enableMobileData);//false表示断开WiFi
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connected = conn.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
        if (!connected) {
            ConnectivityManager gprsCM = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            Class conmanClass;
            try {
                conmanClass = Class.forName(gprsCM.getClass().getName());
                final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(gprsCM);
                final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
                final Method setMobileDataEnabledMethod = iConnectivityManagerClass
                        .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);//true表示连接网络
                setMobileDataEnabledMethod.invoke(iConnectivityManager, enableMobileData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void setMobileDataStatus2(Context context,Boolean enableMobileData){
        TelephonyManager mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        ConnectivityManager mConnectivityManager =(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Object object = Build.VERSION.SDK_INT >= 21 ?       mTelephonyManager : mConnectivityManager;
        String methodName = Build.VERSION.SDK_INT >= 21 ? "setDataEnabled" : "setMobileDataEnabled";
        Method setMobileDataEnable;
        try {
            setMobileDataEnable = object.getClass().getMethod(methodName, boolean.class);
            setMobileDataEnable.setAccessible(true);//true表示连接网络
            setMobileDataEnable.invoke(object, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
