package io.dcloud.net;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import io.dcloud.common.DHInterface.IReqListener;
import io.dcloud.common.DHInterface.IReqListener.NetState;
import io.dcloud.common.DHInterface.IResponseListener;
import io.dcloud.common.adapter.util.DCloudTrustManager;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.net.NetWork;
import io.dcloud.common.util.net.RequestData;

/**
 * http请求包装类（仅处理http请求相关操作）
 * 通过调用设置监听句柄处理网络请求接收数据
 *
 * @version 1.0
 * @author yanglei Email:yanglei@dcloud.io
 * @Date 2013-3-18 下午02:53:38 created.
 * 
 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-3-18 下午02:53:38
 */
public class DownloadNetWork extends NetWork {
	
	private Thread mExecSyncTask;
	public int mStatus = 0;
	public int mRetry = 0;
	/**
	 * 请求文件长度
	 */
	public long mContentLength;
	//网络句柄
	public HttpURLConnection mUrlConn = null;
	protected boolean isStop = false;
	
	public Map<String, String> mResponseHeaders;
	
	private String mUrl;
	/**
	 * Description: 构造函数 
	 * @param pWorkType
	 * @param pRequestData
	 * @param pReqListener
	 * @param pResponseListener 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-10 下午2:50:09</pre>
	 */
	public DownloadNetWork(int pWorkType, RequestData pRequestData,
			IReqListener pReqListener, IResponseListener pResponseListener) {
		super(pWorkType, pRequestData, pReqListener, pResponseListener);
		mUrl = pRequestData.getUrl();
	}
	
	public void onStateChanged(NetState state) {
		if(mReqListener != null){
			mReqListener.onNetStateChanged(state,isAbort);
		}
	}
	public int onReceiveing(InputStream is) throws Exception {
		if(mReqListener != null){
			return mReqListener.onReceiving(is);
		}
		return 0;
	}

	public void onResponsing(OutputStream os) {
		if(mReqListener != null){
//			mReqListener.onResponsing(os);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
//		super.run();
		initUploadData();
	}
	
	/**
	 * Description:返回响应
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-29 下午4:07:59</pre>
	 * @throws Exception 
	 */
	private void response(InputStream pIs) throws Exception {
		
		onStateChanged(NetState.NET_HANDLE_BEGIN);
		//处理网络下发数据
		onReceiveing(pIs);
		if(!isStop) {
			onStateChanged(NetState.NET_HANDLE_ING);
			//网络数据处理完毕
			onStateChanged(NetState.NET_HANDLE_END);
		}
	
	}
	@Override
	public void cancelWork() {
		if(mUrlConn != null){
            try {
                mUrlConn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //vivo 手机会报此异常
            // java.lang.NullPointerException: Attempt to read from field 'int com.android.okio.Segment.limit' on a null object reference
            //at com.android.okio.OkBuffer.write(OkBuffer.java:574)
			mUrlConn = null;
		}
	}
	/**
	 * Description:初始化上传数据
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-29 下午4:07:55</pre>
	 */
	private void initUploadData() {
		try {
			URL url = new URL(mUrl);
			Logger.d("httpreq","request mUrl=" + mUrl);
			onStateChanged(NetState.NET_INIT);
			connect(url);
//		    OutputStream os = mUrlConn.getOutputStream();
//		    //网络建立前的预备处理
//		    onResponsing(os);
		} catch (Exception e) {
			e.printStackTrace();
			onStateChanged(NetState.NET_ERROR);
		}
	}
	
	private void setHeaders(){
		mRequestData.addHeader(mUrlConn);
	}
	
//	class myX509TrustManager implements X509TrustManager {
//	    
//	    public void checkClientTrusted(X509Certificate[] chain, String authType) {
//	    }
//
//	    public void checkServerTrusted(X509Certificate[] chain, String authType) {
//	        System.out.println("cert: " + chain[0].toString() + ", authType: " + authType);
//	    }
//
//	    public X509Certificate[] getAcceptedIssuers() {
//	        return null;
//	    }
//	}
//	class myHostnameVerifier implements HostnameVerifier {
//	    public boolean verify(String hostname, SSLSession session) {
//	        System.out.println("Warning: URL Host: " + hostname + " vs. " + session.getPeerHost());
//	        return true;
//	    }
//	}
//
//	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
//		 
//        public boolean verify(String hostname, SSLSession session) {
//            return true;
//        }
//    };

///**
//* Trust every server - dont check for any certificate
//*/
//	private static void trustAllHosts() {
//		final String TAG = "trustAllHosts";
//		// Create a trust manager that does not validate certificate chains
//		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
//
//			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//				return new java.security.cert.X509Certificate[] {};
//			}
//
//			public void checkClientTrusted(X509Certificate[] chain,
//					String authType) throws CertificateException {
//				Logger.i(TAG, "checkClientTrusted");
//			}
//
//			public void checkServerTrusted(X509Certificate[] chain,
//					String authType) throws CertificateException {
//				Logger.i(TAG, "checkServerTrusted");
//			}
//		} };
//
//		// Install the all-trusting trust manager
//		try {
//			SSLContext sc = SSLContext.getInstance("TLS");
//			sc.init(null, trustAllCerts, new java.security.SecureRandom());
//			HttpsURLConnection
//					.setDefaultSSLSocketFactory(sc.getSocketFactory());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	/**
	 * 
	 * Description:连接服务器
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-29 下午4:08:13</pre>
	 */
	public void connect(URL pUrl){
		try {
			//Properties prop = System.getProperties();
// proxy host IP address
			//String proxyHost = "192.168.12.107";
// proxy port
			//String proxyPort = "8888";
			//prop.put("proxySet", "true");
			//prop.put("proxyHost", proxyHost);
			//prop.put("proxyPort", proxyPort);

			//建立网络请求
			mUrlConn = (HttpURLConnection)pUrl.openConnection();
			setHeaders();
			if(PdrUtil.isEquals(mRequestData.unTrustedCAType,"refuse") || PdrUtil.isEquals(mRequestData.unTrustedCAType,"warning")){
			}else if(mUrlConn instanceof HttpsURLConnection){//manifest.json配置允许使用https请求
				try {
					((HttpsURLConnection) mUrlConn).setSSLSocketFactory(DCloudTrustManager.getSSLSocketFactory());
					((HttpsURLConnection) mUrlConn).setHostnameVerifier(DCloudTrustManager.getHostnameVerifier(false));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			// 360免流量
//			if (BaseInfo.isTrafficFree()) {
//				mUrlConn.setRequestProperty("Host", InvokeExecutorHelper.TrafficFreeHelper.invoke("host"));
//				
//				String host = pUrl.getHost();
//				if (!TextUtils.isEmpty(host)) {
//					mUrlConn.setRequestProperty("RealHost", host);
//				}
//				mUrlConn.setRequestProperty("RealUri", pUrl.getFile());
//				if (!TextUtils.isEmpty(getPAuth())) {
//					mUrlConn.setRequestProperty("P-Auth", getPAuth());
//				}
//			}
			//
			
			onStateChanged(NetState.NET_REQUEST_BEGIN);
			if(!isStop){
				//获取请求状态码
	            mStatus = mUrlConn.getResponseCode();
				//断点重连后206状态值改为200
				if(mStatus == 206){
					mStatus = 200;
				}

				if(mStatus == 302 || mStatus == 301){
					String newURL = mUrlConn.getHeaderField("Location");
					if (!newURL.equals(pUrl.toString())){
						URL reqURL = new URL(newURL);
						connect(reqURL);
						return;
					}
				}
				//网络通道建立成功
				onStateChanged(NetState.NET_CONNECTED);
				InputStream is = mUrlConn.getInputStream();
				mResponseHeaders = getHttpResponseHeader(mUrlConn);
				response(is);
			}
		} catch (Throwable throwable) {
			if(isStop){//取消或，下载完成时，断开请求引发的异常，在此不处理
				return;
			}
			long _nextconnecttime = System.currentTimeMillis() + mRetryIntervalTime *(1<<mTimes)/2;
			if(mTimes <= MAX_TIMES){
				while(true) {
					if(System.currentTimeMillis() > _nextconnecttime){
						mTimes++;
                        try {
                            mUrlConn.disconnect();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        this.connect(pUrl);
						break;
					}
				}
			}else{
				if(throwable instanceof SocketTimeoutException){
					onStateChanged(NetState.NET_TIMEOUT);
				}else if(isStop){//程序退出的暂停
//					onStateChanged(NetState.NET_PAUSE);
					if(mReqListener instanceof JsDownload){
						((JsDownload) mReqListener).saveInDatabase();
					}
				}else{
					onStateChanged(NetState.NET_ERROR);
				}
			}
		} 
	}
	@Override
	public void dispose() {
		mTimes = 4;
		isStop = true;
        mReqListener = null;
        try {
            if(mUrlConn != null){
                mUrlConn.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

	}
	
    public static Map<String, String> getHttpResponseHeader(
            HttpURLConnection http) throws UnsupportedEncodingException {
        Map<String, String> header = new LinkedHashMap<String, String>();
        for (int i = 0;; i++) {
            String mine = http.getHeaderField(i);
            if (mine == null)
                break;
            header.put(http.getHeaderFieldKey(i), mine);
        }
        return header;
    }
    
	public String getResponseHeader(String name) {
		if (mResponseHeaders == null) {
			return "''";
		}

		Iterator it = mResponseHeaders.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String key = (String) entry.getKey();
			if (name.equalsIgnoreCase(key.trim())) {
				return (String) entry.getValue();
			}
		}
		return "''";
	}
	
	public String getResponseHeaders() {
		if (mResponseHeaders != null && mResponseHeaders.size() > 0) {
			try {
				JSONObject jsonObject = new JSONObject(mResponseHeaders);
				return jsonObject.toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return "{}";
	}
}
