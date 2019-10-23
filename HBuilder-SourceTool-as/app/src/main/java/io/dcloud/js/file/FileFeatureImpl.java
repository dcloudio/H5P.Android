package io.dcloud.js.file;

import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.io.DHFile;
import io.dcloud.common.adapter.io.UnicodeInputStream;
import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.IOUtil;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.Md5Utils;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.common.util.ThreadPool;

/**
 * <p>Description:file对象相应扩展</p>
 *
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @version 1.0
 * @Date 2013-1-16 下午5:25:47 created.
 *
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-16 下午5:25:47</pre>
 */
public class FileFeatureImpl implements IFeature {

    private static final int PRIVATE_WWW = 1;
    private static final int PRIVATE_DOCUMENTS = 2;
    private static final int PUBLIC_DOCUMENTS = 3;
    private static final int PUBLIC_DOWNLOADS = 4;
    private static final int PUBLIC_DEVICE_ROOT = 5;

    private static String PRIVATE_WWW_PATH = null;
    private static String PRIVATE_WWW_PATH_APP_MODE;
    private static String PRIVATE_DOCUMENTS_PATH;
    private static String PUBLIC_DOCUMENTS_PATH;
    private static String PUBLIC_DOWNLOADS_PATH;

    /**
     * ERROR CODE
     */
    private final static int NOT_FOUND_ERR = 1;
    private final static int SECURITY_ERR = 2;
    private final static int ABORT_ERR = 3;

    private final static int NOT_READABLE_ERR = 4;
    private final static int ENCODING_ERR = 5;
    private final static int NO_MODIFICATION_ALLOWED_ERR = 6;
    private final static int INVALID_STATE_ERR = 7;
    private final static int SYNTAX_ERR = 8;
    private final static int INVALID_MODIFICATION_ERR = 9;
    private final static int QUOTA_EXCEEDED_ERR = 10;
    private final static int TYPE_MISMATCH_ERR = 11;
    private final static int PATH_EXISTS_ERR = 12;
    private final static int ERROR_EXCEPTION = 13;

    private final static String[] whiteList = new String[]{"getAudioInfo","getFileInfo","getImageInfo","getVideoInfo"};

    @Override
    public String execute(final IWebview pWebViewImpl, String pActionName,
                          String[] pJsArgs) {
        String[] _arr = null;
        String pCallbackId = pJsArgs[0];
        if (pJsArgs.length > 1 && pJsArgs[1] != null) {
            try {
                if (Arrays.binarySearch(whiteList,pActionName)<0)
                _arr = JSUtil.jsonArrayToStringArr(new JSONArray(pJsArgs[1]));
            } catch (JSONException e) {
                e.printStackTrace();
                errorCallback(SYNTAX_ERR, pWebViewImpl, pCallbackId);
            }
        }
        IApp _app = pWebViewImpl.obtainFrameView().obtainApp();
        boolean runningAppStatus = _app.isOnAppRunningMode();
//		if(PRIVATE_WWW_PATH == null){//不应该一直使用app初始化出来的路径值，当多应用运行的时候会发生路径错乱
        PRIVATE_WWW_PATH = _app.getPathByType(IApp.ABS_PRIVATE_WWW_DIR);
        PRIVATE_WWW_PATH_APP_MODE = _app.getPathByType(IApp.ABS_PRIVATE_WWW_DIR_APP_MODE);
        PRIVATE_DOCUMENTS_PATH = _app.getPathByType(IApp.ABS_PRIVATE_DOC_DIR);
        PUBLIC_DOCUMENTS_PATH = _app.getPathByType(IApp.ABS_PUBLIC_DOCUMENTS_DIR);
        PUBLIC_DOWNLOADS_PATH = _app.getPathByType(IApp.ABS_PUBLIC_DOWNLOADS_DIR);
//		}
        //moveTo
        if (pActionName.equals("moveTo")) {
            String _fullpath = _app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), _arr[1] + _arr[2]);
            boolean dontContinue = _app.checkPrivateDir(_arr[0]) || _app.checkPrivateDir(_fullpath);
            if (dontContinue) {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            } else {
                File srcFile = new File(_app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), _arr[0]));
                boolean srcIsDir = srcFile.isDirectory();
                boolean _suc = false;
                File destFile = new File(_fullpath);
                if (!destFile.exists() && !dontContinue) {
//					if(srcIsDir){//moveTo一个目录时使用renameTo解决
                    File parentFile = destFile.getParentFile();
                    if (!parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    _suc = srcFile.renameTo(destFile);
//					}else{
//						_suc = 1 ==  DHFile.copyFile(_app.convert2AbsFullPath(_arr[0]), _fullpath);
//					}
                }
                if (_suc) {
                    JSONObject _json = JsFile.getJsFileEntry(_arr[2], _fullpath, _app.convert2RelPath(_fullpath), srcIsDir);
                    JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.OK, false);
                } else {
                    errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
                }
            }
        }
        //copyTo
        else if (pActionName.equals("copyTo")) {
            String _fullpath = null;
            String _srcPath = null;
            if (_arr[1] != null) {
                _srcPath = _arr[1] + (_arr[1].endsWith(File.separator) ? "" : File.separator);
            } else {
                _srcPath = _arr[1];
            }
            _fullpath = _srcPath + _arr[2];
            boolean doContinue = !_app.checkPrivateDir(_fullpath);
            boolean copySuc = false;
            try {
                if (doContinue) {
                    if (!DHFile.isExist(_fullpath)) {
                        //需要判断源文件的位置
                        if (DHFile.isExist(_arr[0])) {//源文件位置在sdcard
                            if (DHFile.copyFile(_arr[0], _fullpath) == 1) {
                                copySuc = true;
                            }
                        } else if (DHFile.copyAssetsFile(_arr[0], _fullpath)) {//assets运行
                            copySuc = true;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (copySuc) {
                JSONObject _json = JsFile.getJsFileEntry(_arr[2], _fullpath, _app.convert2RelPath(_fullpath), new File(_fullpath).isDirectory());
                JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.OK, false);
            } else {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }
        }
        //remove
        else if (pActionName.equals("remove")) {
            String _path = _arr[0];
            boolean dontContinue = _app.checkPrivateDir(_path);
            if (dontContinue) {
                //WWW为应用目录 用户不可写
                errorCallback(NOT_READABLE_ERR, pWebViewImpl, pCallbackId);
                return null;
            }
            File _delete = new File(_path);
            if (_delete.delete()) {
                JSUtil.execCallback(pWebViewImpl, pCallbackId, "", JSUtil.OK, false);
            } else {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }
        }
        //getMetadata
        else if (pActionName.equals("getMetadata")) {
            boolean dontContinue = _app.checkPrivateDir(_arr[0]);
            boolean _suc = true;
            JSONObject _fileMetedata = null;
            if (dontContinue && runningAppStatus) {//操作_www目录且处于app运行时，失败
                _suc = false;
            } else {
                try {
                    boolean b = false;
                    if (_arr.length == 2 && _arr[1] != null && _arr[1].equalsIgnoreCase("true")) {
                        b = Boolean.parseBoolean(_arr[1]);
                    }
                    _fileMetedata = JsFile.getMetadata(_arr[0], b);
                } catch (Exception e) {
                    e.printStackTrace();
                    _suc = false;
                }
            }
            if (_suc) {
                JSUtil.execCallback(pWebViewImpl, pCallbackId, _fileMetedata, JSUtil.OK, false);
            } else {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }
        }
        //getFileMetadata
        else if (pActionName.equals("getFileMetadata")) {
            boolean dontContinue = _app.checkPrivateDir(_arr[0]);
            boolean _suc = true;
            JSONObject _fileMetedata = null;
            if (dontContinue && runningAppStatus) {//操作_www目录且处于app运行时，失败
                _suc = false;
            } else {
                try {
                    _fileMetedata = JsFile.getFileMetadata(_arr[0], getType(_arr[0]));
                } catch (Exception e) {
                    e.printStackTrace();
                    _suc = false;
                }
            }
            if (_suc) {
                JSUtil.execCallback(pWebViewImpl, pCallbackId, _fileMetedata, JSUtil.OK, false);
            } else {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }

        }
        //readEntries
        else if (pActionName.equals("readEntries")) {
            boolean dontContinue = _app.checkPrivateDir(_arr[0]);
            boolean _suc = true;
            JSONArray _json = null;
            if (dontContinue && runningAppStatus) {//操作_www目录且处于app运行时，失败
                _suc = false;
            } else {
                try {
                    _json = JsFile.readEntries(_arr[0], _app.convert2RelPath(_arr[0]));
                } catch (Exception e) {
                    e.printStackTrace();
                    _suc = false;
                }
            }
            if (_suc) {
                JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.OK, false);
            } else {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);

            }
        }
        //readAsText
        else if (pActionName.equals("readAsText")) {
            boolean dontContinue = _app.checkPrivateDir(_arr[0]);
            boolean _suc = true;
            String _text = null;
            if (dontContinue && runningAppStatus) {//操作_www目录且处于app运行时，失败
                _suc = false;
            } else {
                try {
                    InputStream is = DHFile.getInputStream(DHFile.createFileHandler(_arr[0]));
                    try {
                        is = new UnicodeInputStream(is, Charset.defaultCharset().name());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ByteArrayOutputStream baos = null;
                    baos = new ByteArrayOutputStream();
                    while (true) {
                        byte[] buffer = new byte[DHFile.BUF_SIZE];
                        int ret = is.read(buffer);
                        if (ret == -1) {
                            break;
                        } else {
                            baos.write(buffer, 0, ret);
                        }
                    }
                    _text = PdrUtil.isEmpty(_arr[1]) ? baos.toString() : baos.toString(_arr[1]);
                    _text = JSONUtil.toJSONableString(_text);//对text进行封装
                } catch (Exception e) {
                    e.printStackTrace();
                    _suc = false;
                }
            }
            if (_suc) {//由于已经对text进行封装，此时认为是json
                JSUtil.execCallback(pWebViewImpl, pCallbackId, _text, JSUtil.OK, true, false);
            } else {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }
        }
        //write
        else if (pActionName.equals("write")) {
            try {
                boolean dontContinue = _app.checkPrivateDir(_arr[0]);
                int offset = Integer.valueOf(_arr[2]);
                if (_arr[1] == null || dontContinue) {
                    errorCallback(NOT_READABLE_ERR, pWebViewImpl, pCallbackId);
                    return null;
                }
                byte[] pData = _arr[1].getBytes();
                DHFile.writeFile(pData, offset, _arr[0]);
                JSUtil.execCallback(pWebViewImpl, pCallbackId, pData.length, JSUtil.OK, false);
            } catch (Exception e) {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }
        }
        //requestFileSystem\
        else if (pActionName.equals("requestFileSystem")) {
            try {
                int type = Integer.parseInt(_arr[0]);
                JSONObject _json = null;
                switch (type) {
                    case PRIVATE_WWW:
                        _json = JsFile.getJsFileSystem("PRIVATE_WWW", PRIVATE_WWW, BaseInfo.REAL_PRIVATE_WWW_DIR, PRIVATE_WWW_PATH, BaseInfo.REL_PRIVATE_WWW_DIR);
                        break;
                    case PRIVATE_DOCUMENTS:
                        _json = JsFile.getJsFileSystem("PRIVATE_DOCUMENTS", PRIVATE_DOCUMENTS, BaseInfo.REAL_PRIVATE_DOC_DIR, PRIVATE_DOCUMENTS_PATH, BaseInfo.REL_PRIVATE_DOC_DIR);
                        break;
                    case PUBLIC_DOCUMENTS:
                        _json = JsFile.getJsFileSystem("PUBLIC_DOCUMENTS", PUBLIC_DOCUMENTS, BaseInfo.REAL_PUBLIC_DOCUMENTS_DIR, PUBLIC_DOCUMENTS_PATH, BaseInfo.REL_PUBLIC_DOCUMENTS_DIR);
                        break;
                    case PUBLIC_DOWNLOADS:
                        _json = JsFile.getJsFileSystem("PUBLIC_DOWNLOADS", PUBLIC_DOWNLOADS, BaseInfo.REAL_PUBLIC_DOWNLOADS_DIR, PUBLIC_DOWNLOADS_PATH, BaseInfo.REL_PUBLIC_DOWNLOADS_DIR);
                        break;
                    default:
                        _json = JsFile.getJsFileSystem("PUBLIC_DEVICE_ROOT", PUBLIC_DEVICE_ROOT, DeviceInfo.sDeviceRootDir, DeviceInfo.sDeviceRootDir, DeviceInfo.sDeviceRootDir);
                        break;
                }
                JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.OK, false);
            } catch (Exception e) {
                e.printStackTrace();
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }
        }
        //getFile
        else if (pActionName.equals("getFile")) {
            String _fullPath = _app.convert2AbsFullPath(_arr[0], _arr[1]);
            File _file = new File(_fullPath);
            try {
                JSONObject _create = new JSONObject(_arr[2]);
                String _fileName = _arr[1];
                boolean _ret = _create.optBoolean("create");
                boolean _excl = _create.optBoolean("exclusive");
                byte b = -1;
                if (!_file.exists()) {
                    if (_ret) {
                        b = DHFile.createNewFile(_file);
                        _fileName = _file.getName();
                        if ((b == -1) || (b == -2 && _excl)) {
                            throw new RuntimeException();
                        }
                    } else {
                        throw new FileNotFoundException(_fullPath);
                    }
                } else if (_excl) {
                    errorCallback(PATH_EXISTS_ERR, pWebViewImpl, pCallbackId);
                    return null;
                } else {
                    _fileName = _file.getName();
                }
                JSONObject _json = JsFile.getJsFileEntry(_fileName, _fullPath, _app.convert2RelPath(_fullPath), false);
                JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.OK, false);
            } catch (Exception e) {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }
        }
        //getParent
        else if (pActionName.equals("getParent")) {
            boolean _suc = false;
            String _path = _arr[0];
            if (_path != null && isRootDir(_path)) {
                errorCallback(NOT_READABLE_ERR, pWebViewImpl, pCallbackId);
                return null;
            }
            File _file = new File(_path);
            if (_file.exists()) {
                String parentPath = _file.getParent();
                JSONObject json = JsFile.getJsFileEntry(_file.getParentFile().getName(), parentPath, _app.convert2RelPath(parentPath), true);
                _suc = true;
                JSUtil.execCallback(pWebViewImpl, pCallbackId, json, JSUtil.OK, false);
            }
            if (!_suc) {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }
        }
        //getDirectory
        else if (pActionName.equals("getDirectory")) {
            if (_arr[0]!=null && !_arr[0].endsWith("/")) {
                _arr[0] = _arr[0]+ "/";
            }
            String _fullPath = _app.convert2AbsFullPath(_arr[0], _arr[1]);
            String fullPath = _fullPath;
            if (_arr[0] != null && _arr[1] != null && _arr[1].indexOf("../") != -1 && isRootDir(_arr[0])) {
                errorCallback(NOT_READABLE_ERR, pWebViewImpl, pCallbackId);
                return null;
            }
            if (!_fullPath.endsWith("/")) {
                fullPath = _fullPath + "/";
            }
            File _file = new File(fullPath);
            try {
                JSONObject _create = new JSONObject(_arr[2]);
                boolean _ret = _create.optBoolean("create");
                boolean _excl = _create.optBoolean("exclusive");
                byte b = -1;
                if (!_file.exists()) {
                    if (_ret) {
                        b = DHFile.createNewFile(fullPath);
                        if ((b == -1) || (b == -2 && _excl)) {
                            throw new RuntimeException();
                        }
                    } else {
                        errorCallback(PATH_EXISTS_ERR, pWebViewImpl, pCallbackId);
                        return null;
                    }
                } else if (_excl) {
                    throw new FileNotFoundException(_fullPath);
                }
                JSONObject _json = JsFile.getJsFileEntry(_arr[1], fullPath, _app.convert2RelPath(fullPath), true);
                JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.OK, false);
            } catch (Exception e) {
                Logger.d("Not Found " + fullPath);
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }
        }
        //removeRecursively
        else if (pActionName.equals("removeRecursively")) {
            if (JSUtil.checkOperateDirErrorAndCallback(pWebViewImpl, pCallbackId, _arr[0])) {
                return null;
            }
            File fileHandler = new File(_arr[0]);
            if (fileHandler.isDirectory()) {//判断是否为目录，为目录时先进性重命名，然后删除
                try {
                    String temp = String.valueOf(System.currentTimeMillis());
                    DHFile.rename(fileHandler.getAbsolutePath() + "/", temp);
                    fileHandler = new File(fileHandler.getParent() + "/" + temp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (DHFile.delete(fileHandler)) {
                JSUtil.execCallback(pWebViewImpl, pCallbackId, "", JSUtil.OK, false);
            } else {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }
        }
        //readAsDataURL
        else if (pActionName.equals("readAsDataURL")) {
            boolean dontContinue = _app.checkPrivateDir(_arr[0]);
            boolean _suc = true;
            String _ret = null;
            if (dontContinue && runningAppStatus) {//操作_www目录且处于app运行时，失败
                _suc = false;
            } else {
                byte[] _byts = DHFile.readAll(_arr[0]);
                String base64Text = null;
                try {
                    String type = PdrUtil.getMimeType(_arr[0]);
                    if (DeviceInfo.sDeviceSdkVer >= 8) {
                        base64Text = Base64.encodeToString(_byts, Base64.NO_WRAP);
                    } else {
                        base64Text = io.dcloud.common.util.Base64.encode(_byts);
                    }
                    _ret = String.format("data:%s;base64,%s", type, base64Text);
                } catch (Exception e) {
                    e.printStackTrace();
                    _suc = false;
                }
            }
            if (_suc) {
                JSUtil.execCallback(pWebViewImpl, pCallbackId, _ret, JSUtil.OK, false);
            } else {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }
        }
        //truncate
        else if (pActionName.equals("truncate")) {
            FileInputStream _fis = null;
            FileOutputStream _fos = null;
            try {
                File srcFile = new File(_arr[0]);
                _fis = new FileInputStream(srcFile);
                String t_fileName = System.currentTimeMillis() + srcFile.getName();
                File tFile = new File(srcFile.getParent() + "/" + t_fileName);
                _fos = new FileOutputStream(tFile);
                int _size = Integer.parseInt(_arr[1]);
                int _startPos = Integer.parseInt(_arr[2]);//起始位置
                int bufSize = Math.min(_startPos + _size, 10240);
                byte[] by = new byte[bufSize];
                int sum = 0;
                int len = -1;
                while ((len = _fis.read(by)) != -1) {
                    int count = Math.min(_size - sum, len - _startPos);//保证起始位置正确
                    if (count > 0) {
                        _fos.write(by, _startPos, count);
                        sum += count;
                    }
                    if (sum >= _size) break;
                    if (_startPos > 0) {
                        _startPos -= len;
                    } else {
                        _startPos = 0;
                    }
                }
                srcFile.delete();
                tFile.renameTo(srcFile);
                JSUtil.execCallback(pWebViewImpl, pCallbackId, _arr[1], JSUtil.OK, true, false);
            } catch (Exception e) {
                e.printStackTrace();
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            } finally {
                IOUtil.close(_fis);
                IOUtil.close(_fos);
            }
        } else if (pActionName.equals("convertLocalFileSystemURL")) {
            String path = pJsArgs[0];
            return JSUtil.wrapJsVar(_app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), path), true);
        } else if (pActionName.equals("convertAbsoluteFileSystem")) {
            String path = pJsArgs[0];
            return JSUtil.wrapJsVar(_app.convert2RelPath(path), true);
        } else if (pActionName.equals("resolveLocalFileSystemURL")) {
            try {
//				1. plus路径，以”_doc“、”_www“等开头；
//				2. plus网络路径，以”http://localhost:13131/"开头的路径。
//				3. 绝对路径，以”file://"开头后面跟系统绝对路径；
//				4. 相对路径，以“/”开头表示相对于www根目录，“./"表示相对于当前页面的所属的目录。

                boolean isRootDir = isRootDir(_arr[0]);
                boolean isAppRunMode3WRootDir = false;//是否是操作app运行模式下的www目录
//				if(isRootDir){
                isAppRunMode3WRootDir = _app.isOnAppRunningMode() && _app.checkPrivateDir(_arr[0]);
//				}
                String _path = _app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), _arr[0]);
                if (isAppRunMode3WRootDir) {
                    JSONObject _obj = getFileSystem(_path);
                    JSONObject _json = JsFile.getJsFileEntryAndFS(BaseInfo.REL_PRIVATE_WWW_DIR, _path, true,
                            BaseInfo.REL_PRIVATE_WWW_DIR, _obj.optString("fsName"), _obj.optInt("type"), _obj.optJSONObject("fsRoot"));
                    JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.OK, false);
                } else {
                    File file = new File(_path);
                    boolean b = file.exists();
                    if (!b && isRootDir) {//操作的是，四个根目录时，如果不存在需要创建（除app运行模式下的www目录）
                        try {
                            File parentFile = file.getParentFile();
                            if (!parentFile.exists()) {
                                parentFile.mkdirs();
                            }
//						b = file.createNewFile();//此方法不能创建目录成功，使用DHFile.createNewFile
                            b = 1 == DHFile.createNewFile(DHFile.createFileHandler(_path));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (b) {
                        JSONObject _obj = getFileSystem(_path);
                        JSONObject _json = JsFile.getJsFileEntryAndFS(file.getName(), _path, file.isDirectory(),
                                _arr[0], _obj.optString("fsName"), _obj.optInt("type"), _obj.optJSONObject("fsRoot"));
                        JSUtil.execCallback(pWebViewImpl, pCallbackId, _json, JSUtil.OK, false);
                    } else {
                        errorCallback(PATH_EXISTS_ERR, pWebViewImpl, pCallbackId);
                    }

                }
            } catch (JSONException e) {
                errorCallback(QUOTA_EXCEEDED_ERR, pWebViewImpl, pCallbackId);
            }

        } else if (pActionName.equals("getImageInfo")) {
            final String callbackId = pJsArgs[0];
            final String src = pJsArgs[1];
            if (!PdrUtil.isEmpty(src)) {
                if (PdrUtil.isNetPath(src)) {
                    try {
                        File downFile = Glide.with(pWebViewImpl.getContext()).downloadOnly().load(src).submit().get();
                        getExif(downFile.getAbsolutePath(), pWebViewImpl, callbackId);
                    } catch (Exception e) {
                        JSUtil.execCallback(pWebViewImpl, callbackId, DOMException.toJSON(ERROR_EXCEPTION,"Failed to load resource"), JSUtil.ERROR, true,false);
                    }
                } else {
                    String path = pWebViewImpl.obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), src);
                    if (!new File(path).exists()) {
                        errorCallback(PATH_EXISTS_ERR, pWebViewImpl, pCallbackId);
                    } else {
                        getExif(path, pWebViewImpl, callbackId);
                    }
                }
            }
        } else if (pActionName.equals("getFileInfo")) {
            final String callbackId = pJsArgs[0];
            String path = "";
            String digestAlgorithm = "MD5";
            try {
                JSONObject param = new JSONObject(pJsArgs[1]);
                path = param.optString("filePath");
                digestAlgorithm = param.optString("digestAlgorithm","MD5");
            } catch (JSONException ignored) {
            }
            if (!PdrUtil.isEmpty(path) && !PdrUtil.isNetPath(path)) {
                final String filePath = pWebViewImpl.obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), path);
                final File file = new File(filePath);
                if (!file.exists()) {
                    String message = StringUtil.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_FILE_NOT_EXIST,DOMException.MSG_FILE_NOT_EXIST);
                    JSUtil.execCallback(pWebViewImpl,callbackId,message,JSUtil.ERROR,true,false);
                } else {
                    final String finalDigestAlgorithm = digestAlgorithm;
                    ThreadPool.self().addThreadTask(new Runnable() {
                        @Override
                        public void run() {
                            String md5 = Md5Utils.md5(file,finalDigestAlgorithm);
                            JSONObject message = new JSONObject();
                            try {
                                message.put("size", file.length());
                                if (md5 != null)
                                    message.put("digest",md5.toUpperCase(Locale.US));
                            } catch (JSONException ignored) {
                            }
                            JSUtil.execCallback(pWebViewImpl,callbackId,message.toString(),JSUtil.OK,true,false);
                        }
                    });
                }
            }
        } else if (pActionName.equals("getAudioInfo") || pActionName.equals("getVideoInfo")) {
            String callbackId = pJsArgs[0];
            String path = "";
            try {
                JSONObject param = new JSONObject(pJsArgs[1]);
                path = param.optString("filePath");
            } catch (JSONException ignored) {
            }
            getMediaInfo(path,callbackId,pWebViewImpl,pActionName);
        }
        return null;
    }

    private void getMediaInfo(String path,String callid,IWebview pwebview,String action){
        if (!PdrUtil.isEmpty(path) && !PdrUtil.isNetPath(path)) {
            String filePath = pwebview.obtainApp().convert2AbsFullPath(pwebview.obtainFullUrl(), path);
            if (!new File(filePath).exists()) {
                String message = StringUtil.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_FILE_NOT_EXIST,DOMException.MSG_FILE_NOT_EXIST);
                JSUtil.execCallback(pwebview,callid,message,JSUtil.ERROR,true,false);
            } else {
                try {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(filePath);
                    String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    JSONObject message = new JSONObject();
                    if (action.equals("getVideoInfo")) {
                        if (width != null && height != null) {
                            message.put("resolution", width + "*" + height);
                        }
                        {
                            if (width != null) {
                                message.put("width",width);
                            }
                            if (height != null) {
                                message.put("height",height);
                            }
                        }
                        message.put("size",new File(filePath).length());
                    }
                    message.put("duration", StringUtil.format("%.2f",(Float.parseFloat(duration)/1000)));
                    JSUtil.execCallback(pwebview,callid,message.toString(),JSUtil.OK,true,false);
                }catch (Exception e) {
                    JSONObject message = new JSONObject();
                    try {
                        message.put("code",ERROR_EXCEPTION);
                        message.put("message",e.getMessage());
                    } catch (JSONException ignored) {
                    }
                    JSUtil.execCallback(pwebview,callid,message.toString(),JSUtil.ERROR,true,false);
                }
            }
        }
    }
    //	/**
//	 * 纠正路径
//	 * @param mainPath
//	 * @param subPath
//	 * @return
//	 */
//	private String checkFilePath(String mainPath,String subPath){
//		if(!mainPath.endsWith("/")){
//			mainPath += "/";
//		}
//		if(subPath.startsWith("/")){
//			subPath = subPath.substring(1);
//		}
//		return mainPath + subPath;
//	}
    private boolean isRootDir(String path) {
        return path.endsWith(BaseInfo.REL_PRIVATE_WWW_DIR) || path.endsWith(BaseInfo.REL_PUBLIC_DOCUMENTS_DIR)
                || path.endsWith(BaseInfo.REL_PUBLIC_DOWNLOADS_DIR) || path.endsWith(BaseInfo.REL_PRIVATE_DOC_DIR)
                || path.endsWith(BaseInfo.REL_PRIVATE_WWW_DIR + "/") || path.endsWith(BaseInfo.REL_PUBLIC_DOCUMENTS_DIR + "/")
                || path.endsWith(BaseInfo.REL_PUBLIC_DOWNLOADS_DIR + "/") || path.endsWith(BaseInfo.REL_PRIVATE_DOC_DIR + "/")
                ;
    }

    private JSONObject getFileSystem(String pFullpath) throws JSONException {
        JSONObject _json = new JSONObject();
        if (pFullpath.startsWith(PRIVATE_WWW_PATH)) {
            _json.put("type", PRIVATE_WWW);
            _json.put("fsName", "PRIVATE_WWW");
            _json.put("fsRoot", JsFile.getJsFileEntry("PRIVATE_WWW", PRIVATE_WWW_PATH, getRemoteURL(pFullpath), true));
        } else if (pFullpath.startsWith(PRIVATE_DOCUMENTS_PATH)) {
            _json.put("type", PRIVATE_DOCUMENTS);
            _json.put("fsName", "PRIVATE_DOCUMENTS");
            _json.put("fsRoot", JsFile.getJsFileEntry("PRIVATE_DOCUMENTS", PRIVATE_DOCUMENTS_PATH, getRemoteURL(pFullpath), true));
        } else if (pFullpath.startsWith(PUBLIC_DOCUMENTS_PATH)) {
            _json.put("type", PUBLIC_DOCUMENTS);
            _json.put("fsName", "PUBLIC_DOCUMENTS");
            _json.put("fsRoot", JsFile.getJsFileEntry("PUBLIC_DOCUMENTS", PUBLIC_DOCUMENTS_PATH, getRemoteURL(pFullpath), true));
        } else if (pFullpath.startsWith(PUBLIC_DOWNLOADS_PATH)) {
            _json.put("type", PUBLIC_DOWNLOADS);
            _json.put("fsName", "PUBLIC_DOWNLOADS");
            _json.put("fsRoot", JsFile.getJsFileEntry("PUBLIC_DOWNLOADS", PUBLIC_DOWNLOADS_PATH, getRemoteURL(pFullpath), true));
        } else if (pFullpath.startsWith(PRIVATE_WWW_PATH_APP_MODE)) {
            _json.put("type", PRIVATE_WWW);
            _json.put("fsName", "PRIVATE_WWW");
            _json.put("fsRoot", JsFile.getJsFileEntry("PRIVATE_WWW", PRIVATE_WWW_PATH_APP_MODE, getRemoteURL(pFullpath), true));
        } else if (PdrUtil.isDeviceRootDir(pFullpath)) {
            _json.put("type", PUBLIC_DEVICE_ROOT);
            _json.put("fsName", "PUBLIC_DEVICE_ROOT");
            _json.put("fsRoot", JsFile.getJsFileEntry("PUBLIC_DEVICE_ROOT", DeviceInfo.sDeviceRootDir, getRemoteURL(pFullpath), true));
        }
        return _json;
    }

    private String getType(String pFullpath) {
        String _type = getMimeType(pFullpath);
        boolean isC = false;
        if (PdrUtil.isEmpty(_type)) {
            _type = String.valueOf(-1);
            isC = true;
        }
        if (isC) {
            if (pFullpath.startsWith(PRIVATE_WWW_PATH)) {
                _type = String.valueOf(PRIVATE_WWW);
            } else if (pFullpath.startsWith(PRIVATE_DOCUMENTS_PATH)) {
                _type = String.valueOf(PRIVATE_DOCUMENTS);
            } else if (pFullpath.startsWith(PUBLIC_DOCUMENTS_PATH)) {
                _type = String.valueOf(PUBLIC_DOCUMENTS);
            } else if (pFullpath.startsWith(PUBLIC_DOWNLOADS_PATH)) {
                _type = String.valueOf(PUBLIC_DOWNLOADS);
            }
        }
        return _type;
    }

    private String getRemoteURL(String pFullpath) {
        String _url = null;
        if (pFullpath.startsWith(PRIVATE_WWW_PATH)) {
            _url = BaseInfo.REL_PRIVATE_WWW_DIR + "/" + pFullpath.substring(PRIVATE_WWW_PATH.length(), pFullpath.length());
        } else if (pFullpath.startsWith(PRIVATE_DOCUMENTS_PATH)) {
            _url = BaseInfo.REL_PRIVATE_DOC_DIR + "/" + pFullpath.substring(PRIVATE_DOCUMENTS_PATH.length(), pFullpath.length());
        } else if (pFullpath.startsWith(PUBLIC_DOCUMENTS_PATH)) {
            _url = BaseInfo.REL_PUBLIC_DOCUMENTS_DIR + "/" + pFullpath.substring(PUBLIC_DOCUMENTS_PATH.length(), pFullpath.length());
        } else if (pFullpath.startsWith(PUBLIC_DOWNLOADS_PATH)) {
            _url = BaseInfo.REL_PUBLIC_DOWNLOADS_DIR + "/" + pFullpath.substring(PUBLIC_DOWNLOADS_PATH.length(), pFullpath.length());
        }
        return _url;
    }

    /**
     * Description:失败回调
     *
     * @param pWebViewImpl
     * @param pCallbackId  <pre><p>ModifiedLog:</p>
     *                     Log ID: 1.0 (Log编号 依次递增)
     *                     Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-23 下午4:35:08</pre>
     */
    private void errorCallback(int pErrorCode, IWebview pWebViewImpl, String pCallbackId) {
        JSUtil.execCallback(pWebViewImpl, pCallbackId, errorObj(pErrorCode), JSUtil.ERROR, true, false);
    }

    private String errorObj(int pErrorCode) {
        String _json = "{code:%d,message:'%s'}";
        switch (pErrorCode) {
            case NOT_FOUND_ERR:
                _json = StringUtil.format(_json, pErrorCode, "文件没有发现");
                break;
            case SECURITY_ERR:
                _json = StringUtil.format(_json, pErrorCode, "没有获得授权");
                break;
            case ABORT_ERR:
                _json = StringUtil.format(_json, pErrorCode, "取消");
                break;
            case NOT_READABLE_ERR:
                _json = StringUtil.format(_json, pErrorCode, "不允许读");
                break;
            case ENCODING_ERR:
                _json = StringUtil.format(_json, pErrorCode, "编码错误");
                break;
            case NO_MODIFICATION_ALLOWED_ERR:
                _json = StringUtil.format(_json, pErrorCode, "不允许修改");
                break;
            case INVALID_STATE_ERR:
                _json = StringUtil.format(_json, pErrorCode, "无效的状态");
                break;
            case SYNTAX_ERR:
                _json = StringUtil.format(_json, pErrorCode, "语法错误");
                break;
            case INVALID_MODIFICATION_ERR:
                _json = StringUtil.format(_json, pErrorCode, "无效的修改");
                break;
            case QUOTA_EXCEEDED_ERR:
                _json = StringUtil.format(_json, pErrorCode, "执行出错");
                break;
            case TYPE_MISMATCH_ERR:
                _json = StringUtil.format(_json, pErrorCode, "类型不匹配");
                break;
            case PATH_EXISTS_ERR:
                _json = StringUtil.format(_json, pErrorCode, "路径不存在");
                break;
            default:
                _json = StringUtil.format(_json, pErrorCode, "未知错误");
                break;
        }
        return _json;
    }

    @Override
    public void init(AbsMgr pFeatureMgr, String pFeatureName) {
    }

    @Override
    public void dispose(String pAppid) {
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    private void getExif(String src, IWebview pwebview, String callbackId) {
        try {
            ExifInterface exifInterface = new ExifInterface(src);
            String orientation = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
            BitmapFactory.Options options = new BitmapFactory.Options();
            BitmapFactory.decodeFile(src,options);
            int width = options.outWidth;
            int height = options.outHeight;
            String mimeType = options.outMimeType;
            if (!PdrUtil.isEmpty(mimeType) && mimeType.contains("/")) {
                String[] types = mimeType.split("/");
                mimeType = types[types.length-1];
            }
            String orientationStr;
            switch (orientation) {
                default:
                case "0":
                case "1":
                    orientationStr = "up";
                    break;
                case "2":
                    orientationStr = "up-mirrored";
                    break;
                case "3":
                    orientationStr = "down";
                    break;
                case "4":
                    orientationStr = "down-mirrored";
                    break;
                case "5":
                    orientationStr = "left-mirrored";
                    break;
                case "6":
                    orientationStr = "right";
                    break;
                case "7":
                    orientationStr = "right-mirrored";
                    break;
                case "8":
                    orientationStr = "left";
                    break;
            }

            JSONObject object = new JSONObject();
            object.put("path", "file://"+src);
            object.put("width", width);
            object.put("height", height);
            object.put("orientation", orientationStr);
            object.put("type", mimeType);
            JSUtil.execCallback(pwebview, callbackId, object, JSUtil.OK, false);
        } catch (IOException e) {
            JSUtil.execCallback(pwebview, callbackId, StringUtil.format("{code:%d,message:'%s'}",ERROR_EXCEPTION,e.getMessage()), JSUtil.ERROR, true, false);
        } catch (JSONException e) {
            JSUtil.execCallback(pwebview, callbackId, StringUtil.format("{code:%d,message:'%s'}",ERROR_EXCEPTION,e.getMessage()), JSUtil.ERROR, true, false);
        }
    }
}
