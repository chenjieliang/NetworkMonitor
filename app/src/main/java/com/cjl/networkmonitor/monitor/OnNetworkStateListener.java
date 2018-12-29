package com.cjl.networkmonitor.monitor;

/**
 * @author chenjieliang
 */
public interface OnNetworkStateListener extends OnSignalStrengthsListener {

    /**
     * 当前连接网络是否可访问后台服务器
     * @param networkType
     * @param available
     */
    public void onServerAvailable(int networkType, boolean available);
}
