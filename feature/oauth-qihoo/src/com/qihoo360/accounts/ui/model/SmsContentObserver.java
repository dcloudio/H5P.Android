package com.qihoo360.accounts.ui.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.widget.EditText;

/**
 * 监听服务器下发的短信，并截取其中的验证码
 * 
 * @author wangzefeng
 * 
 */

public class SmsContentObserver extends ContentObserver {

	private static final String SMS_URI_INBOX = "content://sms/inbox";
	private static final String SMS_FILTER = "360安全中心";// 短信验证码过滤值;

	private final Pattern pattern = Pattern.compile("(\\d{6})");
	private Context mContext = null;
	private EditText etCode = null;

	public SmsContentObserver(Handler handler) {
		super(handler);
	}

	public SmsContentObserver(Context context, EditText etCode) {
		super(new Handler());
		this.mContext = context;
		this.etCode = etCode;
	}

	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		Cursor cursor = null;
		// 读取收件箱中指定内容的短信
		cursor = mContext.getContentResolver().query(Uri.parse(SMS_URI_INBOX),
				new String[] { "body", "read", "date" },
				"body like ? and read=?",
				new String[] { "%" + SMS_FILTER + "%", "0" }, "date desc");

		if (cursor != null) {// 如果短信内容包含“360安全中心”，则做以下处理
			cursor.moveToFirst();
			if (cursor.moveToFirst()) {

				String smsContentString = cursor.getString(cursor
						.getColumnIndex("body"));
				Matcher matcher = pattern.matcher(smsContentString);
				if (matcher.find()) {
					String smsContent = matcher.group();
					if (smsContent != null) {
						etCode.setText(smsContent);
						etCode.setSelection(etCode.getText().toString().trim()
								.length());// 定位光标位置为末尾处
						mContext.getContentResolver().unregisterContentObserver(this);
					}
				}
			}

		}
	}

}
