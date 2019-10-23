package io.dcloud.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.net.DownloadMgr;

/**
 * <p>Description:负责分发js过来的下载请求</p>
 * <p>
 * 启动下载，杀死进程进入dispose，下载中异常，均会保存任务到sharepreference中
 * 所以杀死应用进程，重新进入可以枚举得到以前的下载任务
 *
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @version 1.0
 * @Date 2013-3-27 上午11:14:21 created.
 * <p>
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-27 上午11:14:21</pre>
 */
public class DownloadJSMgr {
    /**
     * appid-list<downloadtask>
     */
    public HashMap<String, AppDownloadInfo> mAppsDownloadTasks = null;


    private static DownloadJSMgr mDownloadJSMgr;

    private DownloadJSMgr() {
        if (mAppsDownloadTasks == null) {
            mAppsDownloadTasks = new HashMap<String, AppDownloadInfo>();
        }
    }

    protected static DownloadJSMgr getInstance() {
        if (mDownloadJSMgr == null) {
            mDownloadJSMgr = new DownloadJSMgr();
        }
        return mDownloadJSMgr;
    }

    /**
     * Description:初始化保存的task
     *
     * @param pIWebview TODO
     * @param pAppid    <pre><p>ModifiedLog:</p>
     *                  Log ID: 1.0 (Log编号 依次递增)
     *                  Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-17 下午4:25:17</pre>
     */
    private void initAppDownloadList(IWebview pIWebview, String pAppid) {
        //加载应用的下载列表
        if (mAppsDownloadTasks.containsKey(pAppid)) {//如果当前应用已经初始化则立即返回
            return;
        }
        mAppsDownloadTasks.put(pAppid, new AppDownloadInfo(pIWebview.getContext(), pIWebview, pAppid));
    }

    public String execute(IWebview pWebViewImpl, String pActionName,
                          String pJsArgs[]) {
        String _ret = null;
        try {
            IApp _app = pWebViewImpl.obtainFrameView().obtainApp();
            String _appid = _app.obtainAppId();
            initAppDownloadList(pWebViewImpl, _appid);
            switch (pActionName) {
                case "start":
                case "resume": {
                    String _uuid = pJsArgs[0];
                    JsDownload _nDownloadTask = findDownloadTask(pWebViewImpl, _appid, _uuid);
                    _nDownloadTask.start();
                    String requestHeader = pJsArgs[1];
                    if (!TextUtils.isEmpty(requestHeader)) {
                        JSONObject jsonObject = new JSONObject(requestHeader);
                        if(jsonObject != null && jsonObject.length() > 0) {
                            Iterator<String> keys = jsonObject.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                String value = jsonObject.getString(key);
                                _nDownloadTask.setRequestHeader(key, value);
                            }
                        }
                    }
                    break;
                }
                case "pause": {
                    String _uuid = pJsArgs[0];
                    JsDownload _nDownloadTask = findDownloadTask(pWebViewImpl, _appid, _uuid);
                    _nDownloadTask.mPause = true;
                    //Logger.d("jsdownload","pause " + _uuid);
                    DownloadMgr.getDownloadMgr().removeTask(_nDownloadTask.mDownloadNetWork);
                    break;
                }
                case "abort": {
                    String _uuid = pJsArgs[0];
                    JsDownload _nDownloadTask = findDownloadTask(pWebViewImpl, _appid, _uuid);
                    if (!PdrUtil.isEmpty(_nDownloadTask)) {
                        AppDownloadInfo appDownloadInfo = getAppTaskList(_appid);
                        appDownloadInfo.mList.remove(_nDownloadTask);
                        _nDownloadTask.mPause = true;
                        DownloadMgr.getDownloadMgr().removeTask(_nDownloadTask.mDownloadNetWork);
                        _nDownloadTask.abort();
                    }
                    break;
                }
                case "clear": {
                    AppDownloadInfo appDownloadInfo = getAppTaskList(_appid);
                    int state = Integer.parseInt(pJsArgs[0]);
                    if (appDownloadInfo != null) {
                        int count = appDownloadInfo.mList.size();
                        for (int i = count - 1; i >= 0; i--) {
                            JsDownload _downloadTask = appDownloadInfo.mList.get(i);
                            if (state == -1 //泛指所有下载任务的状态，用于enumerate()和clear()操作时指定作用于所有下载任务。
                                    || state == _downloadTask.mState //指定任务状态
                                    || (state == -10000 && _downloadTask.mState != JsDownload.STATE_COMPLETED) //非下载完成的任务
                            ) {
                                _downloadTask.abort();
                                appDownloadInfo.mList.remove(i);
                            }
                        }
                    }
                    break;
                }
                case "startAll": {
                    AppDownloadInfo appDownloadInfo = getAppTaskList(_appid);
                    if (appDownloadInfo.mList != null) {
                        for (int i = 0; i < appDownloadInfo.mList.size(); i++) {
                            appDownloadInfo.mList.get(i).start();
                        }
                    }
                    break;
                }
                case "enumerate" : {
                    enumerate(pWebViewImpl, pJsArgs[0], pJsArgs[1], _appid);
                    break;
                }
                case "createDownload": {
                    JSONObject _downloadTask = new JSONObject(pJsArgs[0]);
                    pushDownloadTask(pWebViewImpl, _appid, createDownloadTask(pWebViewImpl, _downloadTask));
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return _ret;
    }

    /**
     * Description:枚举download对象
     *
     * @pCallbackId
     * @pArr <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-8 下午2:30:37</pre>
     */
    private void enumerate(IWebview pWebViewImpl, String pCallbackId, String pState, String pAppid) {
        AppDownloadInfo appDownloadInfo = getAppTaskList(pAppid);
        String _arr = null;
        try {
            int _state = Integer.parseInt(pState);
            if (_state == JsDownload.STATE_INIT || _state == JsDownload.STATE_CONNECTING || _state == JsDownload.STATE_CONNECTED ||
                    _state == JsDownload.STATE_RECEIVING || _state == JsDownload.STATE_COMPLETED || _state == JsDownload.STATE_PAUSE) {
                ArrayList<JsDownload> arrs = new ArrayList<JsDownload>();
                if (appDownloadInfo != null && !appDownloadInfo.mList.isEmpty()) {
                    int count = appDownloadInfo.mList.size();
                    for (int i = 0; i < count; i++) {
                        JsDownload _JsDownload = appDownloadInfo.mList.get(i);
                        if(_JsDownload.mWebview.obtainApp() == null) {
                            _JsDownload.mWebview = pWebViewImpl;
                        }
                        if (_state == _JsDownload.mState) {
                            arrs.add(_JsDownload);
                        }
                    }
                }
                _arr = enumerateArr(pWebViewImpl, arrs);
            } else {
                _arr = enumerateArr(pWebViewImpl, appDownloadInfo.mList);
            }
        } catch (Exception e) {
            ArrayList<JsDownload> arrs = new ArrayList<JsDownload>();
            if (appDownloadInfo != null && !appDownloadInfo.mList.isEmpty()) {
                int count = appDownloadInfo.mList.size();
                for (int i = 0; i < count; i++) {
                    JsDownload _JsDownload = appDownloadInfo.mList.get(i);
                    _JsDownload.mWebview = pWebViewImpl;
                    if (_JsDownload.mState != JsDownload.STATE_COMPLETED) {//枚举所有未完成的任务
                        arrs.add(_JsDownload);
                    }
                }
            }
            _arr = enumerateArr(pWebViewImpl, arrs);
        }
        JSUtil.execCallback(pWebViewImpl, pCallbackId, _arr, JSUtil.OK, true, false);
    }

    private String enumerateArr(IWebview pWebViewImpl, ArrayList<JsDownload> pArr) {
        StringBuffer _arr = new StringBuffer();
        _arr.append("[");
        if (pArr != null && !pArr.isEmpty()) {
            int count = pArr.size();
            for (int i = 0; i < count; i++) {
                JsDownload _JsDownload = pArr.get(i);
                _arr.append(_JsDownload.toSaveJSON());
                _JsDownload.addRelWebview(pWebViewImpl);
                if (i != count - 1) {
                    _arr.append(",");
                }
            }
        }
        _arr.append("]");
        return _arr.toString();
    }


    private void pushDownloadTask(IWebview webview, String appid, JsDownload pTask) {
        AppDownloadInfo appDownloadInfo = mAppsDownloadTasks.get(appid);
        if (appDownloadInfo == null) {
            appDownloadInfo = new AppDownloadInfo(webview.getContext(), webview, appid);
            mAppsDownloadTasks.put(appid, appDownloadInfo);
        }
        appDownloadInfo.mList.add(pTask);
    }

    private JsDownload createDownloadTask(IWebview pWebViewImpl, JSONObject pJsonDownload) {
        JsDownload _ret = new JsDownload(this, pWebViewImpl, pJsonDownload);
        return _ret;
    }

    private JsDownload findDownloadTask(IWebview webview, String appid, String uuid) {
        JsDownload _ret = null;
        AppDownloadInfo appDownloadInfo = getAppTaskList(appid);
        if (appDownloadInfo != null) {
            int count = appDownloadInfo.mList.size();
            for (int i = 0; i < count; i++) {//解决AndroidRuntime(7255): java.util.ConcurrentModificationException
                JsDownload _downloadTask = appDownloadInfo.mList.get(i);
                if (uuid.equals(_downloadTask.mUUID)) {
                    if(_downloadTask.mWebview.obtainApp() == null) {
                        _downloadTask.mWebview = webview;
                    }
                    _ret = _downloadTask;
                    break;
                }
            }
        }
        return _ret;
    }

    public void dispose() {
        Iterator<String> _iterator = mAppsDownloadTasks.keySet().iterator();
        while (_iterator.hasNext()) {
            String _appid = _iterator.next();
            AppDownloadInfo appDownloadInfo = getAppTaskList(_appid);
            for (JsDownload _download : appDownloadInfo.mList) {
                _download.saveInDatabase();
            }
        }
        //mDownloadJSMgr = null;
    }

    private AppDownloadInfo getAppTaskList(String appid) {
        return mAppsDownloadTasks.get(appid);
    }

    void saveDownloadTaskInfo(String appid, String uuid, String downloadJSON) {
        Message m = Message.obtain();
        m.what = SAVE;
        m.obj = new String[]{appid, uuid, downloadJSON};
        mHander.sendMessage(m);
    }

    void deleteDownloadTaskInfo(String appid, String key) {
        Message m = Message.obtain();
        m.what = DELETE;
        m.obj = new String[]{appid, key};
        mHander.sendMessage(m);
    }

    class AppDownloadInfo {
        String appid;
        /**
         * 保存的数据结构：
         * uuid = {uuid:''}(jsdownload)
         */
        SharedPreferences sharePref;
        ArrayList<JsDownload> mList = null;

        AppDownloadInfo(Context context, IWebview webview, String appid) {
            this.appid = appid;
            //每一个应用存放一个share_pref.xml
            String name = appid + JsDownload.DOWNLOAD_NAME;
            this.sharePref = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            this.mList = new ArrayList<JsDownload>();
            Map _task = this.sharePref.getAll();
            if (_task != null) {//读取保存的下载列表
                Iterator _it = _task.keySet().iterator();
                ArrayList<JsDownload> _list = this.mList;
                while (_it.hasNext()) {
                    String _key = (String) _it.next();
//					if(_key.startsWith("id_")){//下载任务uuid
                    String _json = (String) _task.get(_key);
                    try {
                        JsDownload d = new JsDownload(DownloadJSMgr.this, webview, new JSONObject(_json));
//							long t = (Long) _task.get(d.mUUID + "_t");
//							long ds = (Long) _task.get(d.mUUID + "_d");
//							d.mTotalSize = t;
//							d.mFileSize = ds;
                        _list.add(d);
                    } catch (JSONException e) {

                    }
//					}
                }
                this.mList = _list;
            }
        }
    }

    AppDownloadInfo curAppSharePref = null;
    static final int SAVE = 1;
    static final int DELETE = 2;
    Handler mHander = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SAVE) {
                String[] params = (String[]) msg.obj;
                String appid = params[0];
                String key = params[1];
                String value = params[2];
                SharedPreferences _sh = getAppTaskList(appid).sharePref;
                Editor _ed = _sh.edit();
                _ed.putString(key, value);
                _ed.commit();
//				Logger.d("JSdownload: commit");
            } else if (msg.what == DELETE) {
                String[] params = (String[]) msg.obj;
                String appid = params[0];
                String key = params[1];
                SharedPreferences _sh = getAppTaskList(appid).sharePref;
                if (!TextUtils.isEmpty(_sh.getString(key, ""))) {
                    Editor _ed = _sh.edit();
                    _ed.remove(key).commit();
                }
            }
        }
    };
}
