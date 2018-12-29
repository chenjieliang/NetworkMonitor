package com.cjl.networkmonitor;

import android.app.Application;
import android.content.Context;

/**
 * @author chenjieliang
 */
public class App extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this.getApplicationContext();
    }

    public static Context getContext(){
        return mContext;
    }
}
