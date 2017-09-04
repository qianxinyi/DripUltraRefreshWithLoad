package in.srain.cube.views.ptr;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import in.srain.cube.views.ptr.indicator.PtrIndicator;

public class PtrClassicDefaultFooter extends FrameLayout implements PtrUIFooter {
    private TextView footerTitle;
    private ProgressBar footerProgressBar;

    public PtrClassicDefaultFooter(Context context) {
        super(context);
        initViews(null);
    }

    public PtrClassicDefaultFooter(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews(attrs);
    }

    public PtrClassicDefaultFooter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initViews(attrs);
    }
    protected void initViews(AttributeSet attrs) {
        View footer = LayoutInflater.from(getContext()).inflate(R.layout.cube_ptr_classic_default_footer, this);
        footerTitle = (TextView) footer.findViewById(R.id.ptr_classic_footer_title);
        footerProgressBar = (ProgressBar) footer.findViewById(R.id.ptr_classic_footer_rotate_view_progressbar);
    }

    @Override
    public void onUIReset(PtrFrameLayout frame) {

    }

    @Override
    public void onUIRefreshPrepare(PtrFrameLayout frame) {
        footerTitle.setText("我是准备阶段！！");
    }

    @Override
    public void onUIRefreshBegin(PtrFrameLayout frame) {
               footerTitle.setText("我是开始阶段！！");
    }

    @Override
    public void onUIRefreshComplete(PtrFrameLayout frame) {
            footerTitle.setText("我是完成阶段！！");
    }

    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {

    }



}