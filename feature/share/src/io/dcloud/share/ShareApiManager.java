package io.dcloud.share;

import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IMgr.MgrEvent;
import io.dcloud.common.DHInterface.IMgr.MgrType;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameItem.LayoutParamsUtil;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;

/**
 * <p>Description:分享api管理类</p>
 *
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @version 1.0
 * @Date 2013-5-27 下午4:08:23 created.
 * <p>
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-27 下午4:08:23</pre>
 */
public class ShareApiManager {

    private AbsMgr mFeatureMgr;
    private HashMap<String, IFShareApi> mShareApis;
    private HashMap<String, ShareAuthorizeView> mAuthorizeViews;

    private HashMap<String, String> mShareApiNames;

    protected ShareApiManager(AbsMgr pFeatureMgr, String pFeatureName) {
        mFeatureMgr = pFeatureMgr;
        mShareApis = new HashMap<String, IFShareApi>();
        mShareApiNames = (HashMap<String, String>) mFeatureMgr.processEvent(MgrType.FeatureMgr, MgrEvent.OBTAIN_FEATURE_EXT_HASHMAP, pFeatureName);
    }

    /**
     * Description:分享详细分发
     *
     * @param pWebViewImpl
     * @param pActionName
     * @param pJsArgs
     * @return <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-27 下午4:17:16</pre>
     */
    public String execute(IWebview pWebViewImpl, String pActionName,
                          String[] pJsArgs) {
        if (pActionName.equals("getServices")) {
            JSUtil.execCallback(pWebViewImpl, pJsArgs[0], getServices(pWebViewImpl)
                    , 1, true, false);
        } else if (pActionName.equals("authorize")) {
            IFShareApi _shareApi = mShareApis.get(pJsArgs[1]);
            _shareApi.authorize(pWebViewImpl, pJsArgs[0], pJsArgs[2]);
        } else if (pActionName.equals("forbid")) {
            IFShareApi _shareApi = mShareApis.get(pJsArgs[0]);
            _shareApi.forbid(pWebViewImpl);
        } else if (pActionName.equals("send")) {
            IFShareApi _shareApi = mShareApis.get(pJsArgs[1]);
            _shareApi.send(pWebViewImpl, pJsArgs[0], pJsArgs[2]);
        } else if (pActionName.equals("sendWithSystem")) {
            sendWithSystem(pWebViewImpl, pJsArgs[0], pJsArgs[1]);
        } else if (pActionName.equals("create")) {
            ShareAuthorizeView _view = new ShareAuthorizeView(pWebViewImpl, pJsArgs[1]);
            if (pJsArgs[2] != null && pJsArgs[1].equals("false")) {
            } else {
                float scale = pWebViewImpl.getScale();
                int _left = (int) (Integer.parseInt(pJsArgs[3]) * scale);
                int _top = (int) (Integer.parseInt(pJsArgs[4]) * scale);
                int _width = (int) (Integer.parseInt(pJsArgs[5]) * scale);
                int _height = (int) (Integer.parseInt(pJsArgs[6]) * scale);
                pWebViewImpl.addFrameItem(_view, LayoutParamsUtil.createLayoutParams(_left, _top, _width, _height));
            }
            mAuthorizeViews.put(pJsArgs[0], _view);
        } else if (pActionName.equals("load")) {
            ShareAuthorizeView _view = mAuthorizeViews.get(pJsArgs[0]);
            _view.load(this, pJsArgs[1]);
        } else if (pActionName.equals("setVisible")) {
            ShareAuthorizeView _view = mAuthorizeViews.get(pJsArgs[0]);
            boolean b = Boolean.parseBoolean(pJsArgs[1]);
            if (b) {
                _view.setVisibility(View.VISIBLE);
            } else {
                _view.setVisibility(View.GONE);
            }
        }
        return null;
    }

    public String getShareClassName(String flag) {
        String className = null;
        if (PdrUtil.isEquals(flag, "sinaweibo")) {
            className = mShareApiNames.get("sina");//需要小写
        } else if (PdrUtil.isEquals(flag, "tencentweibo")) {
            className = mShareApiNames.get("tencent");//需要小写
        }
        return className;
    }

    private String getServices(IWebview pWebViewImpl) {
        StringBuffer _sb = new StringBuffer();
        _sb.append("[");
        if (mShareApiNames != null && !mShareApiNames.isEmpty()) {
            Iterator<String> iterator = mShareApiNames.keySet().iterator();
            String _key = null;
            String _name = null;
            IFShareApi _shareApiImpl = null;
            for (int i = 0; iterator.hasNext(); i++) {
                try {
                    _key = iterator.next();
                    _shareApiImpl = mShareApis.get(_key);
                    if (_shareApiImpl == null) {
                        _name = mShareApiNames.get(_key);
                        _shareApiImpl = (IFShareApi) Class.forName(_name).newInstance();
                        _shareApiImpl.initConfig();
                        mShareApis.put(_shareApiImpl.getId(), _shareApiImpl);
                    }

                    _sb.append(_shareApiImpl.getJsonObject(pWebViewImpl));
                    if (i != mShareApis.size()) {
                        _sb.append(",");
                    }
                } catch (NotFoundException e) {
                    Logger.e("ShareApiManager getServices " + _name + " is Not found!");
                } catch (Exception e) {
                    Logger.e("ShareApiManager getServices " + _name + " Exception =" + e);
                }
            }
        }
        _sb.append("]");
        return _sb.toString();
    }

    /**
     * 使用系统自身分享，成功失败不能确定，
     * 调起选择界面即触发成功回调，
     * 出现异常即触发失败回调
     * @param pWebViewImpl
     * @param pCallbackId
     * @param pShareMsg
     */
    public void sendWithSystem(final IWebview pWebViewImpl, String pCallbackId, String pShareMsg) {
        try {
            JSONObject _msg = new JSONObject(pShareMsg);
            String _content = _msg.optString("content");
            String _title = _msg.optString("title");
            JSONArray _pictures = _msg.optJSONArray("pictures");
            Intent intent;
            if (!PdrUtil.isEmpty(_pictures)) {
                ArrayList<Uri> localArrayList = new ArrayList<Uri>();
                for (int i = 0; i < _pictures.length(); i++) {
                    String pic = _pictures.optString(i);
                    pic = pWebViewImpl.obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), pic);
                    localArrayList.add(Uri.fromFile(new File(pic)));
                }
                intent = setSysShareIntent(_content, _title, localArrayList);

            } else {
                intent = setSysShareIntent(_content, _title, null);
            }
            pWebViewImpl.getActivity().startActivity(Intent.createChooser(intent, "系统分享"));
            JSUtil.execCallback(pWebViewImpl, pCallbackId, "", JSUtil.OK, false, false);
        } catch (Exception e) {
            String msg = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_UNKNOWN_ERROR, DOMException.MSG_UNKNOWN_ERROR);
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
        }
    }

    /**
     * 设置系统分享intent携带参数
     * @param _content
     * @param _title
     * @param localArrayList
     * @return
     */
    private Intent setSysShareIntent(String _content, String _title, ArrayList<Uri> localArrayList) {
        Intent intent= new Intent();

        if (!PdrUtil.isEmpty(_content)) {
            intent.putExtra(Intent.EXTRA_TEXT, _content);
        }
        if (!PdrUtil.isEmpty(_title)) {
            intent.putExtra(Intent.EXTRA_SUBJECT, _title);
        }
        if (!PdrUtil.isEmpty(localArrayList)) {
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("image/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, localArrayList);
        }else {
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
        }
        return intent;
    }
    /**
     * 释放资源
     */
    public void dispose(){
        if(null != mShareApis){
            for(Map.Entry<String, IFShareApi> entry:mShareApis.entrySet()){
                entry.getValue().dispose();
                mShareApis.remove(entry.getKey());
            }
        }
        //mShareApis = null;

        if(null != mAuthorizeViews){
            for(Map.Entry<String, ShareAuthorizeView> entry:mAuthorizeViews.entrySet()){
                entry.getValue().dispose();
                mAuthorizeViews.remove(entry.getKey());
            }
        }
        //mAuthorizeViews = null;

        if(null != mShareApiNames){
            mShareApiNames.clear();
        }
        //mShareApiNames = null;
    }

}
