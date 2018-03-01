package com.legend.fileoperation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.google.gson.Gson;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadmoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.yanzhenjie.nohttp.Headers;
import com.yanzhenjie.nohttp.NoHttp;
import com.yanzhenjie.nohttp.RequestMethod;
import com.yanzhenjie.nohttp.download.DownloadRequest;
import com.yanzhenjie.nohttp.download.SimpleDownloadListener;
import com.yanzhenjie.nohttp.download.SyncDownloadExecutor;
import com.yanzhenjie.nohttp.rest.OnResponseListener;
import com.yanzhenjie.nohttp.rest.Request;
import com.yanzhenjie.nohttp.rest.RequestQueue;
import com.yanzhenjie.nohttp.rest.Response;
import com.zhy.adapter.abslistview.CommonAdapter;
import com.zhy.adapter.abslistview.ViewHolder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private final int SIZE = 10;

    @BindView(R.id.upload)
    Button mUploadBtn;
    @BindView(R.id.listview)
    SwipeMenuListView mListview;
    @BindView(R.id.text)
    TextView mText;
    @BindView(R.id.select_file)
    Button mSelectFile;
    @BindView(R.id.progress)
    TextView mProgressTV;
    @BindView(R.id.refresh)
    SmartRefreshLayout mRefreshLayout;


    RequestQueue mQueue = NoHttp.newRequestQueue();
    String token = "";

    String filePath = "";

    private int page = 1;
    List<FileInfo> mList;
    MyAdapter mAdapter;

    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    mText.setText("开始下载");
                    break;
                case 1:
                    int progress = msg.getData().getInt("progress");
                    EventBus.getDefault().post(new EventFlag("progress",progress));
                    break;
                case 2: {
                    EventBus.getDefault().post(new EventFlag("finish","finish"));
                    break;
                }
                default:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        initView();
        initData();
        initListener();
    }

    private void initView() {
        SwipeMenuCreator creator = new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                        0x3F, 0x25)));
                // set item width
                deleteItem.setWidth(DensityUtil.dip2px(MainActivity.this, 90));
                // set a icon
                deleteItem.setIcon(R.drawable.icon_download);
                deleteItem.setTitle("下载");
                // add to menu
                menu.addMenuItem(deleteItem);
            }
        };
        mListview.setMenuCreator(creator);
        mListview.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);
    }

    private void initData() {
        mList = new ArrayList<>();
        mAdapter = new MyAdapter(this, R.layout.item_file_list, mList);
        mListview.setAdapter(mAdapter);
        httpGetList();
    }

    private void initListener() {
        mRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                page = 1;
                mList.clear();
                httpGetList();

            }
        });
        mRefreshLayout.setOnLoadmoreListener(new OnLoadmoreListener() {
            @Override
            public void onLoadmore(RefreshLayout refreshlayout) {
                httpGetList();
            }
        });
        mListview.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                httpDownLoadFile(ConnectUrl.DOWNLOAD + mList.get(position).getId());

                            }
                        }).start();
                        break;
                }
                return false;
            }
        });

    }

    private void httpGetList() {
        final Request<JSONObject> request = NoHttp.createJsonObjectRequest(ConnectUrl.FILE, RequestMethod.GET);
        request.add("page", page);
        request.add("size", SIZE);
        mQueue.add(0, request, new OnResponseListener<JSONObject>() {
            @Override
            public void onStart(int what) {

            }

            @Override
            public void onSucceed(int what, Response<JSONObject> response) {
                if (what == 0) {
                    Gson gson = new Gson();
                    JSONObject object = response.get();
                    try {
                        int status = object.getInt("status");
                        if (status == 0) {
                            JSONArray array = object.getJSONArray("data");
                            for (int i = 0; i < array.length(); i++) {
                                FileInfo info = gson.fromJson(array.getJSONObject(i).toString(), FileInfo.class);
                                mList.add(info);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailed(int what, Response<JSONObject> response) {

            }

            @Override
            public void onFinish(int what) {
                if (page == 1) {
                    mRefreshLayout.finishRefresh();
                } else {
                    mRefreshLayout.finishLoadmore();
                }
                mAdapter.notifyDataSetChanged();
                page++;
            }
        });
    }


    public void httpDownLoadFile(String url) {
        String fileFolder = Environment.getExternalStorageDirectory().toString();
        DownloadRequest request = new DownloadRequest(url, RequestMethod.GET, fileFolder, true, false);
        SyncDownloadExecutor.INSTANCE.execute(0, request, new SimpleDownloadListener() {
            @Override
            public void onStart(int what, boolean resume, long range, Headers headers, long size) {
                // 开始下载，回调的时候说明文件开始下载了。
                // 参数1：what。
                // 参数2：是否是断点续传，从中间开始下载的。
                // 参数3：如果是断点续传，这个参数非0，表示之前已经下载的文件大小。
                // 参数4：服务器响应头。
                // 参数5：文件总大小，可能为0，因为服务器可能不返回文件大小。
//                mProgressDialog.show();
                mHandler.sendEmptyMessage(0);
            }

            @Override
            public void onProgress(int what, int progress, long fileCount, long speed) {
                // 进度发生变化，服务器不返回文件总大小时不回调，因为没法计算进度。
                // 参数1：what。
                // 参数2：进度，[0-100]。
                // 参数3：文件总大小，可能为0，因为服务器可能不返回文件大小。
                // 参数4：下载的速度，含义为1S下载的byte大小，计算下载速度时：
                //        int xKB = (int) speed / 1024; // 单位：xKB/S
                //        int xM = (int) speed / 1024 / 1024; // 单位：xM/S
                Log.e("下载进度", progress + "%");
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putInt("progress",progress);
                message.setData(bundle);
                message.what = 1;
                mHandler.sendMessage(message);
            }

            @Override
            public void onFinish(int what, String filePaths) {
                // 下载完成，参数2为保存在本地的文件路径。
                filePath = filePaths;

                mHandler.sendEmptyMessage(2 );

                Log.e("文件保存在", "onFinish: " + filePaths);

            }
        });
    }


    @Subscribe
    public void onEvent(EventFlag flag) {
        if (flag.getFlag().equals("progress")) {
            int progress = (int) flag.getObject();
            mProgressTV.setText("下载进度：" + progress + "%");
        }
        if (flag.getFlag().equals("finish")) {
            mText.setText("文件保存在：" + filePath);
        }
    }


    class MyAdapter extends CommonAdapter<FileInfo> {

        public MyAdapter(Context context, int layoutId, List<FileInfo> datas) {
            super(context, layoutId, datas);
        }

        @Override
        protected void convert(ViewHolder holder, FileInfo item, int position) {
            TextView timeTV = holder.getView(R.id.time);
            TextView filenameTV = holder.getView(R.id.file_name);
            TextView urlTV = holder.getView(R.id.url);
            TextView numTV = holder.getView(R.id.num);
            String time = "";
            try {
                time = item.getCreateTime();
                time = time.split(" ")[0];
            } catch (Exception e) {
                e.printStackTrace();
            }
            timeTV.setText(time);
            filenameTV.setText(item.getRemark());
            numTV.setText("下载次数：" + item.getDownload());
            urlTV.setText("地址：" + item.getUrl());
        }
    }

}
