package com.legend.manage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.google.gson.Gson;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadmoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.yanzhenjie.nohttp.NoHttp;
import com.yanzhenjie.nohttp.RequestMethod;
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
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private final int SIZE = 10;
    private final String  EVENTFLAG_PROGRESS = "progress";


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
    UploadManager mUploadManager;
    String filePath = "";

    private int page = 1;
    List<FileInfo> mList;
    MyAdapter mAdapter;


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
        initSwipeMenuList();
    }


    private void initData() {
        mList = new ArrayList<>();
        mUploadManager = new UploadManager();
        mAdapter = new MyAdapter(this, R.layout.item_file_list, mList);
        mListview.setAdapter(mAdapter);
        httpGetList();
    }

    private void initSwipeMenuList() {
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
                deleteItem.setWidth(com.legend.manage.DensityUtil.dip2px(MainActivity.this, 90));
                // set a icon
                deleteItem.setIcon(R.drawable.icon_delete);
                // add to menu
                menu.addMenuItem(deleteItem);
            }
        };
        mListview.setMenuCreator(creator);
        mListview.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);
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
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        httpDelete(mList.get(position).getId(), position);
//                        Toast.makeText(MainActivity.this, "删除", Toast.LENGTH_SHORT).show();
                        break;
                }
                return false;
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                Uri uri = data.getData();
                filePath = uri.getPath().toString();
                Toast.makeText(this, "文件路径：" + uri.getPath().toString(), Toast.LENGTH_SHORT).show();
                mText.setText(uri.getPath().toString());
            }
        }
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

    private void httpGetToken() {
        final Request<JSONObject> request = NoHttp.createJsonObjectRequest(ConnectUrl.GET_TOKEN, RequestMethod.GET);
        mQueue.add(0, request, new OnResponseListener<JSONObject>() {
            @Override
            public void onStart(int what) {

            }

            @Override
            public void onSucceed(int what, Response<JSONObject> response) {
                if (what == 0) {
                    JSONObject object = response.get();
                    try {
                        token = object.getString("uptoken");
                        Log.d("token", "onSucceed: " + token);
                        if (!token.isEmpty()) {
                            uploadFile();
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

            }
        });
    }

    private void httpDelete(int id, final int position) {
        final Request<JSONObject> request = NoHttp.createJsonObjectRequest(ConnectUrl.FILE + "/" + id, RequestMethod.DELETE);
        mQueue.add(0, request, new OnResponseListener<JSONObject>() {
            @Override
            public void onStart(int what) {

            }

            @Override
            public void onSucceed(int what, Response<JSONObject> response) {
                if (what == 0) {
                    JSONObject object = response.get();
                    try {
                        int status = object.getInt("status");
                        if (status == 0) {
                            mList.remove(position);
                            mAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(MainActivity.this, "删除失败，请重试！", Toast.LENGTH_SHORT).show();
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

            }
        });
    }

    private void uploadFile() {
        int last = filePath.lastIndexOf("/") + 1;
        final String name = filePath.substring(last);
        String upKey = UUID.randomUUID().toString() + "_" + name;
        Log.d("upkey", "uploadFile: " + upKey);
        mUploadManager.put(filePath, upKey, token, new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                if (info.isOK()) {
                    Log.i("qiniu", "Upload Success");
                    try {
                        httpUploadFinleInfo(response.getString("key"), name);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    mProgressTV.setText("上传失败");
                    Log.i("qiniu", "Upload Fail");
                    //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                }
//                Log.i("qiniu", key + ",\r\n " + info + ",\r\n " + response);
            }
        }, new UploadOptions(null, "test-type", true, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                EventBus.getDefault().post(new EventFlag(EVENTFLAG_PROGRESS,percent));
                Log.i("qiniu", key + ": " + percent);

            }
        }, null));
    }

    @Subscribe
    public void onEvent(EventFlag flag) {
        if (flag.getFlag().equals(EVENTFLAG_PROGRESS)){
            double percent = (double) flag.getObject();
            Log.d("EventFlag", "上传进度：" +percent * 100 + "%");
            mProgressTV.setText("上传进度：" +percent * 100 + "%");
        }

    }

    private void httpUploadFinleInfo(String url, String name) {
        final Request<JSONObject> request = NoHttp.createJsonObjectRequest(ConnectUrl.FILE, RequestMethod.POST);
        request.add("url", url);
        request.add("remark", name);
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
                            mProgressTV.setText("上传成功");
                            mList.clear();
                            page = 1;
                            httpGetList();
                        } else {
                            Toast.makeText(MainActivity.this, "上传失败!", Toast.LENGTH_SHORT).show();
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

            }
        });
    }

    @OnClick({R.id.select_file, R.id.upload})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.select_file:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
                break;
            case R.id.upload:
                httpGetToken();
                break;
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

