# Android平台5+App/uni-app运行环境开源项目

**开源项目需与HBuilderX2.3.6-20191020配套使用**

## 模块与源码对应关系
| 文件夹						| 说明						| 5+APP项目			| uni-app项目		|
| :-------:					| :-------:					| :-------:			| :-------:			|
| feature/aps				        | 推送(基础库)                | [plus.push](https://www.html5plus.org/doc/zh_cn/push.html)                       | https://uniapp.dcloud.io/api/plugins/push |
| feature/aps-igexin		    | 个推推送                    | [plus.push](https://www.html5plus.org/doc/zh_cn/push.html)                       |
| feature/aps-igexin-gp		  | 个推推送(google play版)     | [plus.push](https://www.html5plus.org/doc/zh_cn/push.html)                       |
| feature/aps-unipush		    | unipush推送                 | [plus.push](https://www.html5plus.org/doc/zh_cn/push.html)                       |
| feature/aps-unipush-gp	  | unipush推送(google play版)  | [plus.push](https://www.html5plus.org/doc/zh_cn/push.html)                       |
| feature/aps-xiaomi		    | 小米推送					          | [plus.push](https://www.html5plus.org/doc/zh_cn/push.html)                       |
| feature/audio				      | 音频                        | [plus.audio](https://www.html5plus.org/doc/zh_cn/audio.html)                     | https://uniapp.dcloud.io/api/media/record-manager https://uniapp.dcloud.io/api/media/audio-context |
| feature/audio-mp3acc		  | 音频(MP3格式支持库)          | [plus.audio](https://www.html5plus.org/doc/zh_cn/audio.html)    |
| feature/barcode			      | 二维码                      | [plus.barcode](https://www.html5plus.org/doc/zh_cn/barcode.html)                 | https://uniapp.dcloud.io/api/system/barcode |
| feature/bluetooth			    | 低功耗蓝牙                  | [plus.bluetooth](https://www.html5plus.org/doc/zh_cn/bluetooth.html)             | https://uniapp.dcloud.io/api/system/bluetooth |
| feature/contacts			    | 通讯录                      | [plus.contacts](https://www.html5plus.org/doc/zh_cn/contacts.html) |
| feature/camera			      | 摄像头                      | [plus.camera](https://www.html5plus.org/doc/zh_cn/camera.html)     |
| feature/device			      | 设备信息				            | [plus.device](https://www.html5plus.org/doc/zh_cn/device.html)                   | https://uniapp.dcloud.io/api/system/info |
| feature/downloader		    | 文件下载					          | [plus.downloader](https://www.html5plus.org/doc/zh_cn/downloader.html)           | https://uniapp.dcloud.io/api/request/network-file?id=downloadfile |
| feature/file				      | 文件系统					          | [plus.io](https://www.html5plus.org/doc/zh_cn/io.html)                           | https://uniapp.dcloud.io/api/file/file |
| feature/fingerprint		    | 指纹识别                    | [plus.fingerprint](https://www.html5plus.org/doc/zh_cn/fingerprint.html)         | https://uniapp.dcloud.io/api/other/authentication |
| feature/ibeacon			      | iBeacon                    | [plus.ibeacon](https://www.html5plus.org/doc/zh_cn/ibeacon.html)                 | https://uniapp.dcloud.io/api/system/ibeacon |
| feature/geolacation		    | 定位(基础库)					       | [plus.geolocation](https://www.html5plus.org/doc/zh_cn/geolocation.html)         | https://uniapp.dcloud.io/api/location/location |
| feature/geolacation-amap	| 高德定位                    | [plus.geolocation](https://www.html5plus.org/doc/zh_cn/geolocation.html) |
| feature/geolacation-baidu	| 百度定位                    | [plus.geolocation](https://www.html5plus.org/doc/zh_cn/geolocation.html) |
| feature/geolacation-system| 系统定位                    | [plus.geolocation](https://www.html5plus.org/doc/zh_cn/geolocation.html) |
| feature/map-amap			    | 高德地图                    | [plus.map](https://www.html5plus.org/doc/zh_cn/maps.html)                         | https://uniapp.dcloud.io/api/location/map |
| feature/map-baidu			    | 百度地图                    | [plus.map](https://www.html5plus.org/doc/zh_cn/maps.html)                         | https://uniapp.dcloud.io/api/location/map |
| feature/media				      | 视频播放                    | [plus.video.VideoPlayer](https://www.html5plus.org/doc/zh_cn/video.html#plus.video.VideoPlayer)      | https://uniapp.dcloud.io/api/media/video |
| feature/media-livepusher	| 直播推流                    | [plus.video.LivePusher](https://www.html5plus.org/doc/zh_cn/video.html#plus.video.LivePusher)       | https://uniapp.dcloud.io/api/media/live-player-context |
| feature/messaging			    | 短彩邮件消息                | [plus.messaging](https://www.html5plus.org/doc/zh_cn/messaging.html)     |
| feature/navigatorui		    | 浏览器运行环境信息           | [plus.navigator](https://www.html5plus.org/doc/zh_cn/navigator.html)             | https://uniapp.dcloud.io/api/system/info |
| feature/oauth				      | 登录鉴权(基础库)						 | [plus.oauth](https://www.html5plus.org/doc/zh_cn/oauth.html)                     | https://uniapp.dcloud.io/api/plugins/login |
| feature/oauth-miui		    | 小米登录                    | [plus.oauth](https://www.html5plus.org/doc/zh_cn/oauth.html) |
| feature/oauth-qq			    | qq登录                      | [plus.oauth](https://www.html5plus.org/doc/zh_cn/oauth.html) |
| feature/oauth-sina		    | 新浪微博登录                | [plus.oauth](https://www.html5plus.org/doc/zh_cn/oauth.html) |
| feature/oauth-weixin		  | 微信登录                    | [plus.oauth](https://www.html5plus.org/doc/zh_cn/oauth.html)  |
| feature/payment			      | 支付(基础库)	               | [plus.payment](https://www.html5plus.org/doc/zh_cn/payment.html)                 | https://uniapp.dcloud.io/api/plugins/payment |
| feature/payment-alipay	  | 支付宝支付                  | [plus.payment](https://www.html5plus.org/doc/zh_cn/payment.html) |
| feature/payment-weixin	  | 微信支付                    | [plus.payment](https://www.html5plus.org/doc/zh_cn/payment.html) |
| feature/share				      | 分享(基础库)                | [plus.share](https://www.html5plus.org/doc/zh_cn/share.html)                     | https://uniapp.dcloud.io/api/plugins/share |
| feature/share-qq			    | QQ分享                     | [plus.share](https://www.html5plus.org/doc/zh_cn/share.html) |
| feature/share-sina		    | 新浪微博分享                | [plus.share](https://www.html5plus.org/doc/zh_cn/share.html) |
| feature/share-weixin		  | 微信分享                   | [plus.share](https://www.html5plus.org/doc/zh_cn/share.html) |
| feature/speech			      | 语音识别(基础库)            | [plus.speech](https://www.html5plus.org/doc/zh_cn/speech.html)                   | https://uniapp.dcloud.io/api/plugins/voice |
| feature/speech_baidu		  | 百度语音识别				        | [plus.speech](https://www.html5plus.org/doc/zh_cn/speech.html) |
| feature/speech_ifly		    | 讯飞语音识别                | [plus.speech](https://www.html5plus.org/doc/zh_cn/speech.html) |
| feature/statistics-umeng	| 友盟统计                    | [plus.statistic](https://www.html5plus.org/doc/zh_cn/statistic.html) |
| feature/uploader			    | 文件上传					          | [plus.uploader](https://www.html5plus.org/doc/zh_cn/uploader.html)               | https://uniapp.dcloud.io/api/request/network-file?id=uploadfile |
| feature/xhr               | 网络请求                    | [plus.net](https://www.html5plus.org/doc/zh_cn/xhr.html)                         | https://uniapp.dcloud.io/api/request/request?id=request |
| feature/sqlite            | 数据库                      | [plus.sqlite](https://www.html5plus.org/doc/zh_cn/sqlite.html)  |
| feature/weex_amap				  | nvue原生组件: 高德地图       | 不支持 | https://uniapp.dcloud.io/component/map |
| feature/weex_barcode			| nvue原生组件: 二维码         | 不支持 | https://uniapp.dcloud.io/component/barcode |
| feature/weex_livepusher		| nvue原生组件: 推流           | 不支持 | https://uniapp.dcloud.io/component/live-pusher |
| feature/weex_videoplayer  | nvue原生组件: 视频           | 不支持 | https://uniapp.dcloud.io/component/live-player |
| feature/weex_gcanvas			| nvue原生组件: canvas         | 不支持 | https://github.com/dcloudio/NvueCanvasDemo |


## 源码配置

+ 文件夹中存在build.gradle文件的模块

将源码clone到本地，复制对应文件夹到所在项目的根目录，在setting.gradle中加入module配置，rebuild即可。以高德地图为例，将本工程clone到本地，复制map-amap到项目的根目录，打开setting.gradle，在最底部添加include "map-amap"，重新运行即可，如需添加到主工程中，打开主工程的build.gradle文件，在dependencies节点中添加implementation project(path: ':map-amap')即可。

+ 文件夹中不存在build.gradle文件的模块

如果模块中不存在build.gradle文件，可以将文件夹中的目录直接拷贝到主项目中。例如xhr模块，可以将src下的文件夹拷贝到主项目的src下，如果存在其他文件夹（如libs），直接将文件夹下的内容拷贝到同名文件夹下即可。


**参考HBuilder-SourceTool-as项目**

## 项目配置及打包

clone HBuilder-SourceTool-as项目到本地，直接运行即可。如果运行uni-app时出现问题，可参考：https://ask.dcloud.net.cn/article/35139。

安卓打包配置参考：https://www.cnblogs.com/lsdb/p/9337342.html。


# 许可协议
本工程大部分源码开源，使用者可以自主修改已公开的源码，编译新版本。
但注意：
1. 您不能破解、反向工程、反编译本项目下未开源的各种库文件。
2. 未经DCloud书面许可，您不得利用本项目的全部或部分源码、文件来制作与DCloud根据本项目提供的服务相竞争的产品，例如提供自主品牌的开发者服务。
3. DCloud所拥有的知识产权，包括但不限于商标、专利、著作权，并不发生转移或共享。
4. 您基于本项目，自主开发的代码及输出物，其知识产权归属您所有。除非您通过提交pull request的方式将自己的代码开源。
5. 如果您没有违反本许可协议，那么你使用本项目将无需为DCloud支付任何费用。
