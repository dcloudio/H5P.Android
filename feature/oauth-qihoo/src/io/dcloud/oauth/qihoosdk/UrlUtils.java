package io.dcloud.oauth.qihoosdk;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.text.TextUtils;


public class UrlUtils {

    private static final String APP_INFO_GUSESS_HOST = "http://recommend.api.sj.360.cn/";
    private static final String APP_INFO_RELATIVE_APPS = "mintf/getRecommandAppsForDetail?";
    private static final String UPDATE_HOST = "http://update.api.sj.360.cn/";
    // 服务器域名
    public static String HOST_REL = "http://openbox.mobilem.360.cn/";
    private static String RECOMMEND_HOST_REL = "http://recommend.api.sj.360.cn/";
    // 测试服务器域名
    private static String HOST_TEST = "http://test1.baohe.mobilem.360.cn/";
    private static final String CDN_HOST = "http://openbox.mobilem.360.cn/";
    private static final String CORP_WORKS_URL = "AppStore/getAppsbyCorp?";

    /**详情页使用新的接口*/
    private static String APP_INFO_PATH_BY_PNAME = "iservice/getAppDetail?&market_id=360market&webp=0&sort=1";
    // 本地包更新
 	private static String APP_CHECK_PATH = "mintf/getAppsByPackNames?src=appstore&s_3pk=1&";
    //是否使用测试服务器
    public static final boolean IS_USE_TEST_HOST = false;//ConfigUtils.isUseTestHost(ContextUtils.getApplicationContext());
    private static final int SDK_VERSION = android.os.Build.VERSION.SDK_INT;
    public static String OS = "&os=" + SDK_VERSION;
    private static String MID = "";

    public static String TOPIC_HOST = "http://topic.api.sj.360.cn/"; //专题相关

    /** 公共后缀，代表webview_requirelogin。以此参数结尾的statTag，注销时会自动释放掉 */
    public static final String STAT_TAG_REQUIRE_LOGIN_SUFFIX = "_wvrl";

    //生活助手服务端域名
    private static final String LIFE_HELPER_HOST = "http://profile.sj.360.cn";
    private static final String LIFE_HELPER_HOST_TEST = "http://pre.profile.sj.360.cn";

    private static final String URL_PRIVILEGE_PAGE = "html/idhua/mprivilege.html?app=m_mobile&titlebar_space=1";

    //返回生活助手正式环境或测试环境的服务端域名
    private static String getLifeHelperHost() {
        return IS_USE_TEST_HOST ? LIFE_HELPER_HOST_TEST : LIFE_HELPER_HOST;
    }

    //返回正式环境或测试环境的服务端域名
    public static String getHost(){
        return IS_USE_TEST_HOST ? HOST_TEST : CDN_HOST;
    }

    //返回正式环境或测试环境的服务端域名(用CDN).
    public static String getCdnHost(){
        return IS_USE_TEST_HOST ? HOST_TEST : CDN_HOST;
    }

    // 免流量
    public static String FREE_HOST = "http://free.api.sj.360.cn/"; //免流量相

    //用户登录后保存的cookie值，需要用到的域名
    public final static String [] loginCookieDomains = new String[]{
            IS_USE_TEST_HOST ? "test1.baohe.mobilem.360.cn" : "openbox.mobilem.360.cn" ,
            "test.comment.mobilem.360.cn",
            "comment.mobilem.360.cn",
            "profile.openapi.360.cn",
            "pre.profile.sj.360.cn",
            "profile.sj.360.cn",
            "pre.profile.sj.360.cn",
            "test1.baohe.mobilem.360.cn"
    };

    public static String getAppInfoUrlById(String sId){
    	return new StringBuffer().append(IS_USE_TEST_HOST? HOST_TEST : HOST_REL).append(APP_INFO_PATH_BY_PNAME).append("&si=").append(sId).toString();
    }

    public static String getAppInfoUrlByPName(String pkgName){
        return new StringBuffer().append(IS_USE_TEST_HOST ? HOST_TEST : HOST_REL).append(APP_INFO_PATH_BY_PNAME).append("&pname=").append(pkgName).toString();
    }

    /**
     * 详情页猜你喜欢
     * @return
     */
    public static String getGuessLikeApps() {
        return new StringBuilder(IS_USE_TEST_HOST ? HOST_TEST : APP_INFO_GUSESS_HOST).append(APP_INFO_RELATIVE_APPS).append("webp=0&").toString();
    }

    public static String getCorpWorksUrl() {
        return CDN_HOST + CORP_WORKS_URL;
    }

    public static String getCommentsUrl(String appid, int level, int startIndex, int count) {
        StringBuilder buffer = new StringBuilder("http://comment.mobilem.360.cn/comment/getComments?");
        buffer.append("baike=").append(appid).append("&");
        buffer.append("level=").append(level).append("&");
        buffer.append("start=").append(startIndex).append("&");
        buffer.append("count=").append(count);
        return buffer.toString();
    }

    public static String getCommentsUrlByType(String appId, String type, String typeValue, int startIndex, int count){
        StringBuilder buffer = new StringBuilder("http://comment.mobilem.360.cn/comment/getComments?");
        buffer.append("baike=").append(appId).append("&");
        buffer.append(type).append("=").append(typeValue).append("&");
        buffer.append("start=").append(startIndex).append("&");
        buffer.append("count=").append(count);
        return buffer.toString();
    }

    public static String getWeightCommentsUrl(String appId, String type, int count) {
        StringBuilder buffer = new StringBuilder("http://comment.mobilem.360.cn/comment/getWeightComments?");
        buffer.append("objid=").append(appId).append("&");
        buffer.append("objtype=").append(type).append("&");
        buffer.append("count=").append(String.valueOf(count));
        return buffer.toString();
    }

    public static String getScoreUrl(String appId, String type) {
        StringBuilder buffer = new StringBuilder("http://comment.mobilem.360.cn/comment/getScore?");
        buffer.append("objid=").append(appId).append("&");
        buffer.append("objtype=").append(type);
        return buffer.toString();
    }

    public static String getCommentTagsUrl(String sId){
        StringBuilder builder = new StringBuilder("http://comment.mobilem.360.cn/comment/getCommentTags?objid=");
        builder.append(sId);
        return builder.toString();
    }

    /**
     * 获取评论点赞接口
     */
    public static String getCommentsLikeCountUrl(String appid, List<String> tids) {
        StringBuilder buffer = new StringBuilder("http://comment.mobilem.360.cn/comment/getLikes?");
        buffer.append("objid=").append(appid).append("&");
        buffer.append("objtype=0").append("&");
        buffer.append("tids=");
        int size = tids.size();
        for (int i = 0; i < size; i++) {
            buffer.append(tids.get(i));
            if (i != size - 1) {
                buffer.append(",");
            }
        }
        return buffer.toString();
    }

    /**
     * 获取评论回复接口
     */
    public static String getCommentsUrlReplUrl(String appid, List<String> tids, int page, int size) {
        StringBuilder buffer = new StringBuilder("http://comment.mobilem.360.cn/comment/getReplies?");
        buffer.append("baike=").append(appid).append("&");
        buffer.append("page=").append(page).append("&");
        if (size > 0) {
            buffer.append("size=").append(size).append("&");
        }
        buffer.append("tids=");
        int tidSize = tids.size();
        for (int i = 0; i < tidSize; i++) {
            buffer.append(tids.get(i));
            if (i != tidSize - 1) {
                buffer.append(",");
            }
        }
        return buffer.toString();
    }

    /**
     * 提交点赞接口
     */
    public static String gerDoLikeUrl(String appid, String tid) {
        StringBuilder buffer = new StringBuilder("http://comment.mobilem.360.cn/comment/doLike?");
        buffer.append("objid=").append(appid).append("&");
        buffer.append("objtype=0").append("&");
        buffer.append("tid=").append(tid);
        return buffer.toString();
    }

    /**
     * 获取自升级接口
     * @return
     */
    public static String getQueryUpdateUrl(Context context, String src, String toId, boolean bForceUpdate, boolean bk) {
    	StringBuilder builder = new StringBuilder();
        builder.append(IS_USE_TEST_HOST ? HOST_TEST : UPDATE_HOST);
        builder.append("AppStore/getIsUpdate?ext=zip");
    	builder.append("&pname=").append(context.getPackageName());
		builder.append("&sr=").append(android.os.Build.VERSION.RELEASE);
		builder.append("&mysrc=appstore");
        builder.append("&toid=").append(toId);
        if(bk){
            builder.append("&re=sil1");
            builder.append("&bk=1");
        }else{
            builder.append("&re=").append(src);
        }
        if(bForceUpdate){
            builder.append("&forceupdate=1");
        }
        return builder.toString();
    }
    
    /**
     * 获取应用升级信息接口
     * @param
     * @return
     */
    public static String getAppCheckUrl(int verCode) {
        StringBuilder builder = new StringBuilder();
        builder.append(IS_USE_TEST_HOST ? HOST_TEST : UPDATE_HOST);
        builder.append(APP_CHECK_PATH).append(MID);
//        builder.append("ver=").append(verCode);
        return builder.toString();
    }

    public static String appendSdkVersionAndWebPParam(String originUrl) {
        return appendSdkVersionParam(originUrl);
    }

    private static String appendSdkVersionParam(String originUrl) {
        if (originUrl == null) {
            return "";
        }
        if (originUrl.contains("&os=")) {
            return originUrl;
        } else if (originUrl.endsWith("?")) {
            return originUrl + "os=" + SDK_VERSION;
        } else if (originUrl.contains("?")) {
            return originUrl + "&os=" + SDK_VERSION;
        } else {
            return originUrl + "?os=" + SDK_VERSION;
        }
    }

//	private static String appendVersionParam(String originUrl) {
//		return originUrl + "&vc=" + UrlSegment.getSegment(UrlSegment.APPVERCODE) + "&v=" + UrlSegment.getSegment(UrlSegment.APPVERNAME);
//	}
//
//	@SuppressWarnings("deprecation")
//	private static String appendCpuMode(String originUrl){
//		String CPU_AND_MODEL = "";
//		try {
//			CPU_AND_MODEL = String.format("md=%s&sn=%s&cpu=%s&ca1=%s&ca2=%s", UrlSegment.getSegment(UrlSegment.MODEL), UrlSegment.getSegment(UrlSegment.LCDSIZE), UrlSegment.getSegment(UrlSegment.CPU), URLEncoder.encode(Build.CPU_ABI, HTTP.UTF_8),
//					URLEncoder.encode(Build.CPU_ABI2, HTTP.UTF_8));
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//
//		return originUrl + "&" + CPU_AND_MODEL;
//	}
//	
//	private static String appendIMEI(String originUrl) {
//		return originUrl + "&m=" + UrlSegment.getSegment(UrlSegment.IMEI) + "&m2=" + UrlSegment.getSegment(UrlSegment.IMEI2);
//	}
//	
//	private static String appendChannel(String originUrl) {
//		return originUrl + "&ch=" + UrlSegment.getSegment(UrlSegment.CHANNEL);
//	}
//
//	private static String appendScreenSize(String originUrl) {
//		return originUrl + "&ppi=" + UrlSegment.getSegment(UrlSegment.SCREENSIZE);
//	}
//	
//	private static String appendStartCount(String originUrl) {
//		return originUrl + "&startCount=" + UrlSegment.getSegment(UrlSegment.STARTCOUNT);
//	}
//
//    private static String appendOther(String originUrl) {
//        return originUrl + "&re=" + StatConst.startype + "&cpc=1" + "&snt=" + NetUtils.getSubNetType() + "&timestamp=" + System.currentTimeMillis() + "&nt=" + NetApnManager.getInstance().getNetApnConfig().getNetType();
//    }

    public static boolean isUseTestHost(){
        return IS_USE_TEST_HOST;
    }
	
//	public static String appendAllSegment(String url){
//		url = appendSdkVersionParam(url);
//		url = appendVersionParam(url);
//		url = appendCpuMode(url);
//		url = appendIMEI(url);
//		url = appendChannel(url);
//		url = appendScreenSize(url);
//		url = appendStartCount(url);
//		url = appendOther(url);
//		return url;
//	}

    //请求接口 prepage= curpage= 表示上一级页面 和 当前页面
    //一个页面有若干接口、如加载更多 只有一处带上 prepage=&curpage=
    public static String appendStatPage(String url, String pre, String cur){
        if (url == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        if (url.endsWith("?")) {
            sb.append("prepage=");
        } else if (!url.contains("?")) {
            sb.append("?prepage=");
        } else
            sb.append("&prepage=");

        sb.append(pre);
        sb.append("&curpage=");
        sb.append(cur);
        return sb.toString();
    }
	
    public static String getHotWordsUrlStrategyBNative(String type) {
        StringBuilder sb = new StringBuilder();
        if(isUseTestHost()){
            sb.append(HOST_TEST);
        }else{
            sb.append(HOST_REL);
        }
        sb.append("HotWord/hotWord?type="+type);
        return sb.toString();
    }

	public static String getSuggestUrl() {
		return "http://sug.m.so.com/suggest/zhushou?src=ms_zhushou";
	}

    public static String getRecommendUrl() {
        return (IS_USE_TEST_HOST ? HOST_TEST : RECOMMEND_HOST_REL) + "inew/getRecomendApps?iszip=1&logo_type=2&deflate_field=1&bannertype=1&apiversion=2";
    }
    
    // 榜单首页url
    public static String getRankMainUrl() {
        return (IS_USE_TEST_HOST ? HOST_TEST : RECOMMEND_HOST_REL) + "rank/index?";
    }

    // 游戏榜单首页url
    public static String getGameRankHomePageUrl() {
        return (IS_USE_TEST_HOST ? HOST_TEST : RECOMMEND_HOST_REL) + "rank/gameIndex?";
    }

    // 软件榜单首页url
    public static String getSoftRankHomePageUrl() {
        return (IS_USE_TEST_HOST ? HOST_TEST : RECOMMEND_HOST_REL) + "rank/softIndex?";
    }
    
	// &withext=1 为了返回长的一句话简介，和一副图描述
	public static String getGameRecomendUrl() {
         return (IS_USE_TEST_HOST ? HOST_TEST : RECOMMEND_HOST_REL) + "AppStore/getRecomendAppsBytype?type=2&withext=1";
	}
		
	// &withext=1 为了返回长的一句话简介，和一副图描述，添加参数时为了减少不必要的开销
	public static String getSoftRecomendUrl() {
		return (IS_USE_TEST_HOST ? HOST_TEST : RECOMMEND_HOST_REL) + "AppStore/getRecomendAppsBytype?type=1&withext=1";
	}

	public static String getEssentialUrl() {
		return "http://fake.api.mobilem.360.cn/weishi/bibei?src=appstore&withext=1";
	}

	//游戏分类
    public static String getCategoryGameUrl() {
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "app/getCatTags/cid/2?ver_type=1";
    }

    //TODO

    //软件分类
	public static String getCategorySoftUrl() {
		return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "app/getCatTags/cid/1?ver_type=1";
	}
	
	//分类页tab跳转
    public static String getCategoryGotoUrls() {    
		return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "app/list/cid/%s/format/webview?tag=%s";
    }

    public static String getCategoryGameDetailUrl(){
        if (IS_USE_TEST_HOST) {
            return "http://test1.baohe.mobilem.360.cn/app/getTagAppList?cid=%1$s&tag=%2$s&s_3pk=1";
        } else {
            return "http://openbox.mobilem.360.cn/app/getTagAppList?cid=%1$s&tag=%2$s&s_3pk=1";
        }
    }
    
    //常用分类
	public static String getFavorUrl(String cid){		
		return "http://api.rec.zhushou.360.cn/guesstag/FavorTag?cid1="+cid;
	}
	
	//软件榜单
	public static String getSoftRanksUrl() {		
		return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "rank/apps?from=soft";
	}

    //游戏榜单
    public static String getGameRanksUrl() {
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "app/rank?from=game";
    }

    //网游
    public static String getOnLineGameUrl() {
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "html/onlinegame/";
    }

    //女生频道
    public static String getFemaleChannel() {
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "html/nv/girls.html?showTitleBar=0&webpg=nspd";
    }

    //学习
    public static String getStudy() {
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "html/learn/index.html?webpg=rjxxpd";
    }

    //首发
    public static String getFirstRelease() {
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "qcms/view/t/first_release";
    }

    //儿童
    public static String getChildren() {
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "html/kids/index.html?webpg=kidspage";
    }

    public static String getGiftBag() {
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "html/onlinegame/giftpackcenter/giftlist.html?appid=%s";
    }

    //崩溃日志上传
//	public static String getCrashReportUrl(){
//		return String.format("http://mobmgr.dump.360.cn/upload_dump.php?mid=appstore_%s&ver=%s", UrlSegment.getSegment(UrlSegment.IMEI), UrlSegment.getSegment(UrlSegment.APPVERCODE));
//	}
	
	public static String getSearchTextHotWord(){
		return "http://openbox.mobilem.360.cn/AppStore/getHotWordsIconsOfSearch";
	}
	
	/**
	 * 发送崩溃日志接口
	 * sign      签名  ,签名算法: md5(imei+errorlog)    //+ 表示拼接,并无加号
	 * @param context
	 * @return
	 */
//	public static String getSendErrorLogUrl(Context context){
//		String imei2 = UrlSegment.getSegment(UrlSegment.IMEI2);
//		return HOST_REL+"intf/getErrorLog?m2="+imei2+"&sign="+Md5Utils.md5(imei2+"errorlog").toLowerCase();
//	}
	
	public static String getAboutMeUrl(int nIndex){
		String[] urls = {"http://openbox.mobilem.360.cn/html/aqsc/user_deal_statement/index.html",
				"http://openbox.mobilem.360.cn/html/aqsc/privacy_statement/index.html",				
				"http://openbox.mobilem.360.cn/html/aqsc/user_exp_plan/index.html"};
		
		if (nIndex < urls.length) {
			return urls[nIndex];
		}
		return "";
	}

    public  static String getAppFeedbackUrl(){
        return "http://care.help.360.cn/care/upload";
    }

    /**
     * 获得举报地址
     */
    public static String getReportUrl() {
        return "http://zhushou.360.cn/Jubao/boxindex?webpg=eyyyjby";
    }

    public static String getAppInfoTagSearchUrl(Context context, String tag, String cid) {
        StringBuilder builder = new StringBuilder();
        builder.append(IS_USE_TEST_HOST ? HOST_TEST : HOST_REL);
        builder.append("app/list/order/weekpure/format/webview2?tag=");
        builder.append(URLEncoder.encode(tag));
        builder.append("&cid=");
        builder.append(cid);
        return builder.toString() ;
    }

    public static String getAppHistoryVersionDataUrl(String sid,String current_ver){

        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "detail/apphistorydl?sid=" +
                sid + "&current_ver=" + current_ver + "&format=json";

    }

    public static String getSearchBaseUrl() {
        return "http://zonghe.m.so.com/api/search/";
    }
    /**
     * 获取360清理大师的ap
     * @return
     */
    public static String getClearInstallerInfo() {
        return IS_USE_TEST_HOST ? HOST_TEST : CDN_HOST + "index/qinglidashi";
    }

//    /**
//     * 搜索下载相关推荐
//     * @param appId
//     * @return
//     */
//    public static String getSearchRecommendUrl(String appId) {
//        StringBuffer buffer = new StringBuffer(getSearchBaseUrl());
//        buffer.append("apptuijian?src=ms_zhushou&s=0&n=20&id=");
//        buffer.append(appId);
//        return appendAllSegment(buffer.toString());
//    }
    /**
     * 获取360手机卫士的ap
     * @return
     */
    public static String getWeishiInstallerInfo() {
        return CDN_HOST + "/index/weishidetail";
    }

    /**
     * 应用圈-关注页
     */
    public static String getAppGroupFocusUrl() {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/followContList");
        return sb.toString();
    }
    /**
     * 应用圈-发现页
     */
    public static String getAppGroupDiscoverUrl() {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("appGroup/discover?formattype=1");
        return sb.toString();
    }

    /**
     * 应用圈-圈子详情页
     */
    public static String getAppGroupRingDetailUrl(String id) {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("AppGroup/groupInfo?group_id=").append(id).append("&forceNoCache=1");
        return sb.toString();
    }

//    /**
//     * 应用圈-订阅-数据的url
//     */
//    public static String getAppGroupMyUrl(String selectId) {
//        StringBuilder sb = new StringBuilder();
//        if (IS_USE_TEST_HOST) {
//            sb.append(HOST_TEST);
//        } else {
//            sb.append(HOST_REL);
//        }
//        sb.append("AppGroup/mine?");
//        sb.append("m2=").append(UrlSegment.getSegment(UrlSegment.IMEI2));
//        sb.append("s_3pk=1");
//        sb.append("&formattype=1");
//        sb.append("&offline=1");
//        if (!TextUtils.isEmpty(selectId)) {
//            sb.append("&group_ids=").append(selectId);
//        }
//        return sb.toString();
//    }

//    /**
//     * 应用圈-订阅-普通卡片的局部刷新
//     */
//    public static String getAppGroupMyItemRefreshUrl(String id, int brief) {
//        StringBuilder sb = new StringBuilder();
//        if (IS_USE_TEST_HOST) {
//            sb.append(HOST_TEST);
//        } else {
//            sb.append(HOST_REL);
//        }
//        sb.append("daren/detail?").append("id=").append(id)
//                .append("&brief=").append(brief)
//                .append("&m2=").append(UrlSegment.getSegment(UrlSegment.IMEI2));
//        return sb.toString();
//    }

    /**
     * 应用圈-订阅- 点赞
     */
    public static String getAppGroupFavUrl(String m2, String aspn, int sv, int asvc, String sign, String id) {
        return getAppGroupPraiseUrl(true, m2, aspn, sv, asvc, sign, id);
    }

    /**
     * 用圈-订阅- 取消点赞
     */
    public static String getAppGroupDelFavUrl(String m2, String aspn, int sv, int asvc, String sign, String id) {
        return getAppGroupPraiseUrl(false, m2, aspn, sv, asvc, sign, id);
    }

    private static String getAppGroupPraiseUrl(boolean isPraise, String m2, String aspn, int sv, int asvc, String sign, String id) {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/");
        if (isPraise) {
            sb.append("fav?");
        } else {
            sb.append("delFav?");
        }
        sb.append("m2=").append(m2)
                .append("&aspn=").append(aspn)
                .append("&sv=").append(sv)
                .append("&asvc=").append(asvc)
                .append("&sign=").append(sign)
                .append("&recomm_id=").append(id)
                .append("&_=").append(System.currentTimeMillis());
        return sb.toString();
    }

    /**
     * 应用圈-判断是否有邀请码
     * @return
     */
    public static String getAppGroupChkBind() {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/chkBind");
        return sb.toString();
    }

    /**
     * 应用圈-绑定邀请码
     */
    public static String getBindSharedCodeUrl(String code) {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/bindCode?code=").append(code);
        return sb.toString();
    }

    /**
     * 首页-应用圈-应用分享接口
     */
    public static String getAppGroupShareAppUrl() {
        StringBuffer sb = new StringBuffer();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/recommend?");
        return sb.toString();
    }

    /**
     * 首页-应用圈-应用分享-标签接口
     */
    public static String getAppGroupShareAppTagUrl() {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("topic/getTag");
        return sb.toString();
    }

    /**
     * 应用圈-应用分享-自定义标签-默认数据url
     */
    public static String getAppGroupMyCustomTagDefault() {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("DiscoverTag/defaultSearch");
//        sb.append("?myQid=148800088&debug=1");

        return sb.toString();
    }

    /**
     * 应用圈-应用分享-自定义标签-搜索标签url
     */
    public static String getAppGroupMyCustomTagSearchUrl(String name) {
        String encodeName = name;
        try {
            encodeName = URLEncoder.encode(name, "utf-8");
        } catch (Exception e) {
            encodeName = name;
        }
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("DiscoverTag/search?name=").append(encodeName);
//        sb.append("&myQid=148800088&debug=1");
        return sb.toString();
    }

    /**
     * 应用圈-应用分享-自定义标签-保存标签url
     */
    public static String getAppGroupMyCustomTagSaveUrl(String name) {
        String encodeName = name;
        try {
            encodeName = URLEncoder.encode(name, "utf-8");
        } catch (Exception e) {
            encodeName = name;
        }
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("/DiscoverTag/save?name=").append(encodeName);
//        sb.append("&myQid=148800088&debug=1");
        return sb.toString();
    }

    /**
     * 应用圈-订阅-详情页
     */
    public static String getAppGroupMyDetailUrl(String id, boolean showCmmt, boolean showUser) {
        StringBuffer sb = new StringBuffer();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/detail?").append("id=").append(id)
                .append("&showTitleBar=0").append("&forceNoCache=1");
        if (showCmmt) {
            sb.append("&show_cmmt=1");
        }
        if (showUser) {
            sb.append("&show_user=1");
        }
        sb.append("&s_3pk=1");
        return sb.toString();
    }

    /***
     * 应用圈-标签管理-取得所有标签列表
     */
    public static String getAppGroupMyAllTagUrl() {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("appGroup/groups?get_joined=1");
        return sb.toString();
    }

    /***
     * 应用圈-取得推荐标签列表
     */
    public static String getAppGroupMyRecommendTagUrl() {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/getrecommtag");
        return sb.toString();
    }

    /**
     * 应用圈-设置标签
     */
    public static String getAppGroupMySetTagUrl(String tagIds) {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/setmyfavtag?ids=").append(tagIds);
        return sb.toString();
    }

    /**
     * 新版应用圈-发现页的url
     */
    public static String getAppGroupFindUrl() {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("topic/index");
        sb.append("?s_3pk=1");
        return sb.toString();
    }

    /**
     * 应用圈-发现tab-地区个性化页面
     */
    public static String getAppGroupDiscoverLocalUrl() {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("appGroup/localList?size=10");
        return sb.toString();
    }

    /**
     * 应用圈-发现tab-机型个性化页面
     */
    public static String getAppGroupDiscoverModelUrl() {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("appGroup/machineList?size=10");
        return sb.toString();
    }


    /**话题首页*/
    public static String getAppDiscoveryTopicUrl(String target) {
        if (TextUtils.isEmpty(target)) {
            target = "0";
        }

        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("topic/detail?showTitleBar=0&id=").append(target);
        return sb.toString();
    }

    /**
     * 话题，推荐详情页
     */
    public static String getTopicRecommendDetailUrl(String recomm_id) {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("topic/detail?showTitleBar=0&recomm_id=").append(recomm_id);
        return sb.toString();
    }

    public static String getTopicPageUrl(String topicId) {
        String url;
        if (IS_USE_TEST_HOST) {
            url = HOST_TEST + "zhuanti/mdetail?id=" + topicId + "&s_3pk=1";
        } else {
            url = TOPIC_HOST + "zhuanti/mdetail?id=" + topicId + "&s_3pk=1";
        }
        return url;
    }

    public static String getSpecialDetailPageUrl(String topic_id) {
        return (IS_USE_TEST_HOST ? HOST_TEST : TOPIC_HOST) + "zhuanti/mdetail?id=" + topic_id + "&s_3pk=1";
    }

    public static String getCard13DarenType6PageUrl(String topic_id){
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "topic/detail?id=" + topic_id ;
    }

    /**
     * 消息中心，获取未读的消息
     *
     * @param channel : 0 => 全部  1 => App评论  2 => 话题
     * @param clear : 0  => 不设置为已读   1 => 设置为已读
     */
    public static String getUnreadUserMsgUrl(int channel, int clear) {
        if (channel != 0 && channel != 1 && channel != 2) {
            channel = 0;
        }
        if (clear != 0 && clear != 1) {
            clear = 0;
        }

        StringBuffer sb = new StringBuffer();
        sb.append("http://comment.mobilem.360.cn/msg/getUnreadList?");
        sb.append("clear=").append(clear)
                .append("&channel=").append(channel);
        return sb.toString();
    }

    public static String getRecommendCategoryUrl() {
        if (IS_USE_TEST_HOST) {
            return HOST_TEST + "/app/getCollectionTags";
        } else {
            return CDN_HOST + "/app/getCollectionTags";
        }
    }

    /**
     * 返回此机型内置软件的信息，判断某些软件是否可以卸载
     * @return
     */
    public static String getMgrSystemAppUninstallUrl(String imei,String mid,String mod,String appver) {
        StringBuilder builder = new StringBuilder("http://intf1.zsall.mobilem.360.cn/intf/getUninstallSoft3?");
        builder.append("imei=").append(imei);
        builder.append("&mid=").append(mid);
        builder.append("&mod=").append(mod);
        builder.append("&appver=").append(appver);
        return builder.toString();
    }

    public static String getCommentRateUrl(String appid,String score){
        StringBuffer buffer=new StringBuffer();
        buffer.append("http://comment.mobilem.360.cn/comment/doScore?");
        buffer.append("baike=").append(appid).append("&");
        buffer.append("score=").append(score);
        return buffer.toString();
    }

    public static String getOneKeyInstallUrl() {
        if (IS_USE_TEST_HOST) {
            return HOST_TEST + "recommand/install?src=appstore";
        } else {
            return CDN_HOST + "recommand/install?src=appstore";
        }
    }


    public static String getNormalCommentUrl(String appId, String type, String level, String replyTo,
        String comment, String currentAppVersionName,String currentAppVersionCode, int score,boolean needhelp) {

        StringBuilder builder = new StringBuilder();
        builder.append("http://comment.mobilem.360.cn/comment/doPost?");

        builder.append("objid=").append(appId).append("&");
        builder.append("objtype=").append(type).append("&");
        builder.append("level=").append(level).append("&");
        builder.append("replyTo=").append(replyTo).append("&");
        try {
            comment = URLEncoder.encode(comment, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        builder.append("comment=").append(comment);
        builder.append("&cavn=" + currentAppVersionName);//当前版本versionname
        builder.append("&cavc=" + currentAppVersionCode);//当前版本versionCode
        builder.append("&score=" + score);
        builder.append("&needhelp=" + needhelp);
        return builder.toString();
    }

    private static final String APP_PATCH_PATH = "inew/getDiffAppsbyPacknames?";

//    public static String getAppPatchUrl() {
//        if (IS_USE_TEST_HOST)
//            return HOST_TEST + APP_PATCH_PATH + MID + "&ver=" + DeviceUtils.getVersionCode();
//        else
//            return UPDATE_HOST + APP_PATCH_PATH + MID + "&ver=" + DeviceUtils.getVersionCode();
//    }

    public static String getBaseTopicDetailUrl(String id) {
        if (IS_USE_TEST_HOST)
            return HOST_TEST + "topic/detail?id=" + id;
        else
            return UPDATE_HOST + "topic/detail?id=" + id;
    }

    /**
     * 从应用圈相关入口登录成功后，需绑定toid的url
     */
    public static String getAppGroupBindToidUrl(String toId) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(toId)) {
            if (IS_USE_TEST_HOST) {
                sb.append(HOST_TEST);
            } else {
                sb.append(HOST_REL);
            }
            sb.append("daren/bindtoid?toid=").append(toId);
        }
        return sb.toString();
    }

    public static String getPersonalCenterSetting(){
        if(IS_USE_TEST_HOST) {
            return "http://pre.profile.sj.360.cn/profile/setting/getsetting?";
        } else {
            return "http://profile.sj.360.cn/profile/setting/getsetting?";
        }
    }

    public static String getInstallAppGetAll(){
        if(IS_USE_TEST_HOST)
        {
            return "http://pre.profile.sj.360.cn/user/installedApp?type=get";
        }else{
            return "http://profile.sj.360.cn/user/installedApp?type=get";
        }

    }

    public static String getInstallAppAdd(){
        if(IS_USE_TEST_HOST)
        {
            return "http://pre.profile.sj.360.cn/user/installedApp?type=add";
        }else{
            return "http://profile.sj.360.cn/user/installedApp?type=add";
        }

    }

    public static String getInstallAppDel()
    {
        if(IS_USE_TEST_HOST)
        {
            return "http://pre.profile.sj.360.cn/user/installedApp?type=del";
        }else
        {
            return "http://profile.sj.360.cn/user/installedApp?type=del";
        }

    }

    /**
     * 获取生活助带打点信息的url
     * */
    public static String getLifeUrl(String fm){
        return String.format(getHost() + "html/shzs/start.html?fm=%s&t=%d&showTitleBar=1", fm, new Random().nextInt(10000));
    }

//    public static String getUserFeedUrl(String qid) {
//        StringBuffer buffer;
//        if (IS_USE_TEST_HOST) {
//            buffer = new StringBuffer("http://pre.profile.sj.360.cn/user/feed?qid=");
//        } else {
//            buffer = new StringBuffer("http://profile.sj.360.cn/user/feed?qid=");
//        }
//        buffer.append(qid);
//        buffer.append(StatUrlParamsHelper.appendPageViewParams(buffer.toString()));
//        return buffer.toString();
//    }

    public static String getDelMyRecommUrl(String sharedid) {
        StringBuffer buffer = new StringBuffer();
        if (IS_USE_TEST_HOST) {
            buffer.append("http://test1.baohe.mobilem.360.cn/daren/delMyRecomm?id=");
        } else {
            buffer.append("http://openbox.mobilem.360.cn/daren/delMyRecomm?id=");
        }
        buffer.append(sharedid);
        return buffer.toString();
    }

    public static String getDarenPersonalPageUrl(String qid){
        StringBuffer buffer=new StringBuffer();
        if(IS_USE_TEST_HOST)
        {
            buffer.append("http://test1.baohe.mobilem.360.cn/daren/homepage?size=20&formattype=1");
        }else{
            buffer.append("http://openbox.mobilem.360.cn/daren/homepage?size=20&formattype=1");
        }
        buffer.append("&qid=").append(qid);
        return buffer.toString();
    }

    /**
     * 应用圈，关注\粉丝列表url
     * type = fans / follow
     */
    public static String getAppGroupFocusUrl(String type, String userQid) {
        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(userQid)) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/memlist?");
        sb.append("size=20");
        sb.append("&type=").append(type);
        sb.append("&user_qid=").append(userQid);
        return sb.toString();
    }

    /**
     * 关注
     */
    public static String getAppGroupDoFocusUrl(String user_qid) {
        StringBuffer sb = new StringBuffer();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/follow?user_qid=").append(user_qid);
        return sb.toString();
    }

    /**
     * 取消关注
     */
    public static String getAppGroupDoUnFocusUrl(String user_qid) {
        StringBuffer sb = new StringBuffer();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/unfollow?user_qid=").append(user_qid);
        return sb.toString();
    }

    public static String getUserCollectsUrl(){
        StringBuffer buffer=new StringBuffer();
        if(IS_USE_TEST_HOST){
            buffer.append("http://test1.baohe.mobilem.360.cn/index/collectpname/type/get");
        }else{
            buffer.append("http://openbox.mobilem.360.cn/index/collectpname/type/get");
        }
        return buffer.toString();
    }

    public static String getUserAddCollectUrl(){
        StringBuffer buffer=new StringBuffer();
        if(IS_USE_TEST_HOST){
            buffer.append("http://test1.baohe.mobilem.360.cn/index/collectpname/type/update/pnames/");
        }else{
            buffer.append("http://openbox.mobilem.360.cn/index/collectpname/type/update/pnames/");
        }
        return buffer.toString();
    }

    public static String getUserDelCollectUrl(){
        StringBuffer buffer=new StringBuffer();
        if(IS_USE_TEST_HOST){
            buffer.append("http://test1.baohe.mobilem.360.cn/index/collectpname/type/del/pnames/");
        }else{
            buffer.append("http://openbox.mobilem.360.cn/index/collectpname/type/del/pnames/");
        }
        return buffer.toString();
    }

    /**
     * 取得用户的关注相关信息：
     */
    public static String getAppGroupFocusInfoUrl(String user_qid) {
        StringBuilder sb = new StringBuilder();
        if (IS_USE_TEST_HOST) {
            sb.append(HOST_TEST);
        } else {
            sb.append(HOST_REL);
        }
        sb.append("daren/followinfo?user_qid=").append(user_qid);
        return sb.toString();
    }

    public static String getAppNewsAndStratrgyUrl(int id){
        return new StringBuilder(IS_USE_TEST_HOST ? HOST_TEST : HOST_REL)
                .append("gameInfo/detail?id=").append(id).toString();
    }

    /**
     * 生活助手，绑定手机号——账号是否绑定了手机号码的url
     */
    public static String getPhoneInfoUrl() {
        StringBuffer sb = new StringBuffer();
        sb.append(getLifeHelperHost()).append("/live/get-userinfo");
        return sb.toString();
    }
    
    public static String getPhoneInfoUrl2(){
    	StringBuffer sb = new StringBuffer();
        sb.append(getLifeHelperHost()).append("/raffle/sh_raffle/has-phone");
        return sb.toString();
    }

    /**
     * 生活助手，绑定手机号码——获取手机验证码url
     */
    public static String getVerityCodeUrl() {
        StringBuffer sb = new StringBuffer();
        sb.append(getLifeHelperHost()).append("/live/get-vc");
        return sb.toString();
    }

    /**
     * 生活助手，绑定手机号——提交绑定手机号url
     */
    public static String getBindPhoneNumUrl() {
        StringBuffer sb = new StringBuffer();
        sb.append(getLifeHelperHost()).append("/live/set-phone");
        return sb.toString();
    }

    /**
     *开机获取节日画面
     */
    public static String getFestivalPicUrl() {
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "inew/banner/ver/";
    }

    /**
     * 生活助手，获取cp菜单信息url
     * */
    public static String getCpMenuUrl() {
        StringBuffer sb = new StringBuffer();
        sb.append(getLifeHelperHost()).append("/live/service/get-menu");
        return sb.toString();
    }

    /**
     * 获取所有礼包列表url
     */
    public static String getGiftListUrl() {
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + URL_PRIVILEGE_PAGE+"&webpg=gift" + STAT_TAG_REQUIRE_LOGIN_SUFFIX;
    }

//    public static String getLifeClickUrl(String url, String kw, String input, String title) {
//        String resultUrl = "http://res.qhsetup.com/zhushou/redirectshzs.php?";
//        if (!TextUtils.isEmpty(kw)) {
//            kw = URLEncoder.encode(kw);
//        }
//        if (!TextUtils.isEmpty(input)) {
//            input = URLEncoder.encode(input);
//        }
//        if (!TextUtils.isEmpty(title)) {
//            title = URLEncoder.encode(title);
//        }
//        String params = StatUrlParamsHelper.appendPageViewParams(resultUrl);
//        resultUrl = resultUrl + params + "&kw=" + kw + "&inp=" + input + "&title=" + title + "&src=ms_zhushou" + "&type=shzssearch";
//
//        try {
//            url = URLEncoder.encode(url, "utf-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        return resultUrl + "&resurl=" + url;
//    }

    //获取新手引导描述
    public static String getUserGuideDesc(String model) {
        return (IS_USE_TEST_HOST ? "http://api.recommend.zhushou.corp.qihoo.net:8360/GuidePage/index?" :
                "http://api.rec.zhushou.360.cn/GuidePage/index?") + "model=" + model;
    }

//    public static String getUserGuideAppgroup() {
//        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "appGroup/recommGroup?m2=" + UrlSegment.getSegment(UrlSegment.IMEI2);
//    }
//
//    public static String getUserGuideAppgroupSelected(String groupIds) {
//        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "appGroup/setmyGroup?m2=" + UrlSegment.getSegment(UrlSegment.IMEI2) + "&ids=" + groupIds;
//    }

    public static String getEntertainmentUrl(){
        return (IS_USE_TEST_HOST ? HOST_TEST : HOST_REL) + "Iservice/Yule";
    }


//    private static String getConfigUrl() {
//        StringBuilder sb = new StringBuilder();
//        try {
//            String model = UrlSegment.getSegment(UrlSegment.MODEL);
//            String imei = UrlSegment.getSegment(UrlSegment.IMEI);
//            int sdk = Build.VERSION.SDK_INT;
//            String appver = UrlSegment.getSegment(UrlSegment.APPVERCODE);
//            String channel = UrlSegment.getSegment(UrlSegment.CHANNEL);
//            String imei2 = UrlSegment.getSegment(UrlSegment.IMEI2);
//            sb.append("m=").append(imei).append("&m2=").append(imei2).append("&md=").append(model).append("&sk=").append(sdk).append("&v=").append(appver)
//                    .append("&ch=").append(channel);
//        } catch (Exception e) {
//        }
//        return sb.toString();
//    }

//    public static String getHongbaoCheckUrl(String strId) {
//        if (IS_USE_TEST_HOST) {
//            return HOST_TEST + "hongbao/appCanDraw?sid=" + strId + "&" + getConfigUrl();
//        } else {
//            return CDN_HOST + "hongbao/appCanDraw?sid=" + strId + "&" + getConfigUrl();
//        }
//    }

    public static String getHongbaoWebUrl() {
        if (IS_USE_TEST_HOST) {
            return HOST_TEST + "hongbao/index4?showTitleBar=0&";
        } else {
            return CDN_HOST + "hongbao/index4?showTitleBar=0&";
        }
    }

    public static String getHongbaoGuideUrl() {
        if (IS_USE_TEST_HOST) {
            return HOST_TEST + "html/hongbao/privacy4.html?showTitleBar=0";
        } else {
            return CDN_HOST + "html/hongbao/privacy4.html?showTitleBar=0";
        }
    }

    public static String getHongbaoMoneyUrl() {
        if (IS_USE_TEST_HOST) {
            return HOST_TEST + "hongbao/draw?";
        } else {
            return CDN_HOST + "hongbao/draw?";
        }
    }

    public static String getHdIdUrl() {
        if (IS_USE_TEST_HOST) {
            return HOST_TEST + "hongbao/getAppHuodong";
        } else {
            return CDN_HOST + "hongbao/getAppHuodong";
        }

    }
    
    /**
     * 获取需要拦截ip的域名列表url*/
    public static String getRemoveTipsDomainlistUrl() {
        StringBuffer sb = new StringBuffer();
        sb.append(getHost()).append("webviewjs/domainlist");
        return sb.toString();
    }

    /**获取拦截tip的规则和js的url*/
    public static String getRomoveTipsRuleAndJsUrl() {
        StringBuffer sb = new StringBuffer();
        sb.append(getHost()).append("webviewjs/rule");
        return sb.toString();
    }


    public static String getCloudCheckUrl() {
        String url = "https://openbox.mobilem.360.cn/AppStore/check?url=";
        if (IS_USE_TEST_HOST) {
            url = "http://test1.baohe.mobilem.360.cn/AppStore/check?url=";
        }
        return url;

    }

    /**
     * 获得分享链接
     * @param sid
     * @return
     */
    public static String getShareUrl(String sid){
        StringBuilder sb = new StringBuilder();
        if(!TextUtils.isEmpty(sid)){
            sb.append(getHost()+"qcms/view/t/detail?sid="+sid);
        }
        return sb.toString();
    }

    public static String urlEncode(String content){
        if(TextUtils.isEmpty(content)){
            return content;
        }
        try {
            return URLEncoder.encode(content, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return content;
    }
}
