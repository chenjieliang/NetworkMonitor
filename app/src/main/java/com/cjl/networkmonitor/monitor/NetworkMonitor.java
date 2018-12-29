package com.cjl.networkmonitor.monitor;

import android.support.annotation.NonNull;
import android.util.Log;

import com.cjl.networkmonitor.utils.NetWorkUtil;


/**
 * @author chenjieliang
 */
public class NetworkMonitor {

    private static final String TAG = NetworkMonitor.class.getSimpleName();
    public static final int TYPE_MOBILE = 0;
    public static final int TYPE_WIFI = 1;
    private PhoneSignalHandler phoneSignalHandler;
    private OnNetworkStateListener listener;
    private boolean isCheckServer = false;
    private String serverIp;

    public NetworkMonitor() {
        phoneSignalHandler = PhoneSignalHandler.getInstance();
    }

    public void registerOnNetworkStateListener(OnNetworkStateListener listener){
        this.listener = listener;
        phoneSignalHandler.registerOnSignalStrengthsChangedListener(signalStrengthsChangedListener);
    }

    public void unRegisterOnNetworkStateListener() {
        phoneSignalHandler.unregisterOnSignalStrengthsChangedListener(signalStrengthsChangedListener);
    }

    public void openServerCheck(String serverIp) {
        this.isCheckServer = true;
        this.serverIp = serverIp;
    }

    public void closeSeverCheck(){
        isCheckServer = false;
    }

    private OnSignalStrengthsListener signalStrengthsChangedListener = new OnSignalStrengthsListener(){

        @Override
        public void onMobileSignalStrengthsChanged(PhoneSignalHandler.SimCard simCard, int level) {
            if (listener!=null) {
                    listener.onMobileSignalStrengthsChanged(simCard,level);
            }
        }

        @Override
        public void onWifiSignalStrengthsChanged(int level) {
            if (listener!=null) {
                listener.onWifiSignalStrengthsChanged(level);
            }
        }

        @Override
        public void onMobileDataConnectivity(boolean isSim1Exist, boolean isSim2Exist) {
            if (listener!=null) {
                listener.onMobileDataConnectivity(isSim1Exist,isSim2Exist);
                checkServerAvailable(TYPE_MOBILE);
            }
        }

        @Override
        public void onWifiConnectivity() {
            if (listener!=null) {
                listener.onWifiConnectivity();
                checkServerAvailable(TYPE_WIFI);
            }
        }

        @Override
        public void onDisconnection() {
            if (listener!=null) {
                listener.onDisconnection();
            }
        }

    };

    /*
     * 监测当前网络是否可访问服务器
     */
    private void checkServerAvailable(final int networkType){
        if (!isCheckServer
                && (serverIp == null || serverIp.isEmpty())) {
            return;
        }
        NetWorkUtil.isNetWorkAvailableByPing(serverIp, new Comparable<Boolean>() {
            @Override
            public int compareTo(@NonNull Boolean available) {
                if (available) {
                    // TODO 设备访问Internet正常
                    Log.i(TAG, "checkServerAvailable true");
                    if (listener!=null) {
                        listener.onServerAvailable(networkType,true);
                    }
                } else {
                    // TODO 设备无法访问Internet
                    if (listener!=null) {
                        listener.onServerAvailable(networkType,false);
                    }
                    Log.i(TAG, "checkServerAvailable false");
                }
                return 0;
            }
        });
    }

}
