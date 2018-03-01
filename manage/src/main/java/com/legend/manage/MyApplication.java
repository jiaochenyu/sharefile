package com.legend.manage;

import android.app.Application;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.storage.Configuration;
import com.yanzhenjie.nohttp.InitializationConfig;
import com.yanzhenjie.nohttp.Logger;
import com.yanzhenjie.nohttp.NoHttp;

/**
 * Created by JCY on 2018/2/27.
 * 说明：
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        InitializationConfig config = InitializationConfig.newBuilder(this)
//                .addHeader("Content-Type", "multipart/form-data; boundary=-----------------------------264141203718551") // 全局请求头。
                .connectionTimeout(30 * 1000)
                .readTimeout(30 * 1000)
                .retry(10)
                .build();
        NoHttp.initialize(config);
        Logger.setDebug(true);// 开启NoHttp// 的调试模式, 配置后可看到请求过程、日志和错误信息。
        Logger.setTag("NoHttpSample");

        Configuration configuration = new Configuration.Builder()
                .chunkSize(512 * 1024)        // 分片上传时，每片的大小。 默认256K
                .putThreshhold(1024 * 1024)   // 启用分片上传阀值。默认512K
                .connectTimeout(10)           // 链接超时。默认10秒
                .useHttps(true)               // 是否使用https上传域名
                .responseTimeout(60)          // 服务器响应超时。默认60秒
                .zone(FixedZone.zone0)        // 设置区域，指定不同区域的上传域名、备用域名、备用IP。
                .build();

    }
}
