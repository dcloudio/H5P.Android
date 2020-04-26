## 关于各个module的说明

为保证云打包环境与开源项目一致，故添加以下两个模块进行兼容。

+ lib.5plus.base

lib.5plus.base 对应离线SDK中的lib.5plus.base-release.aar，libs文件夹下android-gif-drawable-release@1.2.17.aar和dev-tools-base-release.aar为lib.5plus.base的基础库。

lib.5plus.base 中集成有camera、device、downloader、file、geolocation、geolocation-system、oauth、navigatorui、payment、uploader、xhr、share、audio功能，开发者可根据自己需求删减。

+ dc_weexsdk

**注意：因Android studio不能间接引用aar文件，所以在原基础上添加了一条引用。即feature下的sdk文件夹。**

+ app

app 模块为主项目，即可运行到手机上的app，app模块的build.gradle中集成了所有的功能模块，特殊需要注意的是因一个SDK可能同时存在与多个module之间，所以aar或jar资源需要单独引用。如微信sdk，qq sdk，sina微博sdk，百度地图sdk，高德地图sdk。特别需要注意的是百度地图so文件同时存在于定位和地图之中，使用是也许单独引用。

## 关于各个SDK对应的key相关信息的处理

app module中的build.gradle已经添加了关于key相关信息的配置，可参考注释适当增删。注意：build.gradle中并未配置真实数据，使用时务必替换成真实的信息。

## 关于setting.gradle

各个module在使用前必须在setting.gradle中注册，如果路径不是在主工程目录下，需要特殊配置。可参考根目录下的setting.gradle配置。


## 注意！！！

Android studio在生成aar时不会将libs下的aar文件放到对应项目的aar中，所以如果存在aar文件，需要单独引用，可参考app module的build.gradle 中的支付宝支付模块。



