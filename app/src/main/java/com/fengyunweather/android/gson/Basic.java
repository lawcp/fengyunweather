package com.fengyunweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by HandsomeXu on 2017/3/8.
 */

public class Basic {
    /**
     * Json数据的一些字段不太适合作为Java字段，使用注解让其建立映射关系
     */
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
