package com.fengyunweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fengyunweather.android.db.City;
import com.fengyunweather.android.db.County;
import com.fengyunweather.android.db.Province;
import com.fengyunweather.android.gson.Weather;
import com.fengyunweather.android.util.HttpUtil;
import com.fengyunweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by HandsomeXu on 2017/3/6.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY =2;
    private ProgressDialog mProgressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private List<String> dataList = new ArrayList<>();
    /**
     * 省列表
     */
    private List<Province> mProvinceList;
    /**
     * 市列表
     */
    private List<City> mCityList;
    /**
     * 县列表
     */
    private List<County> mCountyList;
    /**
     * 选中的省份
     */
    private Province mSelectedProvince;
    /**
     * 选中的城市
     */
    private City mSelectedCity;
    /**
     * 当前选中的级别
     */
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        mListView = (ListView) view.findViewById(R.id.list_view);
        mAdapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);
        mListView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){
                    mSelectedProvince = mProvinceList.get(position);
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    mSelectedCity = mCityList.get(position);
                    queryCountise();
                }else if(currentLevel == LEVEL_COUNTY){
                    String weatherId = mCountyList.get(position).getWeatherId();
                    //判断该碎片在哪个活动里面
                    if(getActivity() instanceof  MainActivity){
                        Intent intent = new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.reuestWeather(weatherId);
                    }

                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }
    /**
     * 查询全国所有的省份，优先从数据库查询，如果没有查询到，再去服务器查询
     */
    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        mProvinceList = DataSupport.findAll(Province.class);
        if(mProvinceList.size() == 0)
        {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }else {
                dataList.clear();
                for(Province province : mProvinceList){
                    dataList.add(province.getProvinceName());
                }
                mAdapter.notifyDataSetChanged();
                mListView.setSelection(0);
                currentLevel = LEVEL_PROVINCE;
        }
    }



    /**
     * 查询选中省内的所有的市，优先从数据库查询，如果没有查询到，再去服务器查询
     */
    private void queryCities() {
        titleText.setText(mSelectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        mCityList = DataSupport.where("provinceid = ?",String.valueOf(mSelectedProvince.getId())).find(City.class);
        if(mCityList.size() == 0){
            int proviceCode = mSelectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+proviceCode;
            queryFromServer(address,"city");
        }else {
                dataList.clear();
                for (City city :mCityList){
                    dataList.add(city.getCityName());
                }
                mAdapter.notifyDataSetChanged();
                mListView.setSelection(0);
                currentLevel = LEVEL_CITY;
        }

    }
    /**
     * 查询选中的市内的所有的县，优先从数据库查询，如果没有，再去服务器查询
     */

    private void queryCountise(){
        titleText.setText(mSelectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        mCountyList = DataSupport.where("cityid = ?",String.valueOf(mSelectedCity.getId())).find(County.class);
        if(mCountyList.size() > 0){
            dataList.clear();
            for(County county : mCountyList){
                dataList.add(county.getCountyName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }
        else {
            int provinceCode = mSelectedProvince.getProvinceCode();
            int cityCode = mSelectedCity.getCityCode();
            String address ="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"county");
        }
    }
    /**
     * 根据传入的地址和类型，从服务器上查询省市县数据
     * @param address
     * @param type
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseContent = response.body().string();
                boolean result = false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseContent);
                }else if("city".equals(type)){
                    result = Utility.handleCityResponse(responseContent, mSelectedProvince.getId());
                }else if("county".equals(type)){
                    result = Utility.handleCountyResponse(responseContent,mSelectedCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            clossProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCountise();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread()返回主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        clossProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    /**
     * 显示进度条对话框
     */
    private void showProgressDialog(){
        if(mProgressDialog == null){
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }
    /**
     * 关闭进度条对话框
     */
    private void clossProgressDialog() {
        if(mProgressDialog != null){
            mProgressDialog.dismiss();
        }
    }
}
