package in.srain.cube.views.ptr.demo.ui.classic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import in.srain.cube.mints.base.TitleBaseFragment;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultRefreshLoadHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrLoadHandler;
import in.srain.cube.views.ptr.PtrRefreshHandler;
import in.srain.cube.views.ptr.demo.R;

public class WithWebView extends TitleBaseFragment {

    private PtrClassicFrameLayout mPtrFrame;
    private WebView mWebView;

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setHeaderTitle(R.string.ptr_demo_block_web_view);

        final View contentView = inflater.inflate(R.layout.fragment_classic_header_with_webview, null);
        mWebView = (WebView) contentView.findViewById(R.id.rotate_header_web_view);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mPtrFrame.refreshComplete();
            }
        });
        mPtrFrame = (PtrClassicFrameLayout) contentView.findViewById(R.id.rotate_header_web_view_frame);
        mPtrFrame.setLastUpdateTimeRelateObject(this);
        mPtrFrame.setPtrRefreshHandler(new PtrRefreshHandler() {
            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultRefreshLoadHandler.checkContentCanBePulledDown(frame, mWebView, header);
            }

            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                updateData();
            }
        });

        mPtrFrame.setPtrLoadHandler(new PtrLoadHandler() {
            @Override
            public boolean checkCanDoLoad(PtrFrameLayout frame, View content, View footer) {
                return PtrDefaultRefreshLoadHandler.checkContentCanBePulledUp(frame,mWebView,footer);
            }

            @Override
            public void onLoadBegin(PtrFrameLayout frame) {
                Toast.makeText(getActivity(),"我是footer！！",Toast.LENGTH_SHORT).show();

                mPtrFrame.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPtrFrame.loadComplete();

                    }
                },0);
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
        return contentView;
    }

    private void updateData() {
        mWebView.loadUrl("http://www.liaohuqiu.net/");
    }
}