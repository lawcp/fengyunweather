package com.fengyunweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by HandsomeXu on 2017/3/8.
 */

public class Suggestion {
    @SerializedName("comf")
    public Comfort comfort;

    @SerializedName("cw")
    public CarWash carWash;

    public Sport sport;

    @SerializedName("drsg")
    public  Dress dress;

    public class Comfort{

        @SerializedName("txt")
        public String info;
    }
    public class CarWash{
        @SerializedName("txt")
        public String info;
    }
    public class Sport{
        @SerializedName("txt")
        public String info;
    }
    public class Dress{
        @SerializedName("txt")
        public String info;
    }
}
