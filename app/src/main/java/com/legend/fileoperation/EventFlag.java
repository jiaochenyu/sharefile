package com.legend.fileoperation;

import java.io.Serializable;

/**
 * Created by JCY on 2018/2/27.
 * 说明：
 */

public class EventFlag implements Serializable {
    String flag ;
    Object object;

    public EventFlag(String flag, Object object) {
        this.flag = flag;
        this.object = object;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}
