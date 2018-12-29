package com.cjl.networkmonitor.monitor;

/**
 * @author chenjieliang
 */
public interface OnSignalStrengthsListener {

    /**
     * 手机信号强度变化回调
     * @param simCard
     * @param level
     */
    void onMobileSignalStrengthsChanged(PhoneSignalHandler.SimCard simCard, int level);

    /**
     * wifi信号强度变化回调
     * @param level
     */
    void onWifiSignalStrengthsChanged(int level);

    /**
     * 手机移动网络数据连接时回调
     * @param isSim1Exist
     * @param isSim2Exist
     */
    void onMobileDataConnectivity(boolean isSim1Exist, boolean isSim2Exist);

    /**
     * wifi网络数据连接时回调
     */
    void onWifiConnectivity();

    /**
     * 无网络连接或网络断开
     */
    void onDisconnection();
}
