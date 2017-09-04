package in.srain.cube.views.ptr;

import android.view.View;
import android.widget.AbsListView;
//有待完善
public abstract class PtrDefaultRefreshLoadHandler implements PtrRefreshHandler,PtrLoadHandler {

    public static boolean canChildScrollUp(View view) {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (view instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) view;

                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {

                return view.getScrollY() > 0;
            }
        } else {
            return view.canScrollVertically(-1);
        }
    }
    //有待完善
    public static boolean canChildScrollDown(View targetView){
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (targetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) targetView;
                return absListView.getChildCount() > 0
                        && (absListView.getLastVisiblePosition() < absListView.getChildCount() - 1
                        || absListView.getChildAt(absListView.getChildCount() - 1).getBottom() > absListView.getPaddingBottom());
            } else {
                return targetView.getScrollY() < 0;
            }
        } else {
            return targetView.canScrollVertically(1);
        }
    }

    /**
     * Default implement for check can perform pull to refresh
     *
     * @param frame
     * @param content
     * @param header
     * @return
     */
    public static boolean checkContentCanBePulledDown(PtrFrameLayout frame, View content, View header) {
        return !canChildScrollUp(content);
    }

    @Override
    public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
        return checkContentCanBePulledDown(frame, content, header);
    }

    public static boolean checkContentCanBePulledUp(PtrFrameLayout frame, View content, View footer) {
        return !canChildScrollDown(content);
    }


    @Override
    public boolean checkCanDoLoad(PtrFrameLayout frame, View content, View footer) {
        return checkContentCanBePulledUp(frame,content,footer);
    }


    @Override
    public void onLoadBegin(PtrFrameLayout frame) {

    }
}