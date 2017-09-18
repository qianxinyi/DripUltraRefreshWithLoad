package in.srain.cube.views.ptr;

import android.view.View;
//上拉刷新功能接口

public interface PtrLoadHandler {

    /**
     * Check can do refresh or not. For example the content is empty or the first child is in view.
     * <p/>
     * {@link PtrDefaultRefreshLoadHandler#checkContentCanBePulledDown}
     */
     boolean checkCanDoLoad(final PtrFrameLayout frame, final View content, final View footer);

    /**
     * When load begin
     *
     * @param frame
     */
    void onLoadBegin(final PtrFrameLayout frame);
}