package in.srain.cube.views.ptr.demo.ui.classic;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import in.srain.cube.image.CubeImageView;
import in.srain.cube.image.ImageLoader;
import in.srain.cube.image.ImageLoaderFactory;
import in.srain.cube.mints.base.TitleBaseFragment;
import in.srain.cube.request.JsonData;
import in.srain.cube.request.RequestFinishHandler;
import in.srain.cube.views.list.ListViewDataAdapter;
import in.srain.cube.views.list.ViewHolderBase;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultRefreshLoadHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrRefreshHandler;
import in.srain.cube.views.ptr.PtrLoadHandler;
import in.srain.cube.views.ptr.demo.R;
import in.srain.cube.views.ptr.demo.data.DemoRequestData;
import in.srain.cube.views.ptr.demo.ui.MaterialStyleFragment;

public class WithListView extends TitleBaseFragment {

    private ImageLoader mImageLoader;
    private ListViewDataAdapter<JsonData> mAdapter;
    private PtrClassicFrameLayout mPtrFrame;

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setHeaderTitle(R.string.ptr_demo_block_list_view);

        mImageLoader = ImageLoaderFactory.create(getContext());

        final View contentView = inflater.inflate(R.layout.fragment_classic_header_with_list_view, null);
        final ListView listView = (ListView) contentView.findViewById(R.id.rotate_header_list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0) {
                    final String url = mAdapter.getItem(position).optString("pic");
                    if (!TextUtils.isEmpty(url)) {
                        getContext().pushFragmentToBackStack(MaterialStyleFragment.class, url);
                    }
                }
            }
        });

         listView.setOnScrollListener(new AbsListView.OnScrollListener() {
             boolean isLastRow = false;
             @Override
             public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                 if (isLastRow) {//&& scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE
                     //加载元素

                     isLastRow = false;
                 }
             }

             @Override
             public void onScroll(AbsListView absListView,int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                 //判断是否滚到最后一行
                 if (firstVisibleItem + visibleItemCount+3== totalItemCount && totalItemCount > 0) {
                     updateData2();
                    isLastRow = true;
                 }


             }
         });



        mAdapter = new ListViewDataAdapter<JsonData>();
        mAdapter.setViewHolderClass(this, ViewHolder.class);
        listView.setAdapter(mAdapter);

        mPtrFrame = (PtrClassicFrameLayout) contentView.findViewById(R.id.rotate_header_list_view_frame);
        mPtrFrame.setLastUpdateTimeRelateObject(this);


        mPtrFrame.setPtrRefreshHandler(new PtrRefreshHandler() {
            //数据更新
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                         updateData();
            }
            //检测所要滑动的View
            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultRefreshLoadHandler.checkContentCanBePulledDown(frame, content, header);
            }
        });



        mPtrFrame.setPtrLoadHandler(new PtrLoadHandler() {
            @Override
            public boolean checkCanDoLoad(PtrFrameLayout frame, View content, View footer) {
                return PtrDefaultRefreshLoadHandler.checkContentCanBePulledUp(frame,content,footer);
            }

            @Override
            public void onLoadBegin(PtrFrameLayout frame) {
                Toast.makeText(getActivity(),"我是foot二！",Toast.LENGTH_SHORT).show();
                updateData2();
            }
        });
       //
         //mPtrFrame.setClickToLoad(true);
         //原生风格悬浮式刷新方式
        //mPtrFrame.setPinContent(true);
        // the following are default settings
        mPtrFrame.setResistance(1.7f);//设置滑动阻力（offSetY/resistance）
        mPtrFrame.setRatioOfHeight(1.2f);//刷新所需偏移高度
        mPtrFrame.setDurationToOffset(200);//刷新保持事件
        // default is true
        mPtrFrame.setKeepHeaderWhenRefresh(true);//保持头部刷新
        mPtrFrame.setKeepFooterWhenLoad(true);//保持尾部刷新
        mPtrFrame.setDurationToClose(1000);
        // default is false
        mPtrFrame.setPullToRefresh(false);

        mPtrFrame.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPtrFrame.autoRefresh(true,1000);
            }
        }, 100);

        return contentView;
    }

    protected void updateData() {

        DemoRequestData.getImageList(new RequestFinishHandler<JsonData>() {
            @Override
            public void onRequestFinish(final JsonData data) {
                mPtrFrame.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.getDataList().clear();
                        mAdapter.getDataList().addAll(data.optJson("data").optJson("list").toArrayList());
                        mPtrFrame.refreshComplete();
                        mAdapter.notifyDataSetChanged();
                    }
                }, 0);
            }
        });
    }

    protected void updateData2() {

        DemoRequestData.getImageList(new RequestFinishHandler<JsonData>() {
            @Override
            public void onRequestFinish(final JsonData data) {
                mPtrFrame.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //mAdapter.getDataList().clear();
                        mAdapter.getDataList().addAll(data.optJson("data").optJson("list").toArrayList());
                        mPtrFrame.loadComplete();
                        mAdapter.notifyDataSetChanged();
                    }
                }, 0);
            }
        });
    }


    private class ViewHolder extends ViewHolderBase<JsonData> {

        private CubeImageView mImageView;

        @Override
        public View createView(LayoutInflater inflater) {
            View v = inflater.inflate(R.layout.list_view_item, null);
            mImageView = (CubeImageView) v.findViewById(R.id.list_view_item_image_view);
            mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return v;
        }

        @Override
        public void showData(int position, JsonData itemData) {
            mImageView.loadImage(mImageLoader, itemData.optString("pic"));
        }
    }
}