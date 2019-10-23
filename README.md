# H5P.Android

**开源工程适配HBuilderX2.3.6-20191020**

## 模块与源码对应关系
| 文件夹						| 说明						| 5+APP项目			| uni-app项目		|
| :-------:					| :-------:					| :-------:			| :-------:			|
| feature/aps				|推送的基础库					|plus.push.*		|https://uniapp.dcloud.io/api/plugins/push|
| feature/aps-igexin		|个推推送					|plus.push.*		|
| feature/aps-igexin-gp		|个推推送（google play版）	|plus.push.*		|
| feature/aps-unipush		|unipush推送					|plus.push.*		|
| feature/aps-unipush-gp	|unipush推送（google play版）|plus.push.*		|
| feature/aps-xiaomi		|小米推送					|plus.push.*		|
| feature/audio				|audio模块					|plus.audio.*		|https://uniapp.dcloud.io/api/media/record-manager https://uniapp.dcloud.io/api/media/audio-context|
| feature/audio-mp3acc		|audio模块MP3支持库			|plus.audio.*		|
| feature/barcode			|barcode模块					|plus.barcode.*		|https://uniapp.dcloud.io/api/system/barcode|
| feature/bluetooth			|蓝牙模块					|plus.bluetooth.*	|https://uniapp.dcloud.io/api/system/bluetooth|
| feature/contacts			|通讯录操作模块				|plus.contacts.*	|
| feature/camera			|camera模块					|plus.camera.*		|
| feature/device			|设备信息相关模块				|plus.device.*		|https://uniapp.dcloud.io/api/system/info|
| feature/downloader		|下载模块					|plus.downloader.*	|https://uniapp.dcloud.io/api/request/network-file?id=downloadfile|
| feature/file				|文件操作模块					|plus.io.*			|https://uniapp.dcloud.io/api/file/file|
| feature/fingerprint		|指纹识别模块					|plus.fingerprint.*	|
| feature/ibeacon			|ibeacon模块					|plus.ibeacon.*		|https://uniapp.dcloud.io/api/system/ibeacon|
| feature/geolacation		|定位基础模块					|plus.geolocation.*	|https://uniapp.dcloud.io/api/location/location|
| feature/geolacation-amap	|高德定位模块					|plus.geolocation.*	|
| feature/geolacation-baidu	|百度定位模块					|plus.geolocation.*	|
| feature/geolacation-system|系统定位模块					|plus.geolocation.*	|
| feature/map-amap			|高德地图模块					|plus.map.*			|https://uniapp.dcloud.io/api/location/map|
| feature/map-baidu			|百度地图模块					|plus.map.*			|
| feature/media				|videoplayer视频模块			|plus.video.*		|https://uniapp.dcloud.io/api/media/video|
| feature/media-livepusher	|livepusher推流模块			|plus.video.*		|https://uniapp.dcloud.io/api/media/live-player-context|
| feature/messaging			|通讯管理模块					|plus.messaging.*	|
| feature/navigatorui		|navigator模块				|plus.navigator.*	|https://uniapp.dcloud.io/api/system/info|
| feature/oauth				|授权登录模块					|plus.oauth.*		|https://uniapp.dcloud.io/api/plugins/login|
| feature/oauth-miui		|小米授权登录模块				|plus.oauth.*		|
| feature/oauth-qq			|qq授权登录模块				|plus.oauth.*		|
| feature/oauth-sina		|新浪微博授权登录模块			|plus.oauth.*		|
| feature/oauth-weixin		|微信授权登录模块				|plus.oauth.*		|
| feature/payment			|支付功能基础模块				|plus.payment.*		|https://uniapp.dcloud.io/api/plugins/payment|
| feature/payment-alipay	|支付宝支付模块				|plus.payment.*		|
| feature/payment-weixin	|微信支付模块					|plus.payment.*		|
| feature/share				|分享功能基础模块				|plus.share.*		|https://uniapp.dcloud.io/api/plugins/share|
| feature/share-qq			|QQ分享模块					|plus.share.*		|
| feature/share-sina		|新浪微博分享模块				|plus.share.*		|
| feature/share-weixin		|微信分享模块					|plus.share.*		|
| feature/speech			|语音识别基础模块				|plus.speech.*		|https://uniapp.dcloud.io/api/plugins/voice|
| feature/speech_baidu		|百度语音识别模块				|plus.speech.*		|
| feature/speech_ifly		|讯飞语音识别模块				|plus.speech.*		|
| feature/statistics-umeng	|统计模块					|plus.statistic.*	|
| feature/uploader			|上传模块					|plus.uploader.*	|https://uniapp.dcloud.io/api/request/network-file?id=uploadfile|
| feature/xhr				|网络请求模块					|plus.net.*			|
| feature/sqlite			|数据库操作模块				|plus.sqlite.*		|
| feature/weex_amap				|高德地图原生插件	|
| feature/weex_barcode			|barcode原生插件	|
| feature/weex_livepusher		|推流原生插件		|
| feature/weex_videoplayer		|视频原生模块		|
| feature/weex_gcanvas			|canvas原生模块	|


## 源码配置

+ 文件夹中存在build.gradle文件的模块

将源码clone到本地，复制对应文件夹到所在项目的根目录，在setting.gradle中加入module配置，rebuild即可。以高德地图为例，将本工程clone到本地，复制map-amap到项目的根目录，打开setting.gradle，在最底部添加include "map-amap"，重新运行即可，如需添加到主工程中，打开主工程的build.gradle文件，在dependencies节点中添加implementation project(path: ':map-amap')即可。

+ 文件夹中不存在build.gradle文件的模块

如果模块中不存在build.gradle文件，可以将文件夹中的目录直接拷贝到主项目中。例如xhr模块，可以将src下的文件夹拷贝到主项目的src下，如果存在其他文件夹（如libs），直接将文件夹下的内容拷贝到同名文件夹下即可。

## 项目配置及打包

clone示例项目到本地，直接运行即可。如果运行uni-app时出现问题，可参考：https://ask.dcloud.net.cn/article/35139。

安卓打包配置参考：https://www.cnblogs.com/lsdb/p/9337342.html。


# 许可协议
本工程大部分源码开源，使用者可以自主修改已公开的源码，编译新版本。
但注意：
1. 您不能破解、反向工程、反编译本项目下未开源的各种库文件。
2. 未经DCloud书面许可，您不得利用本项目的全部或部分源码、文件来制作与DCloud根据本项目提供的服务相竞争的产品，例如提供自主品牌的开发者服务。
3. DCloud所拥有的知识产权，包括但不限于商标、专利、著作权，并不发生转移或共享。
4. 您基于本项目，自主开发的代码及输出物，其知识产权归属您所有。除非您通过提交pull request的方式将自己的代码开源。
5. 如果您没有违反本许可协议，那么你使用本项目将无需为DCloud支付任何费用。
