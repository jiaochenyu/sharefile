package com.legend.fileoperation;

import android.app.Application;

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

    }
}
