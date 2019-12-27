package com.karl.kiosk.Interface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Result {

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @SerializedName("status")
    @Expose
    private String status;

    public String getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

    @SerializedName("msg")
    @Expose
    private String msg;

}
