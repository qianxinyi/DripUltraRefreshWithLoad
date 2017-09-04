package in.srain.cube.views.ptr.demo.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultRefreshLoadHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrLoadHandler;
import in.srain.cube.views.ptr.PtrRefreshHandler;
import in.srain.cube.views.ptr.demo.R;


/**
 * Created by Administrator on 2017/8/25.
 */

public class MainActivity extends Activity {

    private PtrClassicFrameLayout mPtrFrame;
    private ScrollView mScrollView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.fragment_classic_header_with_scroll_view);
        mScrollView = (ScrollView) findViewById(R.id.rotate_header_scroll_view);
        mPtrFrame = (PtrClassicFrameLayout) findViewById(R.id.rotate_header_web_view_frame);
        mPtrFrame.setLastUpdateTimeRelateObject(this);
        mPtrFrame.setPtrRefreshHandler(new PtrRefreshHandler() {
            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultRefreshLoadHandler.checkContentCanBePulledDown(frame, mScrollView, header);
            }

            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                Toast.makeText(MainActivity.this,"我是Header" ,Toast.LENGTH_SHORT).show();
                mPtrFrame.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPtrFrame.refreshComplete();
                    }
                }, 100);
            }
        });

        mPtrFrame.setPtrLoadHandler(new PtrLoadHandler() {
            @Override
            public boolean checkCanDoLoad(PtrFrameLayout frame, View content, View footer) {
                return PtrDefaultRefreshLoadHandler.checkContentCanBePulledUp(frame,mScrollView,footer);
            }

            @Override
            public void onLoadBegin(PtrFrameLayout frame) {

                Toast.makeText(MainActivity.this,"我是Footer" ,Toast.LENGTH_SHORT).show();

                mPtrFrame.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPtrFrame.loadComplete();
                    }
                }, 100);

            }
        });
        // the following are default settings
        mPtrFrame.setResistance(1.7f);
        mPtrFrame.setRatioOfHeight(1.2f);
        mPtrFrame.setDurationToClose(200);
        mPtrFrame.setDurationToCloseHeader(1000);
        // default is false
        mPtrFrame.setPullToRefresh(false);
        // default is true
        mPtrFrame.setKeepHeaderWhenRefresh(true);
        mPtrFrame.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPtrFrame.autoRefresh();
            }
        }, 100);
    }








}
