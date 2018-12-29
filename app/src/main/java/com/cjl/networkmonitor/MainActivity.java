package com.cjl.networkmonitor;

import android.app.Application;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.cjl.networkmonitor.monitor.NetworkMonitor;
import com.cjl.networkmonitor.monitor.OnNetworkStateListener;
import com.cjl.networkmonitor.monitor.PhoneSignalHandler;
import com.cjl.networkmonitor.utils.PhoneUtil;

public class MainActivity extends AppCompatActivity {

    private NetworkMonitor networkMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registPhoneSignalReceiver();
    }

    private void registPhoneSignalReceiver() {
        OnNetworkStateListener onNetworkStateListener
                = new OnNetworkStateListener(){

            @Override
            public void onMobileSignalStrengthsChanged(PhoneSignalHandler.SimCard simCard, int level) {
                if (level<2) {
                    Toast.makeText(MainActivity.this, "当前移动网络情况不佳", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onWifiSignalStrengthsChanged(int level) {
                //信号弱
                if (level<2) {
                    if (PhoneUtil.hasSimCard(MainActivity.this)) {
                        Toast.makeText(MainActivity.this, "当前wifi网络情况不佳，请切换到移动数据网络", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "当前wifi网络情况不佳", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onMobileDataConnectivity(boolean isSim1Exist, boolean isSim2Exist) {
                Toast.makeText(MainActivity.this, "手机数据网络连接", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onWifiConnectivity() {
                Toast.makeText(MainActivity.this, "wifi网络连接", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDisconnection() {
                Toast.makeText(MainActivity.this, "网络无连接", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onServerAvailable(int networkType,boolean available) {
                if (!available) {
                    Toast.makeText(MainActivity.this, "当前服务器无法正常访问", Toast.LENGTH_LONG).show();
                }
            }
        };
        if (networkMonitor==null) {
            networkMonitor = new NetworkMonitor();
        }
        networkMonitor.openServerCheck("192.168.90.165");
        networkMonitor.registerOnNetworkStateListener(onNetworkStateListener);
    }

    private void unregisterPhoneSignalReceiver() {
        if (networkMonitor!=null) {
            networkMonitor.unRegisterOnNetworkStateListener();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterPhoneSignalReceiver();
        super.onDestroy();
    }
}
