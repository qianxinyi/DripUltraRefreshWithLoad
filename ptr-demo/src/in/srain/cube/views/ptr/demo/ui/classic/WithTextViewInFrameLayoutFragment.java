package in.srain.cube.views.ptr.demo.ui.classic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import in.srain.cube.mints.base.TitleBaseFragment;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrLoadHandler;
import in.srain.cube.views.ptr.PtrRefreshHandler;
import in.srain.cube.views.ptr.demo.R;

public class WithTextViewInFrameLayoutFragment extends TitleBaseFragment {

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setHeaderTitle(R.string.ptr_demo_block_frame_layout);

        final View contentView = inflater.inflate(R.layout.fragment_classic_header_with_viewgroup, container, false);

        final PtrClassicFrameLayout ptrFrame = (PtrClassicFrameLayout) contentView.findViewById(R.id.fragment_rotate_header_with_view_group_frame);
        ptrFrame.setPtrRefreshHandler(new PtrRefreshHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                frame.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ptrFrame.refreshComplete();
                    }
                }, 1800);
            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return true;
            }
        });

        ptrFrame.setPtrLoadHandler(new PtrLoadHandler() {
            @Override
            public boolean checkCanDoLoad(PtrFrameLayout frame, View content, View footer) {
                return true;
            }

            @Override
            public void onLoadBegin(PtrFrameLayout frame) {
                frame.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ptrFrame.loadComplete();
                    }
                }, 1800);
            }
        });

        ptrFrame.setLastUpdateTimeRelateObject(this);

        // the following are default settings
        ptrFrame.setResistance(1.7f);
        ptrFrame.setRatioOfHeight(1.2f);
        ptrFrame.setDurationToOffset(200);
        ptrFrame.setDurationToClose(1000);
        // default is false
        ptrFrame.setPullToRefresh(false);
        // default is true
        ptrFrame.setKeepHeaderWhenRefresh(true);

        // scroll then refresh
        // comment in base fragment
        ptrFrame.postDelayed(new Runnable() {
            @Override
            public void run() {
                 ptrFrame.autoRefresh();
            }
        }, 150);

        setupViews(ptrFrame);

        return contentView;
    }

    protected void setupViews(final PtrClassicFrameLayout ptrFrame) {

    }
}