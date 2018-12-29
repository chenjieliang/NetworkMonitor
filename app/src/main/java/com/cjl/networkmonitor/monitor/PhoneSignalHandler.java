package com.cjl.networkmonitor.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;


import com.cjl.networkmonitor.App;
import com.cjl.networkmonitor.utils.NetWorkUtil;
import com.cjl.networkmonitor.utils.PermissionUtil;
import com.cjl.networkmonitor.utils.PhoneUtil;
import com.cjl.networkmonitor.utils.ReflectUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * 1.wifi信号强度wifiinfo.getRssi()
 *   值是一个0到-100的区间值，是一个int型数据，其中0到-55表示信号最好，-55到-70表示信号偏差，小于-70表示最差，有可能连接不上或者掉线
 * @author chenjieliang
 */
public class PhoneSignalHandler {
    public static final String TAG = PhoneSignalHandler.class.getSimpleName();
    public static final int INDEX_SIM1 = 0;
    public static final int INDEX_SIM2 = 1;
    private static PhoneSignalHandler mInstance = null;
    public static byte[] mLock = new byte[0];
    private final Context mContext;
    private final TelephonyManager mTelephonyManager;
    private final SubscriptionManager mSubscriptionManager;
    private final NetworkStateReceive mNetworkStateReceiver;

    private SimSignalInfo mSim1SignalInfo = new SimSignalInfo();
    private SimSignalInfo mSim2SignalInfo = new SimSignalInfo();

    private int mWifiSigalLevel;

    private ArrayList<OnSignalStrengthsListener> mOnSignalStrengthsChangedListeners = null;
    private SimSignalStrengthsListener mSim1SignalStrengthsListener;
    private SimSignalStrengthsListener mSim2SignalStrengthsListener;

    private PhoneSignalHandler() {
        mContext = App.getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mSubscriptionManager = (SubscriptionManager) mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        } else {
            mSubscriptionManager = null;
        }
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        initSimSignalListeners();

        mNetworkStateReceiver = new NetworkStateReceive();
        IntentFilter intentFilter = new IntentFilter();
        //intentFilter.addAction(SimStateReceive.ACTION_SIM_STATE_CHANGED);
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mNetworkStateReceiver, intentFilter);
    }

    public static PhoneSignalHandler getInstance() {
        if (null == mInstance) {
            synchronized (mLock) {
                if (null == mInstance) {
                    mInstance = new PhoneSignalHandler();
                }
            }
        }
        return mInstance;
    }

    public void destroyInstance() {
        if (null != mInstance) {
            synchronized (mLock) {
                if (null != mInstance) {
                    if (null != mOnSignalStrengthsChangedListeners) {
                        mOnSignalStrengthsChangedListeners.clear();
                        mOnSignalStrengthsChangedListeners = null;
                    }
                    mContext.unregisterReceiver(mNetworkStateReceiver);
                    mInstance = null;
                }
            }
        }
    }


    private void initSimSignalListeners() {
        if (!PermissionUtil.checkPermission(mContext, PermissionUtil.PERMISSION_READ_PHONE_STATE)){
            return;
        }
        // >=api 22 支持多sim card
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            listenSimSignalStrengths(SimCard.SIM_CARD_1);
            listenSimSignalStrengths(SimCard.SIM_CARD_2);
        } else {
            listenSimSignalStrengths(SimCard.SIM_CARD_1);
        }
    }


    private void listenSimSignalStrengths(SimCard simCard) {
        if (simCard == SimCard.SIM_CARD_1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionInfo sub0 = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(INDEX_SIM1);
                if (sub0 != null && null == mSim1SignalStrengthsListener) {
                    mSim1SignalStrengthsListener = new SimSignalStrengthsListener(sub0.getSubscriptionId(), INDEX_SIM1);
                }
            } else {
                mSim1SignalStrengthsListener = new SimSignalStrengthsListener(0, INDEX_SIM1);
            }
            if (mSim1SignalStrengthsListener!=null){
                mTelephonyManager.listen(mSim1SignalStrengthsListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            }

        } else if (simCard == SimCard.SIM_CARD_2) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionInfo sub1 = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(INDEX_SIM2);
                if (sub1 != null && null == mSim2SignalStrengthsListener) {
                    mSim2SignalStrengthsListener = new SimSignalStrengthsListener(sub1.getSubscriptionId(), INDEX_SIM2);
                }
                if (mSim2SignalStrengthsListener!=null){
                    mTelephonyManager.listen(mSim2SignalStrengthsListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                }
            }
        }
    }

    private void unListenSimSignalStrengths(SimCard simCard) {
        if (simCard == SimCard.SIM_CARD_1) {
            mSim1SignalInfo.mIsActive = false;
            mSim1SignalInfo.mLevel = 0;
            if (null != mSim1SignalStrengthsListener) {
                mTelephonyManager.listen(mSim1SignalStrengthsListener, PhoneStateListener.LISTEN_NONE);
            }
        } else if (simCard == SimCard.SIM_CARD_2) {
            mSim2SignalInfo.mIsActive = false;
            mSim2SignalInfo.mLevel = 0;
            if (null != mSim2SignalStrengthsListener) {
                mTelephonyManager.listen(mSim2SignalStrengthsListener, PhoneStateListener.LISTEN_NONE);
            }
        }

    }

    private void unListenWifiSignalStrengths(){
        mWifiSigalLevel = 0;
    }

    /**
     * 添加监听网络信号强度
     *
     * @param listener
     */
    public void registerOnSignalStrengthsChangedListener(OnSignalStrengthsListener listener) {
        if (null == mOnSignalStrengthsChangedListeners) {
            mOnSignalStrengthsChangedListeners = new ArrayList<>();
        }

        if (mOnSignalStrengthsChangedListeners.contains(listener)) {
            return;
        }

        if (null != listener) {
            mOnSignalStrengthsChangedListeners.add(listener);
        }
    }

    public void unregisterOnSignalStrengthsChangedListener(OnSignalStrengthsListener listener) {
        if (null == mOnSignalStrengthsChangedListeners) {
            return;
        }

        if (null == listener) {
            return;
        }

        if (mOnSignalStrengthsChangedListeners.contains(listener)) {
            mOnSignalStrengthsChangedListeners.remove(listener);
        }
    }

    public void notifyStateChange(boolean isSim1Exist, boolean isSim2Exist,int connectionType) {
        if (null != mOnSignalStrengthsChangedListeners && !mOnSignalStrengthsChangedListeners.isEmpty()) {
            for (int i = 0; i < mOnSignalStrengthsChangedListeners.size(); i++) {
                OnSignalStrengthsListener listener = mOnSignalStrengthsChangedListeners.get(i);
                if (null != listener) {
                    if (connectionType == ConnectivityManager.TYPE_WIFI) {
                        Log.i(TAG, "连上（wifi）");
                        listener.onWifiConnectivity();
                    } else if (connectionType == ConnectivityManager.TYPE_MOBILE) {
                        Log.i(TAG, "连上（移动网络）");
                        listener.onMobileDataConnectivity(isSim1Exist,isSim2Exist);
                    } else if (connectionType == -1) {
                        listener.onDisconnection();
                    }
                }
            }
        }
    }

    public void notifyMobileDataSignalStrengthsChanged(SimCard simCard, int level) {
        if (null != mOnSignalStrengthsChangedListeners && !mOnSignalStrengthsChangedListeners.isEmpty()) {
            for (int i = 0; i < mOnSignalStrengthsChangedListeners.size(); i++) {
                OnSignalStrengthsListener listener = mOnSignalStrengthsChangedListeners.get(i);
                if (null != listener) {
                    listener.onMobileSignalStrengthsChanged(simCard, level);
                }
            }
        }
    }

    public void notifyWifiSignalStrengthsChanged(int level){
        if (null != mOnSignalStrengthsChangedListeners && !mOnSignalStrengthsChangedListeners.isEmpty()) {
            for (int i = 0; i < mOnSignalStrengthsChangedListeners.size(); i++) {
                OnSignalStrengthsListener listener = mOnSignalStrengthsChangedListeners.get(i);
                if (null != listener) {
                    listener.onWifiSignalStrengthsChanged(level);
                }
            }
        }
    }

    /**
     * 获取sim信号 状态信息
     *
     * @return int[]  index: 0:sim1 1:sim2
     */
    public SimSignalInfo[] getSimSignalInfos() {
        return new SimSignalInfo[]{mSim1SignalInfo, mSim2SignalInfo};
    }

    private int getMobileSignalStrengthsLevel(SignalStrength signalStrength) {
        int level = -1;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                level = signalStrength.getLevel();
            } else {
                Method levelMethod = SignalStrength.class.getDeclaredMethod("getLevel");
                level = (int) levelMethod.invoke(signalStrength);
            }
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
        return level;
    }

    private class SimSignalStrengthsListener extends PhoneStateListener {

        private int mSlot = 0;
        public SimSignalStrengthsListener(int subId , int slot) {
            super();
            mSlot = slot;
            //设置当前监听的sim卡
            ReflectUtil.setFieldValue(this, "mSubId", subId);
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            int level = getMobileSignalStrengthsLevel(signalStrength);
            if (mSim1SignalInfo.mLevel == level) {
                return;
            }
            mSim1SignalInfo.mLevel = level;
            if (mSlot == 0) {
                PhoneSignalHandler.this.notifyMobileDataSignalStrengthsChanged(SimCard.SIM_CARD_1, mSim1SignalInfo.mLevel);
                Log.d(TAG, "sim 1 signal strengths level = " + mSim1SignalInfo.mLevel);
            } else {
                PhoneSignalHandler.this.notifyMobileDataSignalStrengthsChanged(SimCard.SIM_CARD_2, mSim2SignalInfo.mLevel);
                Log.d(TAG, "sim 2 signal strengths level = " + mSim2SignalInfo.mLevel);
            }
        }
    }

    public enum SimCard {
        SIM_CARD_1, SIM_CARD_2
    }


    class NetworkStateReceive extends BroadcastReceiver {
        private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_SIM_STATE_CHANGED)) {
                Log.i(TAG, "sim state changed");

            // 监听网络连接，包括wifi和移动数据的打开和关闭,以及连接上可用的连接都会接到监听
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                int type = NetWorkUtil.getConnectedType(context);
                Log.i(TAG, "getConnectedType : " + type);
                if (type != ConnectivityManager.TYPE_WIFI) {
                    unListenWifiSignalStrengths();
                }

                mSim1SignalInfo.mLevel = 0;
                mSim2SignalInfo.mLevel = 0;
                mSim1SignalInfo.mIsActive = false;
                mSim2SignalInfo.mIsActive = false;
                if (PermissionUtil.checkPermission(mContext,PermissionUtil.PERMISSION_READ_PHONE_STATE)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        mSim1SignalInfo.mIsActive = PhoneUtil.hasSimCard(INDEX_SIM1, mContext)
                                && null != mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(INDEX_SIM1);
                        mSim2SignalInfo.mIsActive = PhoneUtil.hasSimCard(INDEX_SIM2, mContext)
                                && null != mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(INDEX_SIM2);
                    } else {
                        mSim1SignalInfo.mIsActive = PhoneUtil.hasSimCard(mContext);
                        mSim2SignalInfo.mIsActive = false;
                    }

                    if (mSim1SignalInfo.mIsActive && type == ConnectivityManager.TYPE_MOBILE) {
                        listenSimSignalStrengths(SimCard.SIM_CARD_1);
                    } else {
                        unListenSimSignalStrengths(SimCard.SIM_CARD_1);
                    }
                    if (mSim2SignalInfo.mIsActive && type == ConnectivityManager.TYPE_MOBILE) {
                        listenSimSignalStrengths(SimCard.SIM_CARD_2);
                    } else {
                        unListenSimSignalStrengths(SimCard.SIM_CARD_2);
                    }
                }
                notifyStateChange(mSim1SignalInfo.mIsActive, mSim2SignalInfo.mIsActive,type);

            } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                Log.i(TAG, "info.isConnected(): " + info.isConnected());
                if (info.isConnected()) {
                    int strength = getWifiSignalStrength(context);
                    Log.i(TAG, "getStrength strength : " + strength);
                    if (strength == mWifiSigalLevel) {
                        return;
                    }
                    mWifiSigalLevel = strength;
                    notifyWifiSignalStrengthsChanged(strength);
                }
            }
        }
    }

    public int getWifiSignalStrength(Context context) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info.getBSSID() != null) {
            Log.i(TAG, "getStrength getRssi : " + info.getRssi());
            int strength = WifiManager.calculateSignalLevel(info.getRssi(), 4);
            // 链接速度
            // int speed = info.getLinkSpeed();
            // // 链接速度单位
            // String units = WifiInfo.LINK_SPEED_UNITS;
            // // Wifi源名称
            // String ssid = info.getSSID();
            return strength;

        }
        return 0;
    }

    public class SimSignalInfo {
        /**
         * 信号强度 0 - 5
         */
        public int mLevel;

        /**
         * sim卡是否有效
         */
        public boolean mIsActive;
    }

}
