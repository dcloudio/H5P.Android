package io.dcloud.net;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;


import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IApp.ConfigProperty;
import io.dcloud.common.DHInterface.IReqListener;
import io.dcloud.common.DHInterface.IResponseListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.IOUtil;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.ThreadPool;
import io.dcloud.common.util.net.DownloadMgr;
import io.dcloud.common.util.net.NetWork;
import io.dcloud.common.util.net.RequestData;

public class JsDownload implements IReqListener, IResponseListener {

    static final int STATE_UNDEFINED = -1000;
    static final int STATE_UNKOWN = -1;
    static final int STATE_INIT = 0;
    static final int STATE_CONNECTING = 1;
    static final int STATE_CONNECTED = 2;
    static final int STATE_RECEIVING = 3;
    static final int STATE_COMPLETED = 4;
    static final int STATE_PAUSE = 5;
    public static final String DOWNLOAD_NAME = "_download_dcloud";
    int mState = STATE_UNDEFINED;
    String mUUID = null;
    String mUrl = null;
    String mRealURL = null;
    boolean append = false;
    private long responseOffset = 0;
    private String sAppid;
    private String sharedPreferenceName;
    private boolean mDownloadComplete;

    /**
     * 下载工作
     */
    DownloadNetWork mDownloadNetWork = null;
    /**
     * 下载请求
     */
    RequestData mRequestData = null;
    /**
     * 创建当前下载任务对象的Iwebview句柄
     */
    public IWebview mWebview = null;
    /**
     * 相关联的IWebview对象句柄s
     */
    private ArrayList<IWebview> mRelWebviews = null;
    /**
     * js层传入的options值
     */
    JSONObject mOptions = null;
    /**
     * 返回到js层的相对路径
     */
    String mFileName = "";
    /**
     * 要保存文件父目录全路径
     */
    private String mParentPath = null;
    /**
     * 下载保存文件的全路径
     */
    private File mFile = null;
    //下载文件的输出流
    RandomAccessFile mFileOs = null;
    /**
     * 下载文件大小
     */
    long mFileSize = 0;
    /**
     * 目标大小
     */
    long mTotalSize = 0;
    /**
     * 任务网络请求模式
     */
    String mMethod;
    /**
     * 任务优先级
     */
    int mPriority;
    /**
     * 任务重连次数
     */
    int mRetry = 3;

    /**
     * Post请求时设置的数据
     */
    String mData;

    /**
     * 下载出错重试时间。
     */
    private long mRetryIntervalTime;

    String mConfigFilePath = null;
    private DownloadJSMgr mDownloadMgr = null;
    /**
     * 是否暂停了
     */
    public boolean mPause;
    /**
     * 是否取消了
     */
    public boolean mAbort;

    /**
     * Description: 构造函数
     *
     * @param downloadJSMgr TODO
     * @param pWebview      当前webview句柄
     * @param pJsonDownload task任务创建时所需属性
     *                      </pre> Create By: yanglei Email:yanglei@dcloud.io at 2013-3-18 下午05:26:40
     */
    JsDownload(DownloadJSMgr downloadJSMgr, IWebview pWebview, JSONObject pJsonDownload) {
        this.mDownloadMgr = downloadJSMgr;
        mWebview = pWebview;
        mRelWebviews = new ArrayList<IWebview>();
        mRelWebviews.add(pWebview);
        parseJson(pJsonDownload);
        mRequestData = new RequestData(mUrl, mMethod);
        mRequestData.unTrustedCAType = pWebview.obtainApp().obtainConfigProperty(ConfigProperty.CONFIG_UNTRUSTEDCA);
        if (!pJsonDownload.isNull(StringConst.JSON_KEY_TIMEOUT)) {
            mRequestData.mTimeout = pJsonDownload.optInt(StringConst.JSON_KEY_TIMEOUT) * 1000;
        }
        mRequestData.addHeader(IWebview.USER_AGENT, pWebview.getWebviewProperty(IWebview.USER_AGENT));
//        String cookie = pWebview.getWebviewProperty(mUrl);
//        if (!PdrUtil.isEmpty(cookie)) {
//            mRequestData.addHeader(IWebview.COOKIE, cookie);
//        }
        mDownloadNetWork = new DownloadNetWork(NetWork.WORK_DOWNLOAD, mRequestData, this, this);
        mDownloadNetWork.MAX_TIMES = mRetry;
        mDownloadNetWork.mPriority = mPriority;
        mDownloadNetWork.setRetryIntervalTime(mRetryIntervalTime);
        sAppid = mWebview.obtainFrameView().obtainApp().obtainAppId();
        sharedPreferenceName = sAppid + DOWNLOAD_NAME;
    }

    /**
     * Description:解析js传递来的json对象
     * <p>
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-22 下午1:55:44</pre>
     */
    private void parseJson(JSONObject pJsonDownload) {
        mOptions = pJsonDownload.optJSONObject(StringConst.JSON_KEY_OPTIONS);
        mUrl = JSONUtil.getString(pJsonDownload, StringConst.JSON_KEY_URL);
        String sRealURL = JSONUtil.getString(pJsonDownload,StringConst.JSON_KEY_REALURL);
        if (sRealURL != null && !sRealURL.equalsIgnoreCase("null") && !sRealURL.equalsIgnoreCase(mUrl)){
            mUrl = sRealURL;
        }
        mUUID = JSONUtil.getString(pJsonDownload, StringConst.JSON_KEY_ID);//js层传入值
        if (TextUtils.isEmpty(mUUID)) {//native层保存的值
            mUUID = JSONUtil.getString(pJsonDownload, "uuid");

        }
        mMethod = JSONUtil.getString(pJsonDownload, StringConst.JSON_KEY_METHOD);
        mPriority = JSONUtil.getInt(pJsonDownload, StringConst.JSON_KEY_PRIORITY);
        mRetry = JSONUtil.getInt(pJsonDownload, StringConst.JSON_KEY_RETRY);
        mFileSize = JSONUtil.getInt(pJsonDownload, StringConst.JSON_KEY_DOWNLOADEDSIZE);
        mTotalSize = JSONUtil.getInt(pJsonDownload, StringConst.JSON_KEY_TOTALSIZE);
        String str_state = JSONUtil.getString(pJsonDownload, StringConst.JSON_KEY_STATE);
        if (!PdrUtil.isEmpty(str_state)) {
            try {
                mState = Integer.parseInt(str_state);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        mConfigFilePath = BaseInfo.sDownloadFullPath + mUUID + ".download";
        File tempFile = new File(mConfigFilePath);
        try {
            //读取下载配置文件中的数据，存在数据时，以此为准
            if (tempFile.exists()) {
                FileInputStream fis = new FileInputStream(tempFile);
                String strConfig = IOUtil.toString(fis);
                strConfig.replace("\n", "");
                String[] nums = strConfig.split("-");
                mTotalSize = Long.parseLong(nums[0]);
                mState = Integer.parseInt(nums[2]);
                String sFilepath = nums[3];
                sFilepath = sFilepath.replace("\n","");
                mFile = new File(sFilepath);
                if(mFile.exists()){
                    mFileSize = mFile.length();
                }else{
                    mFileSize = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        String fileName = JSONUtil.getString(pJsonDownload, StringConst.JSON_KEY_FILENAME);
        if (TextUtils.isEmpty(fileName)) {
            fileName = JSONUtil.getString(mOptions, StringConst.JSON_KEY_FILENAME);
        }
        initPath(fileName);
        mData = JSONUtil.getString(pJsonDownload, StringConst.JSON_KEY_DATA);
        mRetryIntervalTime = JSONUtil.getLong(pJsonDownload, StringConst.JSON_KEY_RETRY_INTERVAL_TIME) * 1000;
    }

    private static boolean startsWith(String src, String prefix, boolean nextCharIsDevide) {
        if (src != null && src.startsWith(prefix)) {
            if (nextCharIsDevide) {
                String s = src.substring(prefix.length());//去除已经符合的前缀，然后判断接下来的字符是否为'/'
                return s.length() == 0 || (s.length() > 1 && s.charAt(0) == '/');
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Description:初始化文件下载路径
     *
     * @param fileName <pre><p>ModifiedLog:</p>
     *                 Log ID: 1.0 (Log编号 依次递增)
     *                 Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-22 下午2:25:46</pre>
     */
    private void initPath(String fileName) {
        IApp _app = mWebview.obtainFrameView().obtainApp();
        if (PdrUtil.isEmpty(fileName)) {//未指定下载目录
            mParentPath = new File(BaseInfo.sDownloadFullPath).getParent() + "/";
            mFileName = BaseInfo.REL_PUBLIC_DOWNLOADS_DIR + "/";
        } else {
            mFileName = fileName;
            if (startsWith(fileName, BaseInfo.REL_PRIVATE_DOC_DIR, true)) {//是否为应用的私有doc目录
//				String convertPath = _app.convert2AbsFullPath(mWebview.obtainFullUrl(),fileName);
                mParentPath = new File(_app.obtainAppDocPath()).getParent() + "/";
//				if(fileName.endsWith("/")){//只指定了根目录
//					if(_first < fileName.length()){
//						mParentPath = convertPath;
//					}
//				}else{//指定了下载文件的名称
//					int _first = fileName.indexOf("/");
//					int _pos = fileName.lastIndexOf("/");
//					if(_pos < fileName.length()){
//						mFileName = fileName.substring(_pos + 1);
//						if(_first < fileName.length() && _first < _pos){
//							mParentPath = mParentPath + fileName.substring(_first+1,_pos+1);
//						}
//					}
//				}
            } else if (startsWith(fileName, BaseInfo.REL_PUBLIC_DOCUMENTS_DIR, true)) {//公共目录documents时
                mParentPath = new File(BaseInfo.sDocumentFullPath).getParent() + "/";
            } else if (startsWith(fileName, BaseInfo.REL_PUBLIC_DOWNLOADS_DIR, true)) {//公共目录downloads时
                mParentPath = new File(BaseInfo.sDownloadFullPath).getParent() + "/";
//				int pos = fileName.lastIndexOf("/");
//				if(pos < fileName.length()){
//					mParentPath += fileName.substring(BaseInfo.REL_PUBLIC_DOWNLOADS_DIR.length() + 1,pos+1);
//					mFileName = fileName.substring(pos + 1);
//				}
            } else {//默认使用downloads目录
                mParentPath = new File(BaseInfo.sDownloadFullPath).getParent() + "/";
                //枚举获得的下载任务相关信息中包含名称，此时走下面代码目标文件会产生重复路径
//				mFileName = BaseInfo.REL_PUBLIC_DOWNLOADS_DIR + (fileName.startsWith("/") ? fileName : "/" + fileName);
            }
        }
//		mRelFilePath = mWebview.obtainFrameView().obtainApp().convert2RelPath(mParentPath + mFileName);
    }

    /**
     * 执行下载状态回调
     *
     * @param state
     */
    private void onStateChanged(int state) {
        if (state == STATE_RECEIVING || state == STATE_PAUSE) {
            mState = state;
        }
        String json = toJSON();
        int size = mRelWebviews.size();
        for (int i = 0; i < size; i++) {
            IWebview webview = mRelWebviews.get(i);
            JSUtil.excDownloadCallBack(webview, json, mUUID);
        }
        if (mAbort) {
            return;
        }
        saveDownloadState();
    }

    private void saveDownloadState() {

        if (mFileSize < mTotalSize) {
            try {

                FileOutputStream fos = new FileOutputStream(new File(mConfigFilePath), false);
                fos.write((mTotalSize + "-" + mFileSize + "-" + mState + "-" + mFile.getAbsolutePath()).getBytes());
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean justDirPath() {
        return TextUtils.isEmpty(mFileName) || mFileName.endsWith("/");
    }

    @Override
    public void onNetStateChanged(NetState state, boolean isAbort) {
        if (mPause) return;//暂停状态事件都不执行
        if (state == NetState.NET_INIT) {
            mState = STATE_INIT;
            mDownloadComplete = false;
        } else if (state == NetState.NET_CONNECTED) {
            mState = STATE_CONNECTED;
        } else if (state == NetState.NET_HANDLE_END) {
            mState = STATE_COMPLETED;
            Logger.d("----NetState.NET_HANDLE_END-----");
            DownloadMgr.getDownloadMgr().removeTask(mDownloadNetWork);
            deleteDownloadData();
            mDownloadComplete = true;
            checkSpecialFile(mFileName);
        } else if (state == NetState.NET_ERROR) {
            mState = STATE_COMPLETED;
            mDownloadNetWork.mStatus = 400;
            Logger.d("----NetState.NET_ERROR-----");
            DownloadMgr.getDownloadMgr().removeTask(mDownloadNetWork);
            if (mDownloadComplete) {
                // 防止已经下载完成后还会毁掉错误信息。解决在5.0上多线程多现成对httpurlconnection进行关闭和读取时出现空指针
                return;
            }
        } else if (state == NetState.NET_REQUEST_BEGIN) {
            try {
                //获取当前已下载大小，设置range
//				handleFileSize(isAbort);
                if (mFileSize > 0) {
                    mDownloadNetWork.mUrlConn.setRequestProperty("Range", "bytes=" + String.valueOf(mFileSize) + "-");
                }
                mDownloadNetWork.mUrlConn.setRequestMethod(mMethod);
                if (mMethod.equals("POST")) {
                    mDownloadNetWork.mUrlConn.setDoOutput(true);
                    mDownloadNetWork.mUrlConn.getOutputStream().write(mData.getBytes("utf8"));
                    mDownloadNetWork.mUrlConn.getOutputStream().flush();
                    mDownloadNetWork.mUrlConn.getOutputStream().close();
                    mDownloadNetWork.mUrlConn.setChunkedStreamingMode(0);
                }
                mDownloadNetWork.mUrlConn.setConnectTimeout(mRequestData.mTimeout);
                mDownloadNetWork.mUrlConn.setReadTimeout(mRequestData.mTimeout);
            } catch (Exception e) {
                e.printStackTrace();
//				handleFileSize(isAbort);
            }
        } else if (state == NetState.NET_HANDLE_BEGIN) {
            String cl = mDownloadNetWork.mUrlConn.getHeaderField("Content-Length");
            String cr = mDownloadNetWork.mUrlConn.getHeaderField("Content-Range");
            String ct = mDownloadNetWork.mUrlConn.getHeaderField("Content-Type");
            String set_cookie = mDownloadNetWork.mUrlConn.getHeaderField(IWebview.SET_COOKIE);
            if (!PdrUtil.isEmpty(set_cookie)) {
                CookieManager.getInstance().setCookie(mRequestData.getUrl(), set_cookie);
            }
            if (cr == null) {
                mTotalSize = PdrUtil.parseLong(cl, 0);
                //不支持断点下载，需要把之前未完成的下载文件进行删除
                mFileSize = 0;
                responseOffset = 0;
                if (mFile != null && mFile.exists()) {//删除临时文件，重新下载
                    mFile.delete();
                    new File(mConfigFilePath).delete();
                }
            } else {
                append = true;
                try {
                    MessageFormat format = new MessageFormat("bytes {0,number}-{1,number}");
                    format.setLocale(Locale.US);
                    java.lang.Object parts[] = format.parse(cr);
                    responseOffset = ((Number)parts[0]).longValue();
                    if (responseOffset < 0) {
                        responseOffset = 0;
                    }
                }
                catch (Exception e) {
                    responseOffset = 0;
                }
            }
            if (justDirPath()) {
                //获取网络http头信息文件名称
                String _httpFilename = mDownloadNetWork.mUrlConn.getHeaderField("content-disposition");
                try {
                    if (PdrUtil.isEmpty(_httpFilename)) {
                        String p = mDownloadNetWork.mUrlConn.getURL().getFile().toString();
                        int pos = 0;
                        if ((pos = p.lastIndexOf('/')) >= 0) {
                            p = p.substring(pos + 1);
                            if (p.indexOf('.') >= 0) {//存在后缀,则认为成功
                                if (p.contains("?")) {
                                    p = p.substring(0,p.indexOf("?"));
                                }
                                mFileName += p;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (justDirPath()) {
                    mFileName += PdrUtil.getDownloadFilename(_httpFilename, ct, mUrl);
                }
//				//获取头信息中总大小，顺便校验大小是否相同，如果相同则认为服务器没有更新，不必下载
//				handleFileSize(false);
            }

            if (mDownloadNetWork.isStop) {
                return;
            }

            try {
                mFile = createDownloadFile(append);
                mFileOs = new RandomAccessFile(mFile, "rw");
                mFileOs.seek(responseOffset);
            } catch (IOException e) {
                e.printStackTrace();
                saveInDatabase();
            }
            return;
//		}else if(state == NetState.NET_PAUSE){
//			mState = STATE_PAUSE;
        }
        onStateChanged(mState);
    }

//	private void initFilesize(){
//		File f = new File(mParentPath + mFileName);
//		mFileSize = DHFile.length(f);
//	}

    //	private String mRelFilePath = null;
    private File createDownloadFile(boolean appendOperate) {
        try {
//            String realPath = mWebview.obtainApp().convert2AbsFullPath(mFileName);//转化相对路径为全路径
            String realPath = getRealPath(mFileName);
            if (realPath != null) {
                File file = new File(realPath);
                if (appendOperate && file.exists()) return file;//支持断点 文件存在时
                String f_fileName = mFileName;
                int i = mFileName.lastIndexOf(".");
                final String f_Name = i<0 ? f_fileName : f_fileName.substring(0, i);
                final String f_Type = i<0 ? "": f_fileName.substring(i);
                String t_fileName = f_fileName;
                int index = 1;
                while (file.exists()) {//检查要创建的文件是否存在,存在时按照规范进行 FileName(i)处理
                    t_fileName = f_Name + "(" + index + ")" + f_Type;//为了生成相对路径
//                    realPath = mWebview.obtainApp().convert2AbsFullPath(t_fileName);//为了生成文件系统路径
                    realPath = getRealPath(t_fileName);
                    file = new File(realPath);
                    index++;
                }
                mFileName = t_fileName;
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
                return file;
            }
        } catch (Exception e) {
            if (mFileName!=null && mFileName.toLowerCase().startsWith("file://")) {
                mFileName = mFileName.substring("file://".length());
                File downloadFile = createDownloadFile(append);
                return downloadFile;
            }
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onResponsing(InputStream os) {
        String json = toJSON();
        mState = STATE_CONNECTING;
        int size = mRelWebviews.size();
        for (int i = 0; i < size; i++) {//将事件分发给与此任务先关的Webview
            IWebview webview = mRelWebviews.get(i);
            JSUtil.excDownloadCallBack(webview, json, mUUID);
        }
    }

    @Override
    public int onReceiving(InputStream is) throws Exception {
        byte[] buffer = new byte[10240];
        if (is != null) {
            mDownloadNetWork.mTimes = 1;
            boolean bSaveToDataBase = false;
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                if (mPause) {
                    onStateChanged(STATE_PAUSE);
                    return -1;
                }
                mFileOs.write(buffer, 0, len);
                mFileSize += len;
                onStateChanged(STATE_RECEIVING);
                if(!bSaveToDataBase){
                    bSaveToDataBase = true;
                    mRealURL = mDownloadNetWork.mUrlConn.getURL().toString();
                    saveInDatabase();
                }
            }
            mFileOs.close();
        }
        if (mFileSize < mTotalSize) {
            throw new RuntimeException();
        } else {
            boolean ret = new File(mConfigFilePath).delete();
        }
        return -1;//标志处理完毕输入流
    }

    /**
     * Description:获取download的json字符
     *
     * @return <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-7 下午5:20:51</pre>
     */
    public String toJSON() {
        String _json = "{status: %d,state: %d,filename: '%s',uuid: '%s',downloadedSize:%d,totalSize:%d,headers:%s}";
//		String relPath = mRelFilePath;
        if (mState == STATE_UNDEFINED) {//undefined状态，不回传state
            _json = "{status: %d,filename: '%s',uuid: '%s',downloadedSize:%d,totalSize:%d,headers:%s}";
            _json = String.format(_json, mDownloadNetWork.mStatus, mFileName, mUUID, mFileSize, mTotalSize, mDownloadNetWork.getResponseHeaders());
        } else {
            _json = String.format(_json, mDownloadNetWork.mStatus, mState, mFileName, mUUID, mFileSize, mTotalSize, mDownloadNetWork.getResponseHeaders());
        }
        return _json;
    }

    /**
     * Description:开始下载任务
     * <p>
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-8 下午2:58:53</pre>
     */
    public void start() {
        /**重置下载 次数**/
        mDownloadNetWork.mTimes = 1;
        mPause = false;
        DownloadMgr.getDownloadMgr().addQuestTask(mDownloadNetWork);
        saveInDatabase();
    }

    /**
     * Description:清除下载任务
     * <p>
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-8 下午2:55:12</pre>
     */
    public void abort() {
        try {
            mAbort = true;
            //取消下载任务后，切断该网络请求，并将文件输出流关闭
            ThreadPool.self().addThreadTask(
                    new Runnable() {

                        @Override
                        public void run() {
                            mDownloadNetWork.cancelWork();
                            DownloadMgr.getDownloadMgr().removeTask(mDownloadNetWork);
                            deleteDownloadData();
                            if (!PdrUtil.isEmpty(mFileOs))
                                try {
                                    mFileOs.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            if (mFile != null) {
                                mFile.delete();
                            }
                            mWebview = null;
                            mRelWebviews.clear();
                        }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Description:保存到数据库
     * <p>
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-22 下午3:19:45</pre>
     */
    public void saveInDatabase() {
        //以uuid作为key
        this.mDownloadMgr.saveDownloadTaskInfo(sAppid, mUUID, toSaveJSON());
    }

    /**
     * 下载完成后删除下载临时数据
     */
    public void deleteDownloadData() {
        this.mDownloadMgr.deleteDownloadTaskInfo(sAppid, mUUID);
        new File(mConfigFilePath).delete();
    }

    /**
     * 记录与当前下载任务相关的webview，方便事件分发
     *
     * @param webview <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-7-8 上午10:56:30
     */
    public void addRelWebview(IWebview webview) {
        if (!mRelWebviews.contains(webview)) {
            mRelWebviews.add(webview);
        }
    }

    /**
     * Description:获取保存的JSON格式
     *
     * @return <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-17 下午5:31:01</pre>
     */
    String toSaveJSON() {
        String _json = "{url: '%s',uuid: '%s',method: '%s',priority: %d,timeout:%d,retry:%d,filename:'%s',downloadedSize:%d,totalSize:%d,state: %d,options:%s,RealURL:'%s'}";
        _json = String.format(_json, mUrl, mUUID, mMethod, mPriority, mRequestData.mTimeout, mRetry, mFileName, mFileSize, mTotalSize, mState, mOptions.toString(), mRealURL);
        return _json;
    }

    @Override
    public void onResponseState(int pState, String pStatusText) {

    }

    public void setRequestHeader(String key, String value) {
        mRequestData.addHeader(key, value);
    }


    private void checkSpecialFile(String msg) {
        if (!TextUtils.isEmpty(msg) && BaseInfo.ISAMU) {
            int msgSize = msg.length();
            String apk = ".apk";
            String wgt = ".wgt";
            String wgtu = ".wgtu";
            if ((msgSize - msg.indexOf(apk) - apk.length() == 0) ||
                    (msgSize - msg.indexOf(wgt) - wgt.length() == 0) ||
                    (msgSize - msg.indexOf(wgtu) - wgtu.length() == 0)) {
                if (mWebview != null) {
                    //TestUtil.PointTime.commitStreamAppRemind(mWebview.obtainApp().obtainAppId(), msg, mWebview.obtainApp().obtainAppVersionName(), 2);
                    try {
                        JSONObject msgJson = new JSONObject();
                        msgJson.put("type", "download");
                        msgJson.put("file", mParentPath + mFileName);
                        msgJson.put("url", mUrl);
                        msgJson.put("appid", mWebview.obtainApp().obtainOriginalAppId());
                        msgJson.put("version", mWebview.obtainApp().obtainAppVersionName());
                        Log.i(StringConst.HBUILDER_TAG, msgJson.toString());
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 生成存储路径
     * @return
     */
    public String getRealPath(String pathJs) {
        String realPath = null;
        // 以"/"开头非sdcard目录，保存到downloads文件夹
        if (pathJs!=null && pathJs.startsWith("/") && !pathJs.toLowerCase().startsWith("/sdcard")) {
            pathJs = pathJs.substring(1);
            realPath = mWebview.obtainApp().convert2AbsFullPath(pathJs);//转化相对路径为全路径
            // www不能主动写入，下载转移到downloads目录
            if (realPath != null && realPath.contains("/www/")) {
                realPath = realPath.replace("/www/", "/" + BaseInfo.REAL_PUBLIC_DOWNLOADS_DIR);
            }
        // 保存到sdcard
        } else if (pathJs!=null && pathJs.toLowerCase().startsWith("/sdcard")) {
            realPath = pathJs;
        } else {
            realPath = mWebview.obtainApp().convert2AbsFullPath(pathJs);//转化相对路径为全路径
            // www不能主动写入，下载转移到downloads目录
            if (realPath != null && realPath.contains("/www/")) {
                realPath = realPath.replace("/www/", "/" + BaseInfo.REAL_PUBLIC_DOWNLOADS_DIR);
            }
        }
        return realPath;
    }

}
