package in.srain.cube.views.ptr.demo.ui.classic;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import in.srain.cube.image.CubeImageView;
import in.srain.cube.image.ImageLoader;
import in.srain.cube.image.ImageLoaderFactory;
import in.srain.cube.mints.base.TitleBaseFragment;
import in.srain.cube.request.JsonData;
import in.srain.cube.request.RequestFinishHandler;
import in.srain.cube.util.LocalDisplay;
import in.srain.cube.views.list.ListViewDataAdapter;
import in.srain.cube.views.list.ViewHolderBase;
import in.srain.cube.views.list.ViewHolderCreator;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import in.srain.cube.views.ptr.PtrHandler2;
import in.srain.cube.views.ptr.demo.R;
import in.srain.cube.views.ptr.demo.data.DemoRequestData;
import in.srain.cube.views.ptr.demo.ui.MaterialStyleFragment;
import in.srain.cube.views.ptr.demo.ui.MyRecyclerViewAdapter;
import in.srain.cube.views.ptr.demo.ui.SmartViewHolder;

public class WithRecyclerView extends TitleBaseFragment {

    private class Model {
        int imageId;

    }

    private static final int sGirdImageSize = (LocalDisplay.SCREEN_WIDTH_PIXELS - LocalDisplay.dp2px(12 + 12 + 10)) / 2;
    private ImageLoader mImageLoader;
    private MyRecyclerViewAdapter<Model> mAdapter;
    private Collection<Model> models = null;
    private PtrClassicFrameLayout mPtrFrame;

    @SuppressLint("NewApi")
    @Override
    public View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setHeaderTitle(R.string.ptr_demo_block_recycle_view);

        mImageLoader = ImageLoaderFactory.create(getContext());

        final View contentView = inflater.inflate(R.layout.fragment_classic_header_with_recycle_view, null);
        final RecyclerView recyclerView = (RecyclerView) contentView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(),2));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        models = loadModels();
        recyclerView.setAdapter(mAdapter = new MyRecyclerViewAdapter<Model>(models, R.layout.recycler_view_item) {
            @Override
            protected void onBindViewHolder(SmartViewHolder holder, Model model, int position) {
                holder.image(R.id.list_view_item_image_view, model.imageId);
            }
        });
        mPtrFrame = (PtrClassicFrameLayout) contentView.findViewById(R.id.rotate_header_grid_view_frame);
        mPtrFrame.setLastUpdateTimeRelateObject(this);

        mPtrFrame.setPtrHandler(new PtrHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {

                mPtrFrame.refreshComplete();

            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultHandler.checkContentCanBePulledDown(frame, content, header);
            }
        });

        mPtrFrame.setPtrHandler2(new PtrHandler2() {
            @Override
            public boolean checkCanDoLoad(PtrFrameLayout frame, View content, View footer) {
                return PtrDefaultHandler.checkContentCanBePulledUp(frame, content, footer);
            }

            @Override
            public void onLoadBegin(PtrFrameLayout frame) {
                Toast.makeText(getActivity(), "我是foot二！", Toast.LENGTH_SHORT).show();
                mAdapter.loadmore(loadModels());
                mPtrFrame.loadComplete();

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

        setupViews(mPtrFrame);
        return contentView;
    }

    protected void setupViews(final PtrClassicFrameLayout ptrFrame) {

    }

    private Collection<Model> loadModels() {
        /**
         * 模拟数据
         */
        return Arrays.asList(
                new Model() {{

                    this.imageId = R.mipmap.image_practice_repast_1;

                }}, new Model() {{

                    this.imageId = R.mipmap.image_practice_repast_2;

                }}, new Model() {{

                    this.imageId = R.mipmap.image_practice_repast_3;

                }}, new Model() {{

                    this.imageId = R.mipmap.image_practice_repast_4;

                }}, new Model() {{

                    this.imageId = R.mipmap.image_practice_repast_5;

                }}, new Model() {{

                    this.imageId = R.mipmap.image_practice_repast_6;

                }});
    }

}