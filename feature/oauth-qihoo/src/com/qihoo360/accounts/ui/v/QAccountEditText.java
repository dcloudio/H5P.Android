
package com.qihoo360.accounts.ui.v;

import static com.qihoo360.accounts.base.env.BuildEnv.LOGE_ENABLED;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView.OnEditorActionListener;

import com.qihoo360.accounts.R;
import com.qihoo360.accounts.QihooAccount;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;

public class QAccountEditText extends LinearLayout {

    private static final String TAG = "ACCOUNT.QAccountEditText";

    // 存放从activity传递过来的变量，通过它来获取activity中的相关数据
    private IContainer mContainer;

    private static final String SEPERATOR = "@";

    private final String[] defaultMailList = {
    		"qq.com","163.com", "126.com","sina.com", "vip.sina.com","sina.cn","hotmail.com", "gmail.com", "sohu.com", "139.com","189.cn"
    };

    private String lastLoggedAccount = ""; // 上次登录的帐号

    private String lastInputStr = ""; // 最后输入的字符串

    private Context context;

    private AutoCompleteTextView autoComplete;

    private Boolean showLastLoggedAccount = false; // 是否显示上次登录的帐号
    
    private Boolean isLoginStat = false; // 是否是登录场景

    private Boolean autoCompleteSetWidthFlag = false; // 设置宽度的标识

    private SelectedCallback dropDownItemSelectedCallback;

    private ClearedCallback dropDownItemClearedCallback;

    private ImageButton delBtn;

    private ArrayAdapter<String> adapter = null;

    private final ArrayList<String> dropdownList = new ArrayList<String>();

    private ArrayList<EmailCount> mailList = new ArrayList<EmailCount>();

    // spf : sharedPreferences field
    private final String spfMailList = "LoginMailList";

    private final String spfAccount = "Account";

    private SharedPreferences sharedPreferences = null;

    private float density = -1;

    public interface ClearedCallback {
        public void run();
    }

    public interface SelectedCallback {
        public void run();
    }

    public QAccountEditText(Context context) {
        this(context, null);
    }

    public QAccountEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QAccountEditText(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        this.context = context;
        getScreenInfo();
        mailList = readMailList();
        sort(mailList);
        @SuppressLint("InflateParams")
        RelativeLayout view = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.qihoo_accounts_qaet_view, null, false);
        delBtn = (ImageButton) view.findViewById(R.id.qaet_delete);
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (showLastLoggedAccount) {
                    String inputStr = autoComplete.getText().toString();
                    if (lastLoggedAccount.equals(inputStr)) {
                        clearLastLoggedAccount();
                    }
                }
                // 当在LoginView的自动完成文本框中，点击“X”按钮，删除该帐号信息
                if (mContainer != null) {
                    removeAccountByName(autoComplete.getText().toString());
                }
                autoComplete.setText("");
                delBtn.setVisibility(View.INVISIBLE);
                if (dropDownItemClearedCallback != null) {
                    dropDownItemClearedCallback.run();
                }
                AddAccountsUtils.setViewFocus(autoComplete);
                AddAccountsUtils.displaySoftInput(context, autoComplete);
            }
        });

        autoComplete = (AutoCompleteTextView) view.findViewById(R.id.qaet_autoComplete);
        autoComplete.setDropDownBackgroundResource(R.drawable.qihoo_accounts_qaet_item_bg);
        autoComplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String inputStr = autoComplete.getText().toString();
                if (inputStr.equals(lastInputStr)) {
                    return;
                }
                lastInputStr = inputStr;
                updateDropdownList(inputStr);
                adapter = new ArrayAdapter<String>(context, R.layout.qihoo_accounts_qaet_item, dropdownList);
                autoComplete.setAdapter(adapter);
                if (!autoCompleteSetWidthFlag) {
                    autoComplete.setDropDownWidth(autoComplete.getMeasuredWidth() + 4);
                    autoCompleteSetWidthFlag = true;
                }
                delBtn.setVisibility(inputStr.length() > 0 ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        autoComplete.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    String inputStr = autoComplete.getText().toString();
                    delBtn.setVisibility(inputStr.length() > 0 ? View.VISIBLE : View.INVISIBLE);
                } else {
                    //delBtn.setVisibility(View.INVISIBLE);
                }
            }
        });
        autoComplete.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (dropDownItemSelectedCallback != null) {
                    dropDownItemSelectedCallback.run();
                }
            }
        });
        addView(view);
    }

    public final void setContainer(IContainer container) {
        mContainer = container;
    }

    public void showLastLoggedAccount(boolean enable) {
        showLastLoggedAccount = enable;

        if (showLastLoggedAccount) {
            lastLoggedAccount = readLoggedAccount();
            autoComplete.setText(lastLoggedAccount);
        }
    }
    
    public void setLoginStatBoolean(boolean isLogin){
    	isLoginStat=isLogin;
    }

    public void setHintText(int resId) {
        autoComplete.setHint(resId);
    }

    public void setTextSize(int unit, float size) {
        autoComplete.setTextSize(unit, size);
    }

    public void setTextColor(int color) {
        autoComplete.setTextColor(color);
    }

    /**
     * 动态更新下拉列表
     *
     * 登录下拉列表内容的显示规则：
     * 1.登录、注册成功的帐号列表
     * 2.用户输入内容
     * 3.用户输入内容 + 邮箱的后缀
     *
     * 注册下拉列表内容的显示规则：
     * 1.用户输入内容
     * 2.用户输入内容 + 邮箱的后缀
     * @param inputStr
     */
    private void updateDropdownList(String inputStr) {
        dropdownList.clear();
        int seperatorPosition = inputStr.indexOf(SEPERATOR);
        String mailName = "", mailSuffix = "";
        if (seperatorPosition > 0) {
            mailName = inputStr.substring(0, seperatorPosition);
            mailSuffix = inputStr.substring(seperatorPosition + 1, inputStr.length());
        } else {
            mailName = inputStr;
        }
        // 超过两个字符提示，且不能为汉字
        if (mailName != null && mailName.length() > 1 && isAsciiChars(mailName)) {
            /* QAccountEditText在LoginView和RegisterEmailView中用到
             * 在LoginView中才会给mContainer赋值，而在RegisterEmailView不会
             * 所以在使用mContainer前先判断其值是否为空，也可通过判断其是否为空，来判断当前是谁在调用
             */
            String savedQID = ""; // 注册、登录成功后的帐号的QID
            if (mContainer!=null && isLoginStat) {
                QihooAccount[] accounts = mContainer.getUiAccounts().getSavedAccounts(context);
                if (accounts != null) {
                    for (QihooAccount account : accounts) {
                        if (account != null && !TextUtils.isEmpty(account.mQID)) {
                            // 判断mQID是否以mailName开头
                            if (account.mQID.startsWith(mailName)) {
                                savedQID = account.mQID;
                                dropdownList.add(savedQID);
                            }
                        }
                    }
                }
            }
            //登录场景
            if(isLoginStat ){
            	if(inputStr.contains(SEPERATOR)){
            		mailName += SEPERATOR;
            		for (int i = 0, len = mailList.size(); i < len; i++) {
                        EmailCount obj = mailList.get(i);
                        if (obj.email.startsWith(mailSuffix)) {
                            String item = mailName + obj.email;
                            if (!item.equals(savedQID)) { // Fix bug：避免在下拉列表中出现两条相同记录(历史记录、联想结果)
                                dropdownList.add(item);
                            }
                        }
                    }
            	}
            	 
            }else{
            	// 第一个提示项就是用户输入的内容
                if (mailSuffix.equals("")) { // Fix bug：从帐号字符串中间输入或删除字符时，下拉联想的框抖动
                    dropdownList.add(mailName);
                }
            	mailName += SEPERATOR;
                for (int i = 0, len = mailList.size(); i < len; i++) {
                    EmailCount obj = mailList.get(i);
                    if (obj.email.startsWith(mailSuffix)) {
                        String item = mailName + obj.email;
                        if (!item.equals(savedQID)) { // Fix bug：避免在下拉列表中出现两条相同记录(历史记录、联想结果)
                            dropdownList.add(item);
                        }
                    }
                }
            }
            
        }
    }

    private static final boolean isAsciiChars(final String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0 || c > 0x7f) {
                return false;
            }
        }
        return true;
    }

    private void getScreenInfo() {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
        density = dm.density;
    }

    public Editable getText() {
        return autoComplete.getText();
    }

    @Override
    public IBinder getWindowToken() {
        return autoComplete.getWindowToken();
    }

    public void setText(String value) {
        lastInputStr = value;
        autoComplete.setText(value);
    }

    public boolean setFocus() {
        return autoComplete.requestFocus();
    }

    public void setSelectedCallback(SelectedCallback callback) {
        dropDownItemSelectedCallback = callback;
    }

    public void setClearedCallback(ClearedCallback callback) {
        dropDownItemClearedCallback = callback;
    }

    public void setDropDownWidth(int width) {
        if (LOGE_ENABLED) {
            Log.d(TAG, "set width : " + width + " AutoComplete width : " + autoComplete.getMeasuredWidth());
        }
        autoComplete.setDropDownWidth(width);

        int border = (int) (density / 1.5);
        if (border < 1) {
            border = 1;
        }
        autoComplete.setDropDownHorizontalOffset(autoComplete.getMeasuredWidth() - width + border);

        autoCompleteSetWidthFlag = true;
    }

    public void setDropDownHeight(int height) {
        autoComplete.setDropDownHeight(height);
    }

    public void setDropDownAnchor(int id) {
        autoComplete.setDropDownAnchor(id);
    }

    public void statAccount() {
        String account = autoComplete.getText().toString();
        saveLoggedAccount(account);

        int seperatorPosition = account.indexOf(SEPERATOR);
        if (seperatorPosition == -1) {
            return;
        }

        String mailSuffix = account.substring(seperatorPosition + 1, account.length());
        boolean flag = false;
        for (int i = 0, len = mailList.size(); i < len; i++) {
            EmailCount obj = mailList.get(i);
            if (mailSuffix.equals(obj.email)) {
                obj.count++;
                flag = true;
                break;
            }
        }

        if (!flag) {
            int defautCount = 1;
            mailList.add(new EmailCount(mailSuffix, defautCount));
        }
        saveMailList();
    }

    public void clearLastLoggedAccount() {
        saveLoggedAccount("");
    }

    public void setOnEditorAction(OnEditorActionListener listener) {
        autoComplete.setOnEditorActionListener(listener);
    }

    private void checkAccountInfoSP() {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("account_info", Context.MODE_PRIVATE);
        }
    }

    private void saveLoggedAccount(String account) {
        checkAccountInfoSP();
        Editor edit = sharedPreferences.edit();
        edit.putString(spfAccount, account);
        edit.commit();
    }

    private String readLoggedAccount() {
        checkAccountInfoSP();
        return sharedPreferences.getString(spfAccount, "");
    }

    private String serialize(ArrayList<EmailCount> list) {
        String str = "";
        if (list.isEmpty()) {
            return str;
        }

        for (EmailCount item : list) {
            str += item.email + "|" + item.count + "||";
        }

        return str.substring(0, str.length() - 2);
    }

    private ArrayList<EmailCount> unSerialize(String str) {
        // 126.com|1||163.com|0||qq.com|0||sina.com.cn|0||sohu.com|0||gmail.com|0||hotmail.com|0||live.cn|0||yeah.net|0||yahoo.com.cn|0
        if (str == null || "".equals(str)) {
            return getDefaultMailList();
        }

        ArrayList<EmailCount> list = new ArrayList<EmailCount>();
        String[] items = str.split("\\|\\|");
        for (int i = 0, len = items.length; i < len; i++) {
            String[] item = items[i].split("\\|");
            list.add(new EmailCount(item[0], Integer.valueOf(item[1])));
        }
        return list;
    }

    private void saveMailList() {
        checkAccountInfoSP();
        Editor edit = sharedPreferences.edit();
        edit.putString(spfMailList, serialize(mailList));
        edit.commit();
    }

    private ArrayList<EmailCount> readMailList() {
        checkAccountInfoSP();
        String configMailListStr = sharedPreferences.getString(spfMailList, "");
        return unSerialize(configMailListStr);
    }

    private ArrayList<EmailCount> getDefaultMailList() {
        ArrayList<EmailCount> list = new ArrayList<EmailCount>();
        for (int i = 0, len = defaultMailList.length; i < len; i++) {
            list.add(new EmailCount(defaultMailList[i], 0));
        }
        return list;
    }

    private ArrayList<EmailCount> sort(ArrayList<EmailCount> sortList) {
        // Collections.sort(sortList, new Comparator<EmailCount>() {
        // public int compare(EmailCount obj1, EmailCount obj2) {
        // return obj2.count - obj1.count;
        // }
        // });
        // return sortList;

        // --------------------------------------------------------------
        // 以上代码不能满足需求， 如两个邮箱使用次数一样， 最近使用的邮箱靠前显示

        ArrayList<EmailCount> tmpList = new ArrayList<EmailCount>();
        for (int i = 0, len = sortList.size(); i < len; i++) {
            EmailCount item = sortList.get(i);
            if (item.count <= 0) {
                continue;
            }

            int tmpLen = tmpList.size(), idx = tmpLen;
            for (int j = 0; j < tmpLen; j++) {
                EmailCount tmpItem = tmpList.get(j);
                if (tmpItem.count <= item.count) {
                    idx = j;
                    break;
                }
            }
            tmpList.add(idx, item);

            sortList.remove(i);
            i--;
            len--;
        }
        sortList.addAll(0, tmpList);
        return sortList;
    }

    public class EmailCount {
        public String email;

        public int count;

        public EmailCount(String email, int count) {
            this.email = email;
            this.count = count;
        }
    }

    /**
     * 根据用户名从服务器删除指定帐号
     */
    private void removeAccountByName(String strName) {
        QihooAccount[] accounts = mContainer.getUiAccounts().getSavedAccounts(context);
        if (accounts != null) {
            for (QihooAccount account : accounts) {
                if (account != null && account.mQID.equals(strName)) {
                    mContainer.getUiAccounts().removeAccount(context, account);
                }
            }
        }
    }

    public AutoCompleteTextView getTextView() {
        return autoComplete;
    }
}
