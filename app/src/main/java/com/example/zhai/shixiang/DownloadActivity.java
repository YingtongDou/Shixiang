package com.example.zhai.shixiang;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobGeoPoint;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;

public class DownloadActivity extends AppCompatActivity {
    ListView lv;
    SimpleAdapter sa;
    double maxDistance = 10.0;
    private double mLocationLongitude;
    private double mLocationLatitude;
    private ProgressDialog mDialog;
    private List<Map<String,Object>> mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        Bmob.initialize(this, "087ee08e4ffbd42c7602d0facd018f71");

        lv = (ListView)findViewById(R.id.downloadpiclist);

        lv.setDivider(new ColorDrawable(Color.GREEN));
        lv.setOnDragListener(new View.OnDragListener() {// 上拉刷新，下拉加载，可能不是这个监听器
            @Override
            public boolean onDrag(View v, DragEvent event) {
                return false;
            }
        });
        //注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(Common.LOCATION_ACTION);
        DownloadActivity.this.registerReceiver(new LocationBroadcastReceiver(), filter);
        Intent intent = new Intent();
        intent.setClass(DownloadActivity.this, LocationSvc.class);
        //Log.i("running","start over");
        DownloadActivity.this.startService(intent);
        mDialog = new ProgressDialog(DownloadActivity.this);
        mDialog.setMessage("正在定位...");
        mDialog.setCancelable(false);
        mDialog.show();
    }


    @Override
    protected void onPause() {
        super.onPause();

        lv.setAdapter(null);

    }

    private class LocationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(Common.LOCATION_ACTION)) return;
            mLocationLatitude = intent.getDoubleExtra(Common.LOCATION_LATITUDE,0);
            mLocationLongitude = intent.getDoubleExtra(Common.LOCATION_LONGITUDE,0);
//            mLoca.setText("纬度" + mLocationLatitude + "\n经度" + mLocationLongitude);

//            Bitmap bm = BitmapFactory.decodeByteArray(mData, 0, mData.length);
            //open database
//            MainDatabase od = new MainDatabase(MainActivity.this);
//            Date d = new Date();
//            od.open();
//            Log.i("time", d.toString());
//            long success = od.insertData(d.toString(), mLocationLatitude, mLocationLongitude, bm);
//            od.close();
//            final PicInfo upload = new PicInfo();
//            BmobGeoPoint point = new BmobGeoPoint(mLocationLongitude,mLocationLatitude);
//            List<Byte> pic = new ArrayList<Byte>();
//            for(int i=0;i<mData.length;i++){
//                pic.add(mData[i]);
//            }
            BmobGeoPoint point = new BmobGeoPoint(mLocationLongitude,mLocationLatitude);
            //查询一定地理范围内拍摄的照片
            mList = new ArrayList<Map<String,Object>>();//should get from server, format: List<Map<String, Object>>;

            BmobQuery<PicInfo> bmobQuery = new BmobQuery<PicInfo>();
            bmobQuery.addWhereWithinKilometers("gpsAdd",point ,maxDistance);
            bmobQuery.setLimit(10);    //获取最接近用户地点的10条数据
            bmobQuery.findObjects(DownloadActivity.this, new FindListener<PicInfo>() {
                @Override
                public void onSuccess(List<PicInfo> object) {
                    // TODO Auto-generated method stub
                    Toast.makeText(DownloadActivity.this, "查询成功：共" + object.size() + "条数据。",Toast.LENGTH_SHORT).show();
                    for(int i=0;i<object.size();i++){
                        Map<String, Object> map = new HashMap<String, Object>();
                        List<Byte> lData = object.get(i).getPic();
                        byte[] data = new byte[lData.size()];
                        for(int j=0;j<data.length;j++){
                            data[j] = lData.get(j);
                        }

                        Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);

                        Matrix m = new Matrix();
                        m.setRotate(90);
                        bm = Bitmap.createBitmap(bm,0,0,bm.getWidth(),bm.getHeight(),m,false);

                        map.put("img",bm);
                        map.put("timestamp",object.get(i).getPicTime());
                        map.put("latitude", object.get(i).getGpsAdd().getLatitude());
                        map.put("longitude", object.get(i).getGpsAdd().getLongitude());
                        mList.add(map);
                    }

                    sa = new SimpleAdapter(DownloadActivity.this,
                            mList,
                            R.layout.piclayout,
                            new String[]{"img","timestamp","latitude","longitude"},
                            new int[]{R.id.img, R.id.timestamp,R.id.latitude,R.id.longitude});
                    sa.setViewBinder(new SimpleAdapter.ViewBinder() {
                        @Override
                        public boolean setViewValue(View view, Object data,
                                                    String textRepresentation) {
                            // TODO Auto-generated method stub
                            if (view instanceof ImageView && data instanceof Bitmap) {
                                ImageView i = (ImageView) view;
                                i.setImageBitmap((Bitmap) data);
                                return true;
                            }
                            return false;
                        }
                    });
                    lv.setAdapter(sa);

                }
                @Override
                public void onError(int code, String msg) {
                    // TODO Auto-generated method stub
                    Toast.makeText(DownloadActivity.this, "查询失败：" + msg ,Toast.LENGTH_SHORT).show();
                }
            });






            mDialog.dismiss();
            DownloadActivity.this.unregisterReceiver(this);// 不需要时注销
        }
    }
}
