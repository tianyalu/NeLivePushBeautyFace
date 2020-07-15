package com.dongnao.livedemo;

import android.Manifest;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.dongnao.livedemo.core.listener.RESConnectionListener;
import com.dongnao.livedemo.filter.hardvideofilter.BaseHardVideoFilter;
import com.dongnao.livedemo.filter.hardvideofilter.HardVideoGroupFilter;
import com.dongnao.livedemo.tools.PermissionUtils;
import com.dongnao.livedemo.ws.StreamAVOption;
import com.dongnao.livedemo.ws.StreamLiveCameraView;
import com.dongnao.livedemo.ws.filter.hardfilter.GPUImageBeautyFilter;
import com.dongnao.livedemo.ws.filter.hardfilter.WatermarkFilter;
import com.dongnao.livedemo.ws.filter.hardfilter.extra.GPUImageCompatibleFilter;

import java.util.LinkedList;


public class LiveActivity extends AppCompatActivity {
    private static final String TAG = LiveActivity.class.getSimpleName();
    private String[] needPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};
    private StreamLiveCameraView mLiveCameraView;
    private StreamAVOption streamAVOption;
//    private String rtmpUrl = "rtmp://txy.live-send.acg.tv/live-txy/?streamname=live_345162489_81809986&key=03693092c85bd15a1d3fbbc227da0ad1";
    private String rtmpUrl = "rtmp://47.115.6.127/myapp/";
    static {
        System.loadLibrary("native-lib");
    }

    private LiveUI mLiveUI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        if (!PermissionUtils.checkPermissions(this, needPermissions)) {
            PermissionUtils.requestPermissions(this, needPermissions);
        }else {
            init();
        }
    }

    private void init() {
        initLiveConfig();
        mLiveUI = new LiveUI(this,mLiveCameraView,rtmpUrl);
    }

    /**
     * 设置推流参数
     */
    public void initLiveConfig() {
        mLiveCameraView = findViewById(R.id.stream_previewView);

        //参数配置 start
        streamAVOption = new StreamAVOption();
        streamAVOption.streamUrl = rtmpUrl;
        //参数配置 end

        mLiveCameraView.init(this, streamAVOption);
        mLiveCameraView.addStreamStateListener(resConnectionListener);
        LinkedList<BaseHardVideoFilter> files = new LinkedList<>();
        files.add(new GPUImageCompatibleFilter(new GPUImageBeautyFilter()));
        mLiveCameraView.setHardVideoFilter(new HardVideoGroupFilter(files));
    }

    RESConnectionListener resConnectionListener = new RESConnectionListener() {
        @Override
        public void onOpenConnectionResult(int result) {
            //result 0成功  1 失败
            Toast.makeText(LiveActivity.this,"打开推流连接 状态："+result+ " 推流地址："+rtmpUrl, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onWriteError(int errno) {
            Toast.makeText(LiveActivity.this,"推流出错,请尝试重连", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCloseConnectionResult(int result) {
            //result 0成功  1 失败
            Toast.makeText(LiveActivity.this,"关闭推流连接 状态："+result, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLiveCameraView.destroy();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PermissionUtils.REQUEST_PERMISSIONS_CODE) {
            if (!PermissionUtils.verifyPermissions(grantResults)) {
                PermissionUtils.showMissingPermissionDialog(this);
            } else {
                init();
            }
        }
    }
}
