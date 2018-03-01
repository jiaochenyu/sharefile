package com.legend.fileoperation;

import java.io.Serializable;

/**
 * Created by JCY on 2018/2/27.
 * 说明：
 */

public class FileInfo implements Serializable {

    /**
     * id : 1
     * url : http://p4omv8kyb.bkt.clouddn.com/icon.png
     * remark : 这里是备注
     * download : 3
     * createTime : 2018-02-25 09:37:44
     */

    private int id;
    private String url;
    private String remark;
    private int download;
    private String createTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getDownload() {
        return download;
    }

    public void setDownload(int download) {
        this.download = download;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
