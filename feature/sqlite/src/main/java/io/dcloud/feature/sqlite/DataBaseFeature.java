package io.dcloud.feature.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.adapter.io.DHFile;
import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;

public class DataBaseFeature extends StandardFeature {
    public HashMap<String, SQLiteDatabase> map = new HashMap<String, SQLiteDatabase>();
    private String resultMessage = "{'code':%d,'message':\"%s\"}";

    public void openDatabase(IWebview pWebview, JSONArray array) {
        String callBackID = array.optString(0);
        String name = array.optString(1);
        String path = array.optString(2);
        if (PdrUtil.isEmpty(name) || PdrUtil.isEmpty(path)) {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1404, DOMException.toString("parameter can't be null")), JSUtil.ERROR, true, false);
            return;
        }
        int dbFlag;
        if (path.startsWith(BaseInfo.REL_PRIVATE_WWW_DIR)) {
            dbFlag = SQLiteDatabase.OPEN_READONLY;
        } else if (PdrUtil.isDeviceRootDir(path)) {
            dbFlag = SQLiteDatabase.CREATE_IF_NECESSARY;
        } else if (path.startsWith(BaseInfo.REL_PRIVATE_DOC_DIR) || path.startsWith(BaseInfo.REL_PUBLIC_DOCUMENTS_DIR) || path.startsWith(BaseInfo.REL_PUBLIC_DOWNLOADS_DIR)) {
            dbFlag = SQLiteDatabase.CREATE_IF_NECESSARY;
        } else {
            dbFlag = SQLiteDatabase.OPEN_READONLY;
        }
        path = pWebview.obtainApp().convert2AbsFullPath(pWebview.obtainFullUrl(), path);
        String toPath = path;
        if (!PdrUtil.isDeviceRootDir(path)) {
            toPath = DeviceInfo.sBaseFsRootPath + path;
            if (!new File(toPath).exists()) {
                DHFile.copyAssetsFile(path, toPath);
            }
            toPath = pWebview.obtainApp().convert2AbsFullPath(pWebview.obtainFullUrl(), toPath);
            dbFlag = SQLiteDatabase.OPEN_READONLY;
        }
        if (!new File(toPath).exists() && dbFlag == SQLiteDatabase.OPEN_READONLY) {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1403, DOMException.toString("Cannot create file private directory,such as:\\'www\\'")), JSUtil.ERROR, true, false);
            return;
        } else {
            File file = new File(toPath);
            if (!file.exists()) {
                if (!file.getParentFile().exists())
                    file.getParentFile().mkdirs();
                try {
                    file.createNewFile();
                } catch (IOException ignored) {
                }
            }
        }
        if (map.containsKey(name)) {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1402, DOMException.toString("Same Name Already Open")), JSUtil.ERROR, true, false);
            return;
        }
        SQLiteDatabase database;
        try {
            database = SQLiteDatabase.openDatabase(toPath, null, dbFlag, null);
        } catch (Exception e) {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1404, DOMException.toString(PdrUtil.makeQueryStringAllRegExp(e.toString()))), JSUtil.ERROR, true, false);
            return;
        }
        map.put(name, database);
        JSUtil.execCallback(pWebview, callBackID, "{}", JSUtil.OK, true, false);
    }

    public String isOpenDatabase(IWebview pwebview, JSONArray array) {
        JSONObject param = array.optJSONObject(0);
        if (PdrUtil.isEmpty(param)) {
            return JSUtil.wrapJsVar("undefined", false);
        }
        String dbName = param.optString("name");
        String dbPath = param.optString("path");
        if (PdrUtil.isEmpty(dbName) || PdrUtil.isEmpty(dbPath)) {
            return JSUtil.wrapJsVar("undefined", false);
        }
        for (String key : map.keySet()) {
            SQLiteDatabase db = map.get(key);
            String realPath = pwebview.obtainApp().convert2AbsFullPath(pwebview.obtainFullUrl(), dbPath);
            if (!PdrUtil.isDeviceRootDir(realPath)) {
                realPath = DeviceInfo.sBaseFsRootPath + realPath;
            }
            if (realPath.equalsIgnoreCase(db.getPath())) {
                return JSUtil.wrapJsVar(true);
            }
        }
        if (map.containsKey(dbName)) {
            return JSUtil.wrapJsVar(true);
        }
        return JSUtil.wrapJsVar(false);
    }

    public void closeDatabase(IWebview pWebview, JSONArray array) {
        String callBackID = array.optString(0);
        String name = array.optString(1);
        SQLiteDatabase database = map.get(name);
        if (PdrUtil.isEmpty(name)) {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1404, DOMException.toString("parameter can't be null")), JSUtil.ERROR, true, false);
            return;
        }
        if (database != null) {
            database.close();
            map.remove(name);
            JSUtil.execCallback(pWebview, callBackID, "{}", JSUtil.OK, true, false);
        } else {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1401, DOMException.toString("Not Open")), JSUtil.ERROR, true, false);
        }
    }

    public void transaction(IWebview pWebview, JSONArray array) {
        String callBackID = array.optString(0);
        String name = array.optString(1);
        SQLiteDatabase database = map.get(name);
        String operation = array.optString(2);
        if (PdrUtil.isEmpty(name) || PdrUtil.isEmpty(operation)) {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1404, DOMException.toString("parameter can't be null")), JSUtil.ERROR, true, false);
            return;
        }
        if (database != null) {
            try {
                if (operation.equals("begin")) {
                    database.beginTransaction();
                } else if (operation.equals("commit")) {
                    database.setTransactionSuccessful();
                    database.endTransaction();
                } else if (operation.equals("rollback")) {
                    database.endTransaction();
                } else {
                    JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1404, DOMException.toString("Operation Error")), JSUtil.ERROR, true, false);
                }
                JSUtil.execCallback(pWebview, callBackID, "{}", JSUtil.OK, true, false);
            } catch (Exception e) {
                JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1404, DOMException.toString(PdrUtil.makeQueryStringAllRegExp(e.toString()))), JSUtil.ERROR, true, false);
            }
        } else {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1401, DOMException.toString("Not Open")), JSUtil.ERROR, true, false);
        }
    }

    public void executeSql(IWebview pWebview, JSONArray array) {
        String callBackID = array.optString(0);
        String name = array.optString(1);
        SQLiteDatabase database = map.get(name);
        String sql = array.optString(2);
        if (PdrUtil.isEmpty(name) || PdrUtil.isEmpty(sql)) {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1404, DOMException.toString("parameter can't be null")), JSUtil.ERROR, true, false);
            return;
        }
        if (database != null) {
            try {
                database.execSQL(sql);
            } catch (Exception e) {
                JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1404, DOMException.toString(PdrUtil.makeQueryStringAllRegExp(e.toString()))), JSUtil.ERROR, true, false);
                return;
            }
            JSUtil.execCallback(pWebview, callBackID, "{}", JSUtil.OK, true, false);
        } else {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1401, DOMException.toString("Not Open")), JSUtil.ERROR, true, false);
        }
    }

    public void selectSql(IWebview pWebview, JSONArray array) {
        String callBackID = array.optString(0);
        String name = array.optString(1);
        SQLiteDatabase database = map.get(name);
        if (database == null) {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1401, DOMException.toString("Not Open")), JSUtil.ERROR, true, false);
            return;
        }
        String sql = array.optString(2);
        if (PdrUtil.isEmpty(name) || PdrUtil.isEmpty(sql)) {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1404, DOMException.toString("parameter can't be null")), JSUtil.ERROR, true, false);
            return;
        }
        Cursor cursor;
        try {
            cursor = database.rawQuery(sql, null);
        } catch (Exception e) {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1404, DOMException.toString(PdrUtil.makeQueryStringAllRegExp(e.toString()))), JSUtil.ERROR, true, false);
            return;
        }
        JSONArray arr = new JSONArray();
        try {
            if (cursor.moveToFirst()) {
                String[] names = cursor.getColumnNames();
                do {
                    JSONObject res = new JSONObject();
                    for (int i = 0; i < names.length; i++) {
                        int type = cursor.getType(i);
                        try {
                            switch (type) {
                                case Cursor.FIELD_TYPE_NULL:
                                    res.put(names[i], JSONObject.NULL);
                                    break;
                                case Cursor.FIELD_TYPE_STRING:
                                    res.put(names[i], cursor.getString(i));
                                    break;
                                case Cursor.FIELD_TYPE_FLOAT:
                                    BigDecimal decimal = new BigDecimal(String.valueOf(cursor.getFloat(i)));
                                    res.put(names[i], decimal.doubleValue());
                                    break;
                                case Cursor.FIELD_TYPE_INTEGER:
                                    res.put(names[i], cursor.getInt(i));
                                    break;
                                case Cursor.FIELD_TYPE_BLOB:
                                    res.put(names[i], Arrays.toString(cursor.getBlob(i)));
                                    break;
                            }
                        } catch (JSONException e) {
                        }
                    }
                    arr.put(res);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            JSUtil.execCallback(pWebview, callBackID, StringUtil.format(resultMessage, -1404, DOMException.toString(PdrUtil.makeQueryStringAllRegExp(e.toString()))), JSUtil.ERROR, true, false);
            return;
        }
        JSUtil.execCallback(pWebview, callBackID, arr, JSUtil.OK, false);
    }
}
