package io.dcloud.feature.contacts;

import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.util.JSONUtil;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Base64;
import android.util.Log;

/**
 * 
 * <p>Description:联系人存储实现</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-9 下午12:11:53 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-9 下午12:11:53</pre>
 */
public class ContactAccessorImpl extends ContactAccessor {

	private static final String Tag = "contacts"; 
	private static final long MAX_PHOTO_SIZE = 1048576;
	
	private static final String EMAIL_REGEXP = ".+@.+\\.+.+";  /* <anything>@<anything>.<anything>*/

    private static final Map<String, String> dbMap = new HashMap<String, String>();
    static {
    	dbMap.put("id", Data.RAW_CONTACT_ID);
    	dbMap.put("displayName", ContactsContract.Contacts.DISPLAY_NAME);
    	dbMap.put("name", StructuredName.DISPLAY_NAME);
    	dbMap.put("name.formatted", StructuredName.DISPLAY_NAME);
    	dbMap.put("name.familyName", StructuredName.FAMILY_NAME);
    	dbMap.put("name.givenName", StructuredName.GIVEN_NAME);
    	dbMap.put("name.middleName", StructuredName.MIDDLE_NAME);
    	dbMap.put("name.honorificPrefix", StructuredName.PREFIX);
    	dbMap.put("name.honorificSuffix", StructuredName.SUFFIX);
    	dbMap.put("nickname", Nickname.NAME);
    	dbMap.put("phoneNumbers", Phone.NUMBER);
    	dbMap.put("phoneNumbers.value", Phone.NUMBER);
    	dbMap.put("emails", Email.DATA);
    	dbMap.put("emails.value", Email.DATA);
    	dbMap.put("addresses", StructuredPostal.FORMATTED_ADDRESS);
    	dbMap.put("addresses.formatted", StructuredPostal.FORMATTED_ADDRESS);
    	dbMap.put("addresses.streetAddress", StructuredPostal.STREET);
    	dbMap.put("addresses.locality", StructuredPostal.CITY);
    	dbMap.put("addresses.region", StructuredPostal.REGION);
    	dbMap.put("addresses.postalCode", StructuredPostal.POSTCODE);
    	dbMap.put("addresses.country", StructuredPostal.COUNTRY);
    	dbMap.put("ims", Im.DATA);
    	dbMap.put("ims.value", Im.DATA);
    	dbMap.put("organizations", Organization.COMPANY);
    	dbMap.put("organizations.name", Organization.COMPANY);
    	dbMap.put("organizations.department", Organization.DEPARTMENT);
    	dbMap.put("organizations.title", Organization.TITLE);
    	dbMap.put("birthday", Event.CONTENT_ITEM_TYPE);
    	dbMap.put("note", Note.NOTE);
    	dbMap.put("photos.value", Photo.CONTENT_ITEM_TYPE);
    	//dbMap.put("categories.value", null);
    	dbMap.put("urls", Website.URL);
    	dbMap.put("urls.value", Website.URL);
    }

    /**
     * Create an contact accessor.
     */
    public ContactAccessorImpl(Context app) {
		mApp = app;
	}
   
	@Override
	public JSONArray search(JSONArray fields, JSONObject options) {
		int limit = Integer.MAX_VALUE;
//		boolean multiple = true;
		// Get the find options
		JSONArray searchTerm = null;
		
		if (options != null) {
			searchTerm = JSONUtil.getJSONArray(options,"filter");//可设置为空，表示不过滤。 ([],null)
			limit = options.optBoolean("multiple",true) ? Integer.MAX_VALUE : 1;//是否查找多个联系人，默认值为true
		}
		//计算 是否无过滤条件，需要检索出素有
		boolean  noSearchTerm = searchTerm == null || searchTerm.length() == 0 || (searchTerm.length() == 1 && searchTerm.optJSONObject(0).isNull("field"));
		String selection = null;
		String[] selectionArgs = null;
//		boolean doContinue = true;
		if(noSearchTerm){//无过滤条件，需要检索出素有
			
		}else{//获得符合过滤条件的联系人id
//			Object[] obj = queryOnlyPhoneNumber(searchTerm);
//			contactIds = (HashSet<String>)obj[0];
//			filterLogics = (HashSet<String>)(obj[1]);
//			doContinue = (Boolean)obj[2];
//			phoneNumFilterLogic = (String)obj[3];
//			if(!doContinue){//不需要继续
//				return new JSONArray();
//			}
			WhereOptions whereOptions = buildWhereClause(searchTerm);
			selection = whereOptions.getSelection();
			selectionArgs = whereOptions.getSelectionArgs();
		}
		//组合查询条件,然后合并phoneNumber查询结果
		Cursor idCursor = mApp.getContentResolver().query(Data.CONTENT_URI,
				new String[] { Data.RAW_CONTACT_ID },
				selection,
				selectionArgs,
				Data.RAW_CONTACT_ID + " ASC");

		//通过组合查询结果，获取符合要求的id集合
		ArrayList<String> endContactIds = new ArrayList<String>();
		while (idCursor.moveToNext()) {
			String raw_id = idCursor.getString(idCursor.getColumnIndex(Data.RAW_CONTACT_ID));
			endContactIds.add(raw_id);
		}
		idCursor.close();
		
		//通过查询到的联系人集合，查询需要的列的值
		if(endContactIds.size() == 0){
			return new JSONArray();
		}
		// Build a query that only looks at ids
		WhereOptions idOptions = buildIdClause(endContactIds);
		
		// Do the id query
		Cursor c = mApp.getContentResolver().query(Data.CONTENT_URI,
				null,
				idOptions.getSelection(),
				idOptions.getSelectionArgs(),
				Data.RAW_CONTACT_ID + " ASC");				
		
		// Loop through the fields the user provided to see what data should be returned.
		HashMap<String,Boolean> populate = buildPopulationSet(fields);
		JSONArray contacts = populateContactArray(limit, populate, c);		
		return contacts;
	}
	
	/**
	 * 
	 * Description:根据查询结果获得联系人集合。
	 * @param limit
	 * @param populate
	 * @param c
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:58:25</pre>
	 */
    private JSONArray populateContactArray(int limit,
            HashMap<String, Boolean> populate, Cursor c) {
		
		long rawId = 0;
		long oldRawId = 0;
		boolean newContact = true;
		String mimetype = "";

		JSONArray contacts = new JSONArray();
		JSONObject contact = new JSONObject();
		JSONArray organizations = new JSONArray();
		JSONArray addresses = new JSONArray();
		JSONArray phones = new JSONArray();
		JSONArray emails = new JSONArray();
		JSONArray ims = new JSONArray();
		JSONArray urls = new JSONArray();
		JSONArray photos = new JSONArray();			
		
		if (c.getCount() > 0) {
			while (c.moveToNext() && (contacts.length() <= (limit-1))) {
				try {
					rawId = c.getLong(c.getColumnIndex(Data.RAW_CONTACT_ID));
					
					// If we are in the first row set the oldContactId
					if (c.getPosition() == 0) {
						oldRawId = rawId;
					}
					
					// When the contact ID changes we need to push the Contact object 
					// to the array of contacts and create new objects.
					if (oldRawId != rawId) {
						// Populate the Contact object with it's arrays
						// and push the contact into the contacts array
						contacts.put(populateContact(populate,contact, organizations, addresses, phones,
								emails, ims, urls, photos));
						
						// Clean up the objects
						contact = new JSONObject();
						organizations = new JSONArray();
						addresses = new JSONArray();
						phones = new JSONArray();
						emails = new JSONArray();
						ims = new JSONArray();
						urls = new JSONArray();
						photos = new JSONArray();
						
						// Set newContact to true as we are starting to populate a new contact
						newContact = true;
					}
					
					// When we detect a new contact set the ID and display name.
					// These fields are available in every row in the result set returned.
					if (newContact) {
						newContact = false;
						contact.put("id", rawId);
					}
					
					// Grab the mimetype of the current row as it will be used in a lot of comparisons
					mimetype = c.getString(c.getColumnIndex(Data.MIMETYPE));
					
					if (mimetype.equals(StructuredName.CONTENT_ITEM_TYPE)) {
						contact.put("displayName", c.getString(c.getColumnIndex(StructuredName.DISPLAY_NAME)));
					}
					if (mimetype.equals(StructuredName.CONTENT_ITEM_TYPE) 
							&& isRequired("name",populate)) {
						contact.put("name", nameQuery(c));
					}
					else if (mimetype.equals(Phone.CONTENT_ITEM_TYPE) 
							&& isRequired("phoneNumbers",populate)) {
						phones.put(phoneQuery(c));
					}
					else if (mimetype.equals(Email.CONTENT_ITEM_TYPE) 
							&& isRequired("emails",populate)) {
						emails.put(emailQuery(c));
					}
					else if (mimetype.equals(StructuredPostal.CONTENT_ITEM_TYPE) 
							&& isRequired("addresses",populate)) {
						addresses.put(addressQuery(c));
					}
					else if (mimetype.equals(Organization.CONTENT_ITEM_TYPE) 
							&& isRequired("organizations",populate)) {
						organizations.put(organizationQuery(c));
					}
					else if (mimetype.equals(Im.CONTENT_ITEM_TYPE) 
							&& isRequired("ims",populate)) {
						ims.put(imQuery(c));
					}
					else if (mimetype.equals(Note.CONTENT_ITEM_TYPE) 
							&& isRequired("note",populate)) {
						contact.put("note",c.getString(c.getColumnIndex(Note.NOTE)));
					}
					else if (mimetype.equals(Nickname.CONTENT_ITEM_TYPE) 
							&& isRequired("nickname",populate)) {
						contact.put("nickname",c.getString(c.getColumnIndex(Nickname.NAME)));
					}
					else if (mimetype.equals(Website.CONTENT_ITEM_TYPE) 
							&& isRequired("urls",populate)) {
						urls.put(urlQuery(c));
					}
					else if (mimetype.equals(Event.CONTENT_ITEM_TYPE)) {
						if (Event.TYPE_BIRTHDAY == c.getInt(c.getColumnIndex(Event.TYPE)) 
								&& isRequired("birthday",populate)) {
							contact.put("birthday", c.getString(c.getColumnIndex(Event.START_DATE)));
						}
					}
					else if (mimetype.equals(Photo.CONTENT_ITEM_TYPE) 
							&& isRequired("photos",populate)) {
						long contactId = c.getLong(c.getColumnIndex(Data.CONTACT_ID));
						photos.put(photoQuery(c, contactId));
					}
				}
				catch (JSONException e) {
					Log.e(LOG_TAG, e.getMessage(),e);
				}
				
				// Set the old contact ID 
				oldRawId = rawId;			
			}
	
			// Push the last contact into the contacts array
			if (contacts.length() < limit) {
				contacts.put(populateContact(populate,contact, organizations, addresses, phones,
						emails, ims, urls, photos));
			}
		}
		c.close();
        return contacts;
    }

	/**
	 * 
	 * Description:根据查找字符构建查找索引对象
	 * @param contactIds
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:57:01</pre>
	 */
	private WhereOptions buildIdClause(ArrayList<String> contactIds) {		
		WhereOptions options = new WhereOptions();

		// This clause means that there are specific ID's to be populated
		Iterator<String> it = contactIds.iterator();
		StringBuffer buffer = new StringBuffer("(");
		
		while (it.hasNext()) {
			buffer.append("'" + it.next() + "'");
			if (it.hasNext()) {
				buffer.append(",");
			}
		}
		buffer.append(")");
		
		options.setSelection(Data.RAW_CONTACT_ID + " IN " + buffer.toString());
		options.setSelectionArgs(null);		
				
		return options;
	}

	/**
	 * 
	 * Description:合并联系人JSON对象
	 * @param populate 需要返回的列的集合
	 * @param contact
	 * @param organizations
	 * @param addresses
	 * @param phones
	 * @param emails
	 * @param ims
	 * @param websites
	 * @param photos
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:56:34</pre>
	 */
	private JSONObject populateContact(HashMap<String, Boolean> populate,JSONObject contact, JSONArray organizations,
			JSONArray addresses, JSONArray phones, JSONArray emails,
			JSONArray ims, JSONArray websites, JSONArray photos) {
		try {
		    // Only return the array if it has at least one entry
			if (phones.length() > 0 || isRequired("phoneNumbers", populate)) {
				contact.put("phoneNumbers", phones);
			}
			if (emails.length() > 0 || isRequired("emails", populate)) {
				contact.put("emails", emails);
			}
			if (addresses.length() > 0 || isRequired("addresses", populate)) {
				contact.put("addresses", addresses);
			}
			if (ims.length() > 0 || isRequired("ims", populate)) {
				contact.put("ims", ims);
			}
            if (organizations.length() > 0 || isRequired("organizations", populate)) {
                contact.put("organizations", organizations);
            }
            if (websites.length() > 0 || isRequired("urls", populate)) {
                contact.put("urls", websites);
            }
            if (photos.length() > 0 || isRequired("photos", populate)) {
                contact.put("photos", photos);
            }
		}
		catch (JSONException e) {
			Log.e(LOG_TAG,"e.getMessage()=="+e.getMessage(),e);
		}
		return contact;
	}
	
	private WhereOptions buildWhereClause(JSONArray searchArr) {
		
		WhereOptions options = new WhereOptions();
		int length;
		if(searchArr != null && (length = searchArr.length()) > 0){
			StringBuffer where = new StringBuffer();
			ArrayList<String> whereArgs = new ArrayList<String>();
			boolean hasReturn = false;
			for(int i = 0; i < length; i++){
				JSONObject obj = JSONUtil.getJSONObject(searchArr, i);
				String field = JSONUtil.getString(obj, "field");
				String logic = formatLogic(JSONUtil.getString(obj, "logic"));
				String value = JSONUtil.getString(obj, "value");
				if(field != null /*&& !field.equals("phoneNumbers") */
						&& dbMap.containsKey(field) && value != null && !value.equals("") ){
					value = value.replace('?', '_');
					value = value.replace('*', '%');
					String db_col = dbMap.get(field);
					where.append("(" + db_col + " LIKE ? )");
					if( i != length - 1){
						where.append(" " + logic + " ");
					}
					whereArgs.add(value);
					hasReturn = true;
				}
			}
			if(hasReturn){
				options.setSelection(where.toString());
				String[] args = new String[whereArgs.size()];
				whereArgs.toArray(args);
				options.setSelectionArgs(args);
			}
		}
        return options;
	}
	
	private String formatLogic(String unFormatLogic){
		if(unFormatLogic != null){
			unFormatLogic = unFormatLogic.toLowerCase();
		}else{
			unFormatLogic = "or";
		}
		if(unFormatLogic.equals("not")){
			
		}else if(unFormatLogic.equals("and")){
			
		}else{
			unFormatLogic = "or";
		}
		return unFormatLogic;
	}
	/**
	 * 
	 * Description:是否查询所有
	 * @param fields
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:55:33</pre>
	 */
	private boolean isSearchAll(JSONArray fields) {
	    // Only do a wildcard search if we are passed ["*"]
	    if (fields.length() == 1) {
	        try {
                if ("*".equals(fields.getString(0))) {
                    return true;
                }                
            } catch (JSONException e) {
                return false;
            }
	    }
        return false;
    }

   /**
    * 
    * Description:查询群组
    * @param cursor
    * @return
    *
    * <pre><p>ModifiedLog:</p>
    * Log ID: 1.0 (Log编号 依次递增)
    * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:54:46</pre>
    */
	private JSONObject organizationQuery(Cursor cursor) {
		JSONObject organization = new JSONObject();
		try {
			organization.put("id", cursor.getString(cursor.getColumnIndex(Organization._ID)));
			organization.put("pref", false); // Android does not store pref attribute
            organization.put("type", getOrgType(cursor.getInt(cursor.getColumnIndex(Organization.TYPE))));
			organization.put("department", cursor.getString(cursor.getColumnIndex(Organization.DEPARTMENT)));
			organization.put("name", cursor.getString(cursor.getColumnIndex(Organization.COMPANY)));
			organization.put("title", cursor.getString(cursor.getColumnIndex(Organization.TITLE)));
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		return organization;
	}

	/**
	 * 
	 * Description:查询地址
	 * @param cursor
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:53:24</pre>
	 */
	private JSONObject addressQuery(Cursor cursor) {
		JSONObject address = new JSONObject();
		try {
			address.put("id", cursor.getString(cursor.getColumnIndex(StructuredPostal._ID)));
			address.put("pref", false); // Android does not store pref attribute
            address.put("type", getAddressType(cursor.getInt(cursor.getColumnIndex(Organization.TYPE))));
			address.put("formatted", cursor.getString(cursor.getColumnIndex(StructuredPostal.FORMATTED_ADDRESS)));
			address.put("streetAddress", cursor.getString(cursor.getColumnIndex(StructuredPostal.STREET)));
			address.put("locality", cursor.getString(cursor.getColumnIndex(StructuredPostal.CITY)));
			address.put("region", cursor.getString(cursor.getColumnIndex(StructuredPostal.REGION)));
			address.put("postalCode", cursor.getString(cursor.getColumnIndex(StructuredPostal.POSTCODE)));
			address.put("country", cursor.getString(cursor.getColumnIndex(StructuredPostal.COUNTRY)));
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		return address;
	}

	/**
	 * 
	 * Description:查询名字
	 * @param cursor
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:53:16</pre>
	 */
	private JSONObject nameQuery(Cursor cursor) {
		JSONObject contactName = new JSONObject();
		try {
			String familyName = cursor.getString(cursor.getColumnIndex(StructuredName.FAMILY_NAME));
			String givenName = cursor.getString(cursor.getColumnIndex(StructuredName.GIVEN_NAME));
			String middleName = cursor.getString(cursor.getColumnIndex(StructuredName.MIDDLE_NAME));
			String honorificPrefix = cursor.getString(cursor.getColumnIndex(StructuredName.PREFIX));
			String honorificSuffix = cursor.getString(cursor.getColumnIndex(StructuredName.SUFFIX));

			// Create the formatted name
			StringBuffer formatted = new StringBuffer("");
			if (honorificPrefix != null) { formatted.append(honorificPrefix + " "); }
			if (givenName != null) { formatted.append(givenName + " "); }
			if (middleName != null) { formatted.append(middleName + " "); }
			if (familyName != null) { formatted.append(familyName + " "); }
			if (honorificSuffix != null) { formatted.append(honorificSuffix + " "); }
			
			contactName.put("familyName", familyName);
			contactName.put("givenName", givenName);
			contactName.put("middleName", middleName);
			contactName.put("honorificPrefix", honorificPrefix);
			contactName.put("honorificSuffix", honorificSuffix);
			contactName.put("formatted", formatted);
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		return contactName;
	}

	/**
	 * 
	 * Description:查询电话
	 * @param cursor
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:53:08</pre>
	 */
	private JSONObject phoneQuery(Cursor cursor) {
		JSONObject phoneNumber = new JSONObject();
		try {
			phoneNumber.put("id", cursor.getString(cursor.getColumnIndex(Phone._ID)));
			phoneNumber.put("pref", false); // Android does not store pref attribute
			phoneNumber.put("value", cursor.getString(cursor.getColumnIndex(Phone.NUMBER)));
			phoneNumber.put("type", getPhoneType(cursor.getInt(cursor.getColumnIndex(Phone.TYPE))));
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		catch (Exception excp) {
			Log.e(LOG_TAG, excp.getMessage(), excp);
		} 
		return phoneNumber;
	}

	/**
	 * 
	 * Description:查询邮箱
	 * @param cursor
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:52:57</pre>
	 */
	private JSONObject emailQuery(Cursor cursor) {
		JSONObject email = new JSONObject();
		try {
			email.put("id", cursor.getString(cursor.getColumnIndex(Email._ID)));
			email.put("pref", false); // Android does not store pref attribute
			email.put("value", cursor.getString(cursor.getColumnIndex(Email.DATA)));
			email.put("type", getContactType(cursor.getInt(cursor.getColumnIndex(Email.TYPE))));
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		return email;
	}

	/**
	 * 
	 * Description:查询IM
	 * @param cursor
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:52:47</pre>
	 */
	private JSONObject imQuery(Cursor cursor) {
		JSONObject im = new JSONObject();
		try {
			im.put("id", cursor.getString(cursor.getColumnIndex(Im._ID)));
			im.put("pref", false); // Android does not store pref attribute
			im.put("value", cursor.getString(cursor.getColumnIndex(Im.DATA)));
			im.put("type", getContactType(cursor.getInt(cursor.getColumnIndex(Im.TYPE))));
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		return im;
	}	

	/**
	 * 
	 * Description:查询网站、网址
	 * @param cursor
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:52:35</pre>
	 */
	private JSONObject urlQuery(Cursor cursor) {
		JSONObject website = new JSONObject();
		try {
			website.put("id", cursor.getString(cursor.getColumnIndex(Website._ID)));
			website.put("pref", false); // Android does not store pref attribute
			website.put("value", cursor.getString(cursor.getColumnIndex(Website.URL)));
			website.put("type", getContactType(cursor.getInt(cursor.getColumnIndex(Website.TYPE))));
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		return website;
	}	

	/**
	 * 
	 * Description:查询照片
	 * @param cursor
	 * @param rawId
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:52:23</pre>
	 */
	private JSONObject photoQuery(Cursor cursor, long contactId) {
		JSONObject photo = new JSONObject();
		try {
			photo.put("id", cursor.getString(cursor.getColumnIndex(Photo._ID)));
			photo.put("pref", false);
			photo.put("type", "url");
		    Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
//		    Uri photoUri = Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
		    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(mApp.getContentResolver(), person);
		    String base64Text = "";
		    try {
				byte[] buf = new byte[input.available()];
				input.read(buf);
				base64Text = "data:image/png;base64," + Base64.encodeToString(buf, Base64.NO_WRAP);
			} catch (Exception e) {
				e.printStackTrace();
			}
			photo.put("value", base64Text);
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		return photo;
	}

	@Override
	public boolean save(JSONObject contact) {
		AccountManager mgr = AccountManager.get(mApp);
		Account[] accounts = mgr.getAccounts();
		Account account = null;

		if (accounts.length == 1)
			account = accounts[0];
		else if (accounts.length > 1) {
			for(Account a : accounts){
				if(a.type.contains("eas")&& a.name.matches(EMAIL_REGEXP)) /*Exchange ActiveSync*/
				{
					account = a;
					break;
				}
			}	
			if(account == null){
				for(Account a : accounts){
					if(a.type.contains("com.google") && a.name.matches(EMAIL_REGEXP)) /*Google sync provider*/
					{
						account = a;
						break;
					}
				}
			}
			if(account == null){
				for(Account a : accounts){
					if(a.name.matches(EMAIL_REGEXP)) /*Last resort, just look for an email address...*/
					{
						account = a;
						break;
					}
				}	
			}
			if(account == null) account = accounts[0];//默认使用第一个账号
		}
		
		String id = getJsonString(contact, "id");
		// Create new contact
		if (id == null || "null".equals(id)) {
			return createNewContact(contact, account);
		}
		// Modify existing contact
		else {
			return modifyContact(contact, account, id);
		}
	}

	
	private void modifyContactOption(ArrayList<ContentProviderOperation> ops,JSONArray data,String rawId,String mimeType,String dataKey,String typeKey){
		
		ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
		          .withSelection(Data.RAW_CONTACT_ID + "=? and " + Data.MIMETYPE + "=?", 
		        		  new String[]{String.valueOf(rawId), mimeType})
		          .build());
		
		if (data != null) {
			for (int i=0; i<data.length(); i++) {
				JSONObject phone = data.optJSONObject(i);
				if (phone!=null) {
					ContentValues contentValues = new ContentValues();
				    contentValues.put(Data.RAW_CONTACT_ID, rawId);
				    contentValues.put(Data.MIMETYPE, mimeType);
				    contentValues.put(dataKey, getJsonString(phone, "value"));
			        contentValues.put(typeKey, getContactType(getJsonString(phone, "type")));

				    ops.add(ContentProviderOperation.newInsert(
				            Data.CONTENT_URI).withValues(contentValues).build()); 						
				}
			}
		}
	}
	/**
	 * 
	 * Description:修改联系人信息
	 * @param contact
	 * @param account
	 * @param id
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午4:55:53</pre>
	 */
	private boolean modifyContact(JSONObject contact, Account account, String id) {
		// Get the RAW_CONTACT_ID which is needed to insert new values in an already existing contact.
		// But not needed to update existing values.
//		int rawId = Integer.parseInt(id);
		
		// Create a list of attributes to add to the contact database
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		
		//Add contact type
		if(account != null){
			ops.add(ContentProviderOperation.newUpdate(ContactsContract.RawContacts.CONTENT_URI)
		        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
		        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
		        .build());
		}
		// Modify name
		JSONObject name;
		try {
			
//			ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
//			          .withSelection(Data.RAW_CONTACT_ID + "=? and " + Data.MIMETYPE + "=?", 
//			        		  new String[]{String.valueOf(id), StructuredName.CONTENT_ITEM_TYPE})
//			          .build());
			
			String displayName = getJsonString(contact, "displayName");
			name = contact.getJSONObject("name");
//			if (displayName != null || name != null) {
				ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI)
					.withSelection(Data.RAW_CONTACT_ID + "=? AND " + 
							Data.MIMETYPE + "=?", 
							new String[]{id, StructuredName.CONTENT_ITEM_TYPE});

//				if (displayName != null) {
					builder.withValue(StructuredName.DISPLAY_NAME, displayName);
//				}
					
				String familyName = getJsonString(name, "familyName");
//				if (familyName != null) {
					builder.withValue(StructuredName.FAMILY_NAME, familyName);
//				}
				String middleName = getJsonString(name, "middleName");
//				if (middleName != null) {
					builder.withValue(StructuredName.MIDDLE_NAME, middleName);
//				}
				String givenName = getJsonString(name, "givenName");
//				if (givenName != null) {
					builder.withValue(StructuredName.GIVEN_NAME, givenName);
//				}
				String honorificPrefix = getJsonString(name, "honorificPrefix");
//				if (honorificPrefix != null) {
					builder.withValue(StructuredName.PREFIX, honorificPrefix);
//				}
				String honorificSuffix = getJsonString(name, "honorificSuffix");
//				if (honorificSuffix != null) {
					builder.withValue(StructuredName.SUFFIX, honorificSuffix);
//				}
				
				ops.add(builder.build());
//			}
		} catch (JSONException e1) {
			Log.d(LOG_TAG, "Could not get name");
		}
		
		// Modify phone numbers
		{
			JSONArray	phones = contact.optJSONArray("phoneNumbers");
			modifyContactOption(ops, phones, id, Phone.CONTENT_ITEM_TYPE, Phone.NUMBER, Phone.TYPE);
		}
		// Modify emails
		
		{
			JSONArray emails = contact.optJSONArray("emails");
			modifyContactOption(ops, emails, id, Email.CONTENT_ITEM_TYPE, Email.DATA, Email.TYPE);
		}
		// Modify IMs
		{
			JSONArray ims = contact.optJSONArray("ims");
			modifyContactOption(ops, ims, id, Im.CONTENT_ITEM_TYPE, Im.DATA, Im.TYPE);
		}
				
		// Modify urls	
		{
			JSONArray websites = contact.optJSONArray("websites");
			modifyContactOption(ops, websites, id, Website.CONTENT_ITEM_TYPE, Website.DATA, Website.TYPE);
		}
		
		
		// Modify addresses
		JSONArray addresses = null;
		try {
			ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
			          .withSelection(Data.RAW_CONTACT_ID + "=? and " + Data.MIMETYPE + "=?", 
			        		  new String[]{String.valueOf(id), StructuredPostal.CONTENT_ITEM_TYPE})
			          .build());
			
			addresses = contact.optJSONArray("addresses");
			if (addresses != null) {
				for (int i=0; i<addresses.length(); i++) {
					JSONObject address = addresses.optJSONObject(i);
					if (address !=null) {
						ContentValues contentValues = new ContentValues();
					    contentValues.put(Data.RAW_CONTACT_ID, id);
					    contentValues.put(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
                        contentValues.put(StructuredPostal.TYPE, getAddressType(getJsonString(address, "type")));
					    contentValues.put(StructuredPostal.FORMATTED_ADDRESS, getJsonString(address, "formatted"));
				        contentValues.put(StructuredPostal.STREET, getJsonString(address, "streetAddress"));
				        contentValues.put(StructuredPostal.CITY, getJsonString(address, "locality"));
				        contentValues.put(StructuredPostal.REGION, getJsonString(address, "region"));
				        contentValues.put(StructuredPostal.POSTCODE, getJsonString(address, "postalCode"));
				        contentValues.put(StructuredPostal.COUNTRY, getJsonString(address, "country"));

					    ops.add(ContentProviderOperation.newInsert(
					            Data.CONTENT_URI).withValues(contentValues).build()); 						
					}
				}
			}
		}
		catch (Exception e) {
			Log.d(LOG_TAG, "Could not get addresses");
		}

		// Modify organizations
		JSONArray organizations = null;
		try {
			ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
			          .withSelection(Data.RAW_CONTACT_ID + "=? and " + Data.MIMETYPE + "=?", 
			        		  new String[]{String.valueOf(id), Organization.CONTENT_ITEM_TYPE})
			          .build());
			
			organizations = contact.optJSONArray("organizations");
			if (organizations != null) {
				for (int i=0; i<organizations.length(); i++) {
					JSONObject org = organizations.optJSONObject(i);
					if (org!=null) {
						ContentValues contentValues = new ContentValues();
					    contentValues.put(Data.RAW_CONTACT_ID, id);
					    contentValues.put(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
                        contentValues.put(Organization.TYPE, getOrgType(getJsonString(org, "type")));
					    contentValues.put(Organization.DEPARTMENT, getJsonString(org, "department"));
				        contentValues.put(Organization.COMPANY, getJsonString(org, "name"));
				        contentValues.put(Organization.TITLE, getJsonString(org, "title"));

					    ops.add(ContentProviderOperation.newInsert(
					            Data.CONTENT_URI).withValues(contentValues).build()); 						
					}
//					}
				}
			}
		}
		catch (Exception e) {
			Log.d(LOG_TAG, "Could not get organizations");
		}

		// Modify photos
		JSONArray photos = null;
		try {
			ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
			          .withSelection(Data.RAW_CONTACT_ID + "=? and " + Data.MIMETYPE + "=?", 
			        		  new String[]{String.valueOf(id), Photo.CONTENT_ITEM_TYPE})
			          .build());
			
			photos = contact.optJSONArray("photos");
			if (photos != null) {
				for (int i=0; i<photos.length(); i++) {
					JSONObject photo = photos.optJSONObject(i);
					if(photo != null){
						byte[] bytes = getPhotoBytes(getJsonString(photo, "value"));
						if (bytes!=null) {
							ContentValues contentValues = new ContentValues();
						    contentValues.put(Data.RAW_CONTACT_ID, id);
						    contentValues.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
						    contentValues.put(Data.IS_SUPER_PRIMARY, 1);
					        contentValues.put(Photo.PHOTO, bytes);
	
						    ops.add(ContentProviderOperation.newInsert(
						            Data.CONTENT_URI).withValues(contentValues).build()); 						
						}
					}
				}
			} 
			

			// Modify note
			{
				String note = getJsonString(contact, "note");
					ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
						.withSelection(Data.RAW_CONTACT_ID + "=? AND " + 
								Data.MIMETYPE + "=?", 
								new String[]{id,Note.CONTENT_ITEM_TYPE})
				        .withValue(Note.NOTE, note)
						.build());
			}
			// Modify nickname
			{
				String nickname = getJsonString(contact, "nickname");
				ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
					.withSelection(Data.RAW_CONTACT_ID + "=? AND " + 
							Data.MIMETYPE + "=?", 
							new String[]{id,Nickname.CONTENT_ITEM_TYPE})
							.withValue(Nickname.NAME, nickname)
							.build());
			}
			// Modify birthday
			{
				String birthday = getJsonString(contact, "birthday");
				ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
					.withSelection(Data.RAW_CONTACT_ID + "=? AND " + 
							Data.MIMETYPE + "=? AND " + 
							Event.TYPE + "=?", 
							new String[]{id,Event.CONTENT_ITEM_TYPE, ""+Event.TYPE_BIRTHDAY})
			        .withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
			        .withValue(Event.START_DATE, birthday)
			        .build());
			}
		}
		catch (Exception e) {
			Log.d(LOG_TAG, "Could not get photos");
		}
		
		boolean retVal = true;
		ContentProviderResult[] ret = null;
		//Modify contact
		try {
			ret = mApp.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
			Log.e(LOG_TAG, Log.getStackTraceString(e), e);
			retVal = false;
		} catch (OperationApplicationException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
			Log.e(LOG_TAG, Log.getStackTraceString(e), e);
			retVal = false;
		}
		
		return retVal;
	}

	/**
	 * 
	 * Description:插入网站、网址
	 * @param ops
	 * @param website
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:51:54</pre>
	 */
	private void insertWebsite(ArrayList<ContentProviderOperation> ops,
			JSONObject website) {
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
		        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
		        .withValue(Data.MIMETYPE, Website.CONTENT_ITEM_TYPE)
		        .withValue(Website.DATA, getJsonString(website, "value"))
		        .withValue(Website.TYPE, getContactType(getJsonString(website, "type")))
		        .build());
	}

	/**
	 * 
	 * Description:插入IM
	 * @param ops
	 * @param im
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:51:45</pre>
	 */
	private void insertIm(ArrayList<ContentProviderOperation> ops, JSONObject im) {
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
		        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
		        .withValue(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE)
		        .withValue(Im.DATA, getJsonString(im, "value"))
		        .withValue(Im.TYPE, getContactType(getJsonString(im, "type")))
		        .build());
	}

	/**
	 * 
	 * Description:插入群组
	 * @param ops
	 * @param org
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:51:30</pre>
	 */
	private void insertOrganization(ArrayList<ContentProviderOperation> ops,
			JSONObject org) {
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
		        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
		        .withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
		        .withValue(Organization.TYPE, getOrgType(getJsonString(org, "type")))
                .withValue(Organization.DEPARTMENT, getJsonString(org, "department"))
		        .withValue(Organization.COMPANY, getJsonString(org, "name"))
		        .withValue(Organization.TITLE, getJsonString(org, "title"))
		        .build());
	}

	/**
	 * 
	 * Description:插入地址
	 * @param ops
	 * @param address
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:51:21</pre>
	 */
	private void insertAddress(ArrayList<ContentProviderOperation> ops,
			JSONObject address) {
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
		        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
		        .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
                .withValue(StructuredPostal.TYPE, getAddressType(getJsonString(address, "type")))
		        .withValue(StructuredPostal.FORMATTED_ADDRESS, getJsonString(address, "formatted"))
		        .withValue(StructuredPostal.STREET, getJsonString(address, "streetAddress"))
		        .withValue(StructuredPostal.CITY, getJsonString(address, "locality"))
		        .withValue(StructuredPostal.REGION, getJsonString(address, "region"))
		        .withValue(StructuredPostal.POSTCODE, getJsonString(address, "postalCode"))
		        .withValue(StructuredPostal.COUNTRY, getJsonString(address, "country"))
		        .build());
	}

	/**
	 * 
	 * Description:插入邮箱
	 * @param ops
	 * @param email
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:51:09</pre>
	 */
	private void insertEmail(ArrayList<ContentProviderOperation> ops,
			JSONObject email) {
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
		        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
		        .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
		        .withValue(Email.DATA, getJsonString(email, "value"))
		        .withValue(Email.TYPE, getPhoneType(getJsonString(email, "type")))
		        .build());
	}

	/**
	 * 
	 * Description:插入电话
	 * @param ops
	 * @param phone
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:51:01</pre>
	 */
	private void insertPhone(ArrayList<ContentProviderOperation> ops,
			JSONObject phone) {
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
		        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
		        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
		        .withValue(Phone.NUMBER, getJsonString(phone, "value"))
		        .withValue(Phone.TYPE, getPhoneType(getJsonString(phone, "type")))
		        .build());
	}

	/**
	 * 
	 * Description:插入头像/照片
	 * @param ops
	 * @param photo
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:49:57</pre>
	 */
	private void insertPhoto(ArrayList<ContentProviderOperation> ops,
			JSONObject photo) {
		byte[] bytes = getPhotoBytes(getJsonString(photo, "value"));
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
		        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
		        .withValue(Data.IS_SUPER_PRIMARY, 1)
		        .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
		        .withValue(Photo.PHOTO, bytes)
		        .build());
	}
	
	/**
	 * 
	 * Description:根据地址获得二进制字节
	 * @param filename
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:50:37</pre>
	 */
	private byte[] getPhotoBytes(String filename) {
		int pos = filename.indexOf(";base64,"); //filename=data:image/png;base64, 当前filename中含有“;base64,”时则认为是base64数据
		if(pos > 0){
			filename = filename.substring(pos + ";base64,".length());
			if(DeviceInfo.sDeviceSdkVer >= 8){
				return android.util.Base64.decode(filename, android.util.Base64.NO_WRAP);
			}else{
				return io.dcloud.common.util.Base64.decode2bytes(filename);
			}
		}
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			int bytesRead = 0;
			long totalBytesRead = 0;
			InputStream in = getPathFromUri(filename);
			if(in != null){
				byte[] data = new byte[8192];
				
				while ((bytesRead = in.read(data, 0, data.length)) != -1 && totalBytesRead <= MAX_PHOTO_SIZE) {
					buffer.write(data, 0, bytesRead);
					totalBytesRead += bytesRead;
				}
				
				in.close();
				buffer.flush();
			}
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		return buffer.toByteArray();
	}
/**
 * 
 * Description:根据地址获取头像输入流
 * @param path
 * @return
 * @throws IOException
 *
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午7:50:44</pre>
 */
    private InputStream getPathFromUri(String path) throws IOException {  	  
    	if (path.startsWith("content:")) {
    		Uri uri = Uri.parse(path);
    		return mApp.getContentResolver().openInputStream(uri);
    	}
    	if (path.startsWith("http:") || path.startsWith("file:")) {
    		URL url = new URL(path);
    		return url.openStream();
    	}
    	else {
    		return mView.obtainFrameView().obtainApp().obtainResInStream(mView.obtainFullUrl(),path);
    	}
    }  

	/**
	 * 
	 * Description:创建新的联系人对象
	 * @param contact
	 * @param account
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午4:56:07</pre>
	 */
	private boolean createNewContact(JSONObject contact, Account account) {
		// Create a list of attributes to add to the contact database
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		//记录传入contact里属性
		ArrayList<JSONObject> jsonObjs = new ArrayList<JSONObject>();
		if(account != null){
			//Add contact type
			ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
			        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
			        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
			        .build());
		}else{
			ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
			        .withValue(ContactsContract.RawContacts.AGGREGATION_MODE, ContactsContract.RawContacts.AGGREGATION_MODE_DISABLED)
			        .build());
		}
		jsonObjs.add(contact);
		int rawContactId = 0;
//		if(contact.isNull("id")){//没有id则认为需要新创建联系人
//			ContentValues values = new ContentValues();  
//			Uri rawContactUri = mApp.getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, values);  
//			rawContactId = (int)ContentUris.parseId(rawContactUri);
//			if(rawContactId >0) return false;
//		}else{
//			rawContactId = contact.optInt("id");
//		}
		JSONObject name = contact.optJSONObject("name");
		String displayName = getJsonString(contact, "displayName");
		if (displayName != null || name != null) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, 0)
					.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
					.withValue(StructuredName.DISPLAY_NAME, displayName)
					.withValue(StructuredName.FAMILY_NAME, getJsonString(name, "familyName"))
					.withValue(StructuredName.MIDDLE_NAME, getJsonString(name, "middleName"))
					.withValue(StructuredName.GIVEN_NAME, getJsonString(name, "givenName"))
					.withValue(StructuredName.PREFIX, getJsonString(name, "honorificPrefix"))
					.withValue(StructuredName.SUFFIX, getJsonString(name, "honorificSuffix"))
					.build());
			jsonObjs.add(name);
		}
		
		//Add phone numbers
		JSONArray phones = null;
		try {
			phones = contact.getJSONArray("phoneNumbers");
			if (phones != null) {
				for (int i=0; i<phones.length(); i++) {
					JSONObject phone = (JSONObject)phones.get(i);
					insertPhone(ops, phone);
//					jsonObjs.add(phone);
				}
			}
		}
		catch (JSONException e) {
			Log.d(LOG_TAG, "Could not get phone numbers");
		}
				
		// Add emails
		JSONArray emails = null;
		try {
			emails = contact.getJSONArray("emails");
			if (emails != null) {
				for (int i=0; i<emails.length(); i++) {
					JSONObject email = (JSONObject)emails.get(i);
					insertEmail(ops, email);
					jsonObjs.add(email);
				}
			}
		}
		catch (JSONException e) {
			Log.d(LOG_TAG, "Could not get emails");
		}

		// Add addresses
		JSONArray addresses = null;
		try {
			addresses = contact.getJSONArray("addresses");
			if (addresses != null) {
				for (int i=0; i<addresses.length(); i++) {
					JSONObject address = (JSONObject)addresses.get(i);
					insertAddress(ops, address);
					jsonObjs.add(address);
				}
			}
		}
		catch (JSONException e) {
			Log.d(LOG_TAG, "Could not get addresses");
		}

		// Add organizations
		JSONArray organizations = null;
		try {
			organizations = contact.getJSONArray("organizations");
			if (organizations != null) {
				for (int i=0; i<organizations.length(); i++) {
					JSONObject org = (JSONObject)organizations.get(i);
					insertOrganization(ops, org);
					jsonObjs.add(org);
				}
			}
		}
		catch (JSONException e) {
			Log.d(LOG_TAG, "Could not get organizations");
		}

		// Add IMs
		JSONArray ims = null;
		try {
			ims = contact.getJSONArray("ims");
			if (ims != null) {
				for (int i=0; i<ims.length(); i++) {
					JSONObject im = (JSONObject)ims.get(i);
					insertIm(ops, im);
					jsonObjs.add(im);
				}
			}
		}
		catch (JSONException e) {
			Log.d(LOG_TAG, "Could not get emails");
		}

		
		
		// Add urls	
		JSONArray websites = null;
		try {
			websites = contact.getJSONArray("websites");
			if (websites != null) {
				for (int i=0; i<websites.length(); i++) {
					JSONObject website = (JSONObject)websites.get(i);
					insertWebsite(ops, website);
					jsonObjs.add(website);
				}
			}
		}
		catch (JSONException e) {
			Log.d(LOG_TAG, "Could not get websites");
		}
		
		// Add photos
		JSONArray photos = null;
		try {
			photos = contact.getJSONArray("photos");
			if (photos != null) {
				for (int i=0; i<photos.length(); i++) {
					JSONObject photo = (JSONObject)photos.get(i);
					insertPhoto(ops, photo);
					jsonObjs.add(photo);
				}
			}
		}
		catch (JSONException e) {
			Log.d(LOG_TAG, "Could not get photos");
		}
		
		// Add birthday
		String birthday = getJsonString(contact, "birthday");
		if (birthday != null) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
			        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
			        .withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE)
			        .withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
			        .withValue(Event.START_DATE, birthday)
			        .build());
		}
		
		// Add note
		String note = getJsonString(contact, "note");
		if (note != null) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
			        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
			        .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
			        .withValue(Note.NOTE, note)
			        .build());
		}

		// Add nickname
		String nickname = getJsonString(contact, "nickname");
		if (nickname != null) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
			        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
			        .withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
			        .withValue(Nickname.NAME, nickname)
			        .build());
		}
		
		

		boolean suc = false;
		//Add contact
		try {
		    ContentProviderResult[] cpResults = mApp.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            if (cpResults.length >= 0) {
            	for(int i=0; i<jsonObjs.size(); i++){
            		String newId = cpResults[i].uri.getLastPathSegment();
            		JSONObject jsonObj = jsonObjs.get(i);
            		if(jsonObj != null){
            			jsonObj.put("id", newId);
            		}
            	}
            }
		    suc = true;
		} catch (RemoteException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		} catch (OperationApplicationException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		} catch (JSONException e) {
			Logger.e(e.getMessage());
		}
		return suc;
	}

	@Override
	public boolean remove(String id) {
    	ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
    	ops.add(ContentProviderOperation
				.newDelete(
						ContentUris.withAppendedId(
								RawContacts.CONTENT_URI, Long.parseLong(id))).build());
    	boolean ret = false;
		try {
			ret = mApp.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops).length > 0 ? true : false;
		} catch (Exception e) {
		}
		return ret;
	}	


/**************************************************************************
 * 	
 * 所有的android类型和JS类型转换
 * 
 *************************************************************************/
	
	/**
     * 
     * Description:根据字符电话类型获得android电话类型
     * @param string
     * @return
     *
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午6:58:50</pre>
     */
	private int getPhoneType(String string) {
		int type = Phone.TYPE_OTHER;
		if ("home".equals(string.toLowerCase())) {
			return Phone.TYPE_HOME;
		}
		else if ("mobile".equals(string.toLowerCase())) {
			return Phone.TYPE_MOBILE;
		}
		else if ("work".equals(string.toLowerCase())) {
			return Phone.TYPE_WORK;
		}
		else if ("work fax".equals(string.toLowerCase())) {
			return Phone.TYPE_FAX_WORK;
		}
		else if ("home fax".equals(string.toLowerCase())) {
			return Phone.TYPE_FAX_HOME;
		}
		else if ("fax".equals(string.toLowerCase())) {
			return Phone.TYPE_FAX_WORK;
		}
		else if ("pager".equals(string.toLowerCase())) {
			return Phone.TYPE_PAGER;
		}
		else if ("other".equals(string.toLowerCase())) {
			return Phone.TYPE_OTHER;
		}
		else if ("car".equals(string.toLowerCase())) {
			return Phone.TYPE_CAR;
		}
		else if ("company main".equals(string.toLowerCase())) {
			return Phone.TYPE_COMPANY_MAIN;
		}
		else if ("isdn".equals(string.toLowerCase())) {
			return Phone.TYPE_ISDN;
		}
		else if ("main".equals(string.toLowerCase())) {
			return Phone.TYPE_MAIN;
		}
		else if ("other fax".equals(string.toLowerCase())) {
			return Phone.TYPE_OTHER_FAX;
		}
		else if ("radio".equals(string.toLowerCase())) {
			return Phone.TYPE_RADIO;
		}
		else if ("telex".equals(string.toLowerCase())) {
			return Phone.TYPE_TELEX;
		}
		else if ("work mobile".equals(string.toLowerCase())) {
			return Phone.TYPE_WORK_MOBILE;
		}
		else if ("work pager".equals(string.toLowerCase())) {
			return Phone.TYPE_WORK_PAGER;
		}
		else if ("assistant".equals(string.toLowerCase())) {
			return Phone.TYPE_ASSISTANT;
		}
		else if ("mms".equals(string.toLowerCase())) {
			return Phone.TYPE_MMS;
		}
		else if ("callback".equals(string.toLowerCase())) {
			return Phone.TYPE_CALLBACK;
		}
		else if ("tty ttd".equals(string.toLowerCase())) {
			return Phone.TYPE_TTY_TDD;
		}
		else if ("custom".equals(string.toLowerCase())) {
			return Phone.TYPE_CUSTOM;
		}
		return type;
	}

	/**
    * 
    * Description:根据android电话类型型获得字符电话类型
    * @param type
    * @return
    *
    * <pre><p>ModifiedLog:</p>
    * Log ID: 1.0 (Log编号 依次递增)
    * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午6:57:55</pre>
    */
	private String getPhoneType(int type) {
		String stringType;
		switch (type) {
		case Phone.TYPE_CUSTOM:
			stringType = "custom";
			break;
		case Phone.TYPE_FAX_HOME:
			stringType = "home fax";
			break;
		case Phone.TYPE_FAX_WORK:
			stringType = "work fax";
			break;
		case Phone.TYPE_HOME:
			stringType = "home";
			break;
		case Phone.TYPE_MOBILE:
			stringType = "mobile";
			break;
		case Phone.TYPE_PAGER:
			stringType = "pager";
			break;
		case Phone.TYPE_WORK:
			stringType = "work";
			break;
		case Phone.TYPE_CALLBACK:
			stringType = "callback";
			break;
		case Phone.TYPE_CAR:
			stringType = "car";
			break;
		case Phone.TYPE_COMPANY_MAIN:
			stringType = "company main";
			break;
		case Phone.TYPE_OTHER_FAX:
			stringType = "other fax";
			break;
		case Phone.TYPE_RADIO:
			stringType = "radio";
			break;
		case Phone.TYPE_TELEX:
			stringType = "telex";
			break;
		case Phone.TYPE_TTY_TDD:
			stringType = "tty tdd";
			break;
		case Phone.TYPE_WORK_MOBILE:
			stringType = "work mobile";
			break;
		case Phone.TYPE_WORK_PAGER:
			stringType = "work pager";
			break;
		case Phone.TYPE_ASSISTANT:
			stringType = "assistant";
			break;
		case Phone.TYPE_MMS:
			stringType = "mms";
			break;
		case Phone.TYPE_ISDN:
			stringType = "isdn";
			break;
		case Phone.TYPE_OTHER:
		default: 
			stringType = "other";
			break;
		}
		return stringType;
	}

	/**
     * 
     * Description:根据字符联系类型获得android联系类型
     * @param string
     * @return
     *
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午6:58:50</pre>
     */
    private int getContactType(String string) {
        int type = Email.TYPE_OTHER;
        if (string!=null) {
            if ("home".equals(string.toLowerCase())) {
                return Email.TYPE_HOME;
            }
            else if ("work".equals(string.toLowerCase())) {
                return Email.TYPE_WORK;
            }
            else if ("other".equals(string.toLowerCase())) {
                return Email.TYPE_OTHER;
            }
            else if ("mobile".equals(string.toLowerCase())) {
                return Email.TYPE_MOBILE;
            }
            else if ("custom".equals(string.toLowerCase())) {
                return Email.TYPE_CUSTOM;
            }       
        }
        return type;
    }

    /**
     * 
     * Description:根据android联系类型获得字符联系类型
     * @param type
     * @return
     *
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午6:57:55</pre>
     */
    private String getContactType(int type) {
        String stringType;
        switch (type) {
            case Email.TYPE_CUSTOM: 
                stringType = "custom";
                break;
            case Email.TYPE_HOME: 
                stringType = "home";
                break;
            case Email.TYPE_WORK: 
                stringType = "work";
                break;
            case Email.TYPE_MOBILE: 
                stringType = "mobile";
                break;
            case Email.TYPE_OTHER: 
            default: 
                stringType = "other";
                break;
        }
        return stringType;
    }


    /**
     * 
     * Description:根据字符组织类型获得android组织类型
     * @param string
     * @return
     *
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午6:58:50</pre>
     */
    private int getOrgType(String string) {
        int type = Organization.TYPE_OTHER;
        if (string!=null) {
            if ("work".equals(string.toLowerCase())) {
                return Organization.TYPE_WORK;
            }
            else if ("other".equals(string.toLowerCase())) {
                return Organization.TYPE_OTHER;
            }
            else if ("custom".equals(string.toLowerCase())) {
                return Organization.TYPE_CUSTOM;
            }       
        }
        return type;
    }

    /**
     * 
     * Description:根据android组织类型获得字符组织类型
     * @param type
     * @return
     *
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午6:57:55</pre>
     */
    private String getOrgType(int type) {
        String stringType;
        switch (type) {
            case Organization.TYPE_CUSTOM: 
                stringType = "custom";
                break;
            case Organization.TYPE_WORK: 
                stringType = "work";
                break;
            case Organization.TYPE_OTHER: 
            default: 
                stringType = "other";
                break;
        }
        return stringType;
    }

    /**
     * 
     * Description:根据字符地址类型获得android地址类型
     * @param string
     * @return
     *
     * <pre><p>ModifiedLog:</p>
     * Log ID: 1.0 (Log编号 依次递增)
     * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午6:58:50</pre>
     */
    private int getAddressType(String string) {
        int type = StructuredPostal.TYPE_OTHER;
        if (string!=null) {
            if ("work".equals(string.toLowerCase())) {
                return StructuredPostal.TYPE_WORK;
            }
            else if ("other".equals(string.toLowerCase())) {
                return StructuredPostal.TYPE_OTHER;
            }
            else if ("home".equals(string.toLowerCase())) {
                return StructuredPostal.TYPE_HOME;
            }       
        }
        return type;
    }

   /**
    * 
    * Description:根据android地址类型获得字符地址类型
    * @param type
    * @return
    *
    * <pre><p>ModifiedLog:</p>
    * Log ID: 1.0 (Log编号 依次递增)
    * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-10 下午6:57:55</pre>
    */
    private String getAddressType(int type) {
        String stringType;
        switch (type) {
            case StructuredPostal.TYPE_HOME: 
                stringType = "home";
                break;
            case StructuredPostal.TYPE_WORK: 
                stringType = "work";
                break;
            case StructuredPostal.TYPE_OTHER: 
            default: 
                stringType = "other";
                break;
        }
        return stringType;
    }
}