package com.cjl.networkmonitor.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 网络操作工具类
 * 
 * @author chenjieliang
 * 
 */
public class NetWorkUtil {
	/**
	 * 检测网络是否可用
	 */
	public static boolean isNetworkConnected(Activity activity) {
		ConnectivityManager cm = (ConnectivityManager) activity
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnectedOrConnecting();
	}

	/**
	 * 判断WIFI网络是否可用
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isWifiConnected(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mWiFiNetworkInfo = mConnectivityManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (mWiFiNetworkInfo != null) {
				return mWiFiNetworkInfo.isAvailable();
			}
		}
		return false;
	}

	/**
	 * 判断MOBILE网络是否可用
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isMobileConnected(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mMobileNetworkInfo = mConnectivityManager
					.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			if (mMobileNetworkInfo != null) {
				return mMobileNetworkInfo.isAvailable();
			}
		}
		return false;
	}

	/**
	 * 获取当前网络连接的类型信息
	 * 
	 * @param context
	 * @return
	 */
	public static int getConnectedType(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager
					.getActiveNetworkInfo();
			if (mNetworkInfo != null && mNetworkInfo.isAvailable()) {
				return mNetworkInfo.getType();
			}
		}
		return -1;
	}

	/**
	 * 获取本机WIFI ip
	 * 
	 * @return
	 */
	public static String getLocalIpAddress(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		// 获取32位整型IP地址
		int ipAddress = wifiInfo.getIpAddress();

		// 返回整型地址转换成“*.*.*.*”地址
		return String.format("%d.%d.%d.%d", (ipAddress & 0xff),
				(ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
				(ipAddress >> 24 & 0xff));
	}

	/**
	 * 3G网络IP
	 * @return
	 */
	public static String getIpAddress() {
		
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()
							&& inetAddress instanceof Inet4Address) {
						// if (!inetAddress.isLoopbackAddress() && inetAddress
						// instanceof Inet6Address) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 检查互联网地址是否可以访问
	 *
	 * @param address  要检查的域名或IP地址
	 * @param callback 检查结果回调（是否可以ping通地址）{@see java.lang.Comparable<T>}
	 */
	public static void isNetWorkAvailableByPing(final String address, final Comparable<Boolean> callback) {
		final Handler handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (callback != null) {
					callback.compareTo(msg.arg1 == 0);
				}
			}

		};
		new Thread(new Runnable() {

			@Override
			public void run() {
				Runtime runtime = Runtime.getRuntime();
				Message msg = new Message();
				try {

					Process pingProcess = runtime.exec("ping -c 1 -w 1 " + address);
                     // 读取ping的内容，可不加
					InputStreamReader isr = new InputStreamReader(pingProcess.getInputStream());
					BufferedReader buf = new BufferedReader(isr);
					StringBuffer stringBuffer = new StringBuffer();
					String content = "";
					while ((content = buf.readLine()) != null) {
						stringBuffer.append(content);
					}
                    // PING的状态，一般来说 0 表示正常停止，即正常完成，未出现异常情况。1 表示网络已连接，但是无法访问，2 表示网络未连接。
					int status = pingProcess.waitFor();
					if (status == 0) {
						msg.arg1 = 0;
					} else {
						msg.arg1 = -1;
					}
					buf.close();
					isr.close();
				} catch (Exception e) {
					msg.arg1 = -1;
					e.printStackTrace();
				} finally {
					runtime.gc();
					handler.sendMessage(msg);
				}
			}

		}).start();
	}

	/**
	 * 检查互联网地址是否可以访问-使用DNS解析
	 *
	 * @param hostname   要检查的域名或IP
	 * @param callback 检查结果回调（是否可以解析成功）{@see java.lang.Comparable<T>}
	 */
	public static void isNetWorkAvailableByDNS(final String hostname, final Comparable<Boolean> callback) {
		final Handler handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (callback != null) {
					callback.compareTo(msg.arg1 == 0);
				}
			}

		};
		new Thread(new Runnable() {

			@Override
			public void run() {
				Message msg = new Message();
				try {
					DNSParse parse = new DNSParse(hostname);
					Thread thread = new Thread(parse);
					thread.start();
					thread.join(3 * 1000); // 设置等待DNS解析线程响应时间为3秒
					InetAddress resCode = parse.get(); // 获取解析到的IP地址
					msg.arg1 = resCode == null ? -1 : 0;
				} catch (Exception e) {
					msg.arg1 = -1;
					e.printStackTrace();
				} finally {
					handler.sendMessage(msg);
				}
			}

		}).start();
	}

	/**
	 * DNS解析线程
	 */
	private static class DNSParse implements Runnable {
		private String hostname;
		private InetAddress address;

		public DNSParse(String hostname) {
			this.hostname = hostname;
		}

		public void run() {
			try {
				set(InetAddress.getByName(hostname));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public synchronized void set(InetAddress address) {
			this.address = address;
		}

		public synchronized InetAddress get() {
			return address;
		}
	}

}
