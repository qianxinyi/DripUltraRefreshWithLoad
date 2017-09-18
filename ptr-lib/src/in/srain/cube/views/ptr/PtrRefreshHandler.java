package in.srain.cube.views.ptr;

import android.view.View;
//下拉刷新功能接口<上拉刷新功能接口>是否合并

public interface PtrRefreshHandler {

    /**
     * Check can do refresh or not. For example the content is empty or the first child is in view.
     * <p/>
     * {@link PtrDefaultRefreshLoadHandler#checkContentCanBePulledDown}
     */
    boolean checkCanDoRefresh(final PtrFrameLayout frame, final View content, final View header);

    /**
     * When refresh begin
     *
     * @param frame
     */
    void onRefreshBegin(final PtrFrameLayout frame);
}