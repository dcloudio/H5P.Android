package io.dcloud.feature.contacts;

import io.dcloud.common.DHInterface.IWebview;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

/**
 * 
 * <p>Description:联系人存储对象</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-9 下午12:12:48 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-9 下午12:12:48</pre>
 */
public abstract class ContactAccessor {
	
    protected final String LOG_TAG = "ContactsAccessor";
    protected Context mApp;
    protected IWebview mView;
	
    /**
     * 
     * Description:是否是必需的字段
     * @param key
     * @param map
     * @return
     *
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午4:54:58</pre>
     */
    protected boolean isRequired(String key, HashMap<String,Boolean> map) {
		Boolean retVal = map.get(key);
		return (retVal == null) ? false : retVal.booleanValue();
	}
    
    /**
     * 
     * Description:
     * @param fields
     * @return
     *
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午4:54:10</pre>
     */
	protected HashMap<String,Boolean> buildPopulationSet(JSONArray fields) {
		HashMap<String,Boolean> map = new HashMap<String,Boolean>();
		
		String key;
		try {
		    if (fields.length() == 0 || (fields.length() == 1 && fields.getString(0).equals("*"))) {
                map.put("displayName", true);
                map.put("name", true);
                map.put("nickname", true);
                map.put("phoneNumbers", true);
                map.put("emails", true);
                map.put("addresses", true);
                map.put("ims", true);
                map.put("organizations", true);
                map.put("birthday", true);
                map.put("note", true);
                map.put("urls", true);
                map.put("photos", true);
                map.put("categories", true);
		    } 
		    else {
    			for (int i=0; i<fields.length(); i++) {
    				key = fields.getString(i);
    				if (key.startsWith("displayName")) {
    					map.put("displayName", true);
    				}
    				else if (key.startsWith("name")) {
    					map.put("name", true);
    				}
    				else if (key.startsWith("nickname")) {
    					map.put("nickname", true);
    				}
    				else if (key.startsWith("phoneNumbers")) {
    					map.put("phoneNumbers", true);
    				}
    				else if (key.startsWith("emails")) {
    					map.put("emails", true);
    				}
    				else if (key.startsWith("addresses")) {
    					map.put("addresses", true);
    				}
    				else if (key.startsWith("ims")) {
    					map.put("ims", true);
    				}
    				else if (key.startsWith("organizations")) {
    					map.put("organizations", true);
    				}
    				else if (key.startsWith("birthday")) {
    					map.put("birthday", true);
    				}
    				else if (key.startsWith("note")) {
    					map.put("note", true);
    				}
    				else if (key.startsWith("urls")) {
    					map.put("urls", true);
    				}
                    else if (key.startsWith("photos")) {
                        map.put("photos", true);
                    }
                    else if (key.startsWith("categories")) {
                        map.put("categories", true);
                    }
    			}
		    }
		}
		catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		return map;
	}
	
	/**
	 * 
	 * Description:获取对象的属性
	 * @param obj
	 * @param property
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午4:53:08</pre>
	 */
	protected String getJsonString(JSONObject obj, String property) {
		if(obj != null){
			String i = obj.optString(property);
			if(i == null || "null".equals(i)){
				return null;
			}else{
				return i;
			}
		}else{
			return null;
		}
	}

   /**
    * 
    * Description:保存（更新）联系人到手机
    * @param contact 联系人JSON对象
    * @return
    *
    * <pre><p>ModifiedLog:</p>
    * Log ID: 1.0 (Log编号 依次递增)
    * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午4:51:03</pre>
    */
	public abstract boolean save(JSONObject contact);

   /**
    * 
    * Description:查找联系人
    * @param filter
    * @param options
    * @return
    *
    * <pre><p>ModifiedLog:</p>
    * Log ID: 1.0 (Log编号 依次递增)
    * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午4:51:54</pre>
    */
    public abstract JSONArray search(JSONArray filter, JSONObject options);

//   /**
//    * 
//    * Description:通过ID查找联系人
//    * @param id
//    * @return
//    * @throws JSONException
//    *
//    * <pre><p>ModifiedLog:</p>
//    * Log ID: 1.0 (Log编号 依次递增)
//    * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午4:52:10</pre>
//    */
//    public abstract JSONObject getContactById(String id) throws JSONException;

    /**
     * 
     * Description:通过ID删除联系人
     * @param id
     * @return
     *
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午4:52:41</pre>
     */
	public abstract boolean remove(String id);
	
	/**
	 * A class that represents the where clause to be used in the database query 
	 */
	class WhereOptions {
		private String selection;
		private String[] selectionArgs;
		public void setSelection(String where) {
			this.selection = where;
		}
		public String getSelection() {
			return selection;
		}
		public void setSelectionArgs(String[] whereArgs) {
			this.selectionArgs = whereArgs;
		}
		public String[] getSelectionArgs() {
			return selectionArgs;
		}
	}
}