### 插件使用方法

* 在主`module`的`build.gradle`中加入
    ```apply plugin: 'com.netease.hearttouch.PresetResourcePlugin'```
    
* 在主`module`的`build.gradle`中加入

```
buildscript {
    //目前先发布在本地，后面会通过maven进行引用
    repositories {
//        maven {
//            url "http://mvn.hz.netease.com/artifactory/libs-releases/"
//        }
//        maven {
//            url "http://mvn.hz.netease.com/artifactory/libs-snapshots/"
//        }
        maven {
            url "./plugin"
        }
    }
    dependencies {
        classpath 'com.netease.hearttouch:candywebcache-plugin:0.0.1-SNAPSHOT' //添加依赖，后续发布后版本号会变
    }
}
```
* 在主`module`的`build.gradle`中加入
    
    - `url`:检测版本的`http`请求地址
    - `appID `:本应用注册在`CandyWebcache`的`native`应用标识
    - `appVersion `:本应用当前的版本号
    - `needDownload`:是否需要下载，默认设置为true，如果确定在`assembleRelease`的时候资源包没有更新，可以设置为false，加速打包过程

```
presetExt{
    url  'http://10.242.27.37:9001/api/version_check'
    appID 'KaoLa'
    appVersion   '1.2.3'
    needDownload true
}
```
    
