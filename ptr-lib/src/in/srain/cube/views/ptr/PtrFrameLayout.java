package in.srain.cube.views.ptr;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.*;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;
import android.widget.Scroller;

import in.srain.cube.views.ptr.indicator.PtrIndicator;
import in.srain.cube.views.ptr.util.PtrCLog;

/**
 * This layout view for "Pull to Refresh(Ptr)" support all of the view, you can contain everything you want.
 * support: pull to refresh / release to refresh / auto refresh / keep header view while refreshing / hide header view while refreshing
 * It defines {@link PtrUIHeader}, which allows you customize the UI easily.
 */
public class PtrFrameLayout extends ViewGroup {

    // status
    public final static byte PTR_STATUS_INIT = 1;
    private byte mStatus = PTR_STATUS_INIT;
    public final static byte PTR_STATUS_PREPARE = 2;
    public final static byte PTR_STATUS_LOADING = 3;
    public final static byte PTR_STATUS_COMPLETE = 4;

    private static final boolean DEBUG_LAYOUT = true;
    public static boolean DEBUG = false;
    private static int ID = 1;
    protected final String LOG_TAG = "ptr-frame-" + ++ID;

    // auto refresh status  <刷新方式的定义>
    private final static byte FLAG_AUTO_REFRESH_AT_ONCE = 0x01;   //立即刷新方式
    private final static byte FLAG_AUTO_REFRESH_BUT_LATER = 0x01 << 1;
    private final static byte FLAG_ENABLE_NEXT_PTR_AT_ONCE = 0x01 << 2;
    private final static byte FLAG_PIN_CONTENT = 0x01 << 3;   //悬浮式刷新
    private final static byte MASK_AUTO_REFRESH = 0x03;    //自动刷新方式

    // optional config for define header and content in xml file
    private int mHeaderId = 0;
    private int mContainerId = 0;
    private int mFooterId=0;
    // config
    private int mDurationToClose = 200;
    private int mDurationToCloseHeader = 1000;
    private boolean mKeepHeaderWhenRefresh = true;
    private boolean mKeepFooterWhenRefresh=true;

    private boolean mPullToRefresh = false;//下拉刷新
    private boolean mPullToLoad=false;//上拉加载

    private View mHeaderView;
    protected View mContent;
    private View mFooterView;

    private PtrUIHeaderHolder mPtrUIHeaderHolder = PtrUIHeaderHolder.create();
    private PtrRefreshHandler mPtrRefreshHandler;
    private PtrUIFooterHolder mPtrUIFooterHolder = PtrUIFooterHolder.create();
    private PtrLoadHandler mPtrLoadHandler;

    // working parameters
    private ScrollChecker mScrollChecker;
    private int mPagingTouchSlop;
    private int mHeaderHeight;
    private int mFooterHeight;
    private boolean mDisableWhenHorizontalMove = false;
    private int mFlag = 0x00;

    // disable when detect moving horizontally
    private boolean mPreventForHorizontal = false;

    private MotionEvent mLastMoveEvent;

    private PtrUIHandlerHook mRefreshCompleteHook;
    private PtrUIHandlerHook mLoadCompleteHook;

    private int mLoadingMinTime = 500;
    private long mLoadingStartTime = 0;
    private PtrIndicator mPtrIndicator;
    private boolean mHasSendCancelEvent = false;
    private Runnable mPerformRefreshCompleteDelay = new Runnable() {
        @Override
        public void run() {
            performRefreshComplete();
        }
    };
    private Runnable mPerformLoadCompleteDelay = new Runnable() {
        @Override
        public void run() {
            performLoadComplete();
        }
    };

    public PtrFrameLayout(Context context) {
        this(context, null);
    }

    public PtrFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PtrFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context,attrs);
    }

    private void initView(Context context,AttributeSet attrs){
        mPtrIndicator = new PtrIndicator();
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.PtrFrameLayout, 0, 0);
        if (arr != null) {
            mHeaderId = arr.getResourceId(R.styleable.PtrFrameLayout_ptr_header, mHeaderId);
            mContainerId = arr.getResourceId(R.styleable.PtrFrameLayout_ptr_content, mContainerId);
            mFooterId=arr.getResourceId(R.styleable.PtrFrameLayout_ptr_footer,mFooterId);
            mPtrIndicator.setResistance(arr.getFloat(R.styleable.PtrFrameLayout_ptr_resistance, mPtrIndicator.getResistance()));
            mDurationToClose = arr.getInt(R.styleable.PtrFrameLayout_ptr_duration_to_close, mDurationToClose);
            mDurationToCloseHeader = arr.getInt(R.styleable.PtrFrameLayout_ptr_duration_to_close_header, mDurationToCloseHeader);
            float ratio = mPtrIndicator.getRatio();
            ratio = arr.getFloat(R.styleable.PtrFrameLayout_ptr_ratio_of_header_height_to_refresh, ratio);
            mPtrIndicator.setRatioOfHeight(ratio);
            mKeepHeaderWhenRefresh = arr.getBoolean(R.styleable.PtrFrameLayout_ptr_keep_header_when_refresh, mKeepHeaderWhenRefresh);
            mPullToRefresh = arr.getBoolean(R.styleable.PtrFrameLayout_ptr_pull_to_fresh, mPullToRefresh);
            arr.recycle();
        }

        mScrollChecker = new ScrollChecker();
        final ViewConfiguration conf = ViewConfiguration.get(getContext());
        mPagingTouchSlop = conf.getScaledTouchSlop() * 2;

    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final int childCount = getChildCount();
        if (childCount > 3) {
            throw new IllegalStateException("PtrFrameLayout can only contains 3 children");
        }
        //定义为确认的子View索引
        boolean[] uncertains = new boolean[childCount];
        //第一次查找确认的 子View
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            if (view instanceof PtrUIHeader && mHeaderView == null) {
                mHeaderView = view;
            } else if (view instanceof PtrUIFooter && mFooterView == null) {
                mFooterView = view;
            } else if (mContent == null && (
                               view instanceof AbsListView
                            || view instanceof ScrollView
                            || view instanceof WebView)) {
                mContent = view;
            } else {
                uncertains[i] = true;
            }
        }
        //如果有 未确认（uncertains）的子View 通过智能算法计算
        for (int i = 0; i < childCount; i++) {
            if (uncertains[i]) {
                View view = getChildAt(i);
                if (childCount == 1 && mContent == null) {
                    mContent = view;
                } else if (i == 0 && mHeaderView == null) {
                    mHeaderView = view;
                } else if (childCount == 2 && mContent == null) {
                    mContent = view;
                } else if (i == 2 && mFooterView == null) {
                    mFooterView = view;
                } else if (mContent == null) {
                    mContent = view;
                }
            }
        }

        //重新排序
        if (mContent != null) {
            bringChildToFront(mContent);
        }
        if (mHeaderView != null) {
            bringChildToFront(mHeaderView);
        }
        if (mFooterView != null) {
            bringChildToFront(mFooterView);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(mHeaderView==null)
            onFinishInflate();
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mScrollChecker != null) {
            mScrollChecker.destroy();
        }

        if (mPerformRefreshCompleteDelay != null) {
            removeCallbacks(mPerformRefreshCompleteDelay);
        }
        if (mPerformLoadCompleteDelay != null)
            removeCallbacks(mPerformLoadCompleteDelay);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isDebug()) {
            PtrCLog.d(LOG_TAG, "onMeasure frame: width: %s, height: %s, padding: %s %s %s %s",
                    getMeasuredHeight(), getMeasuredWidth(),
                    getPaddingLeft(), getPaddingRight(), getPaddingTop(), getPaddingBottom());
        }

        if (mHeaderView != null) {
            measureChildWithMargins(mHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            mHeaderHeight = mHeaderView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            mPtrIndicator.setHeaderHeight(mHeaderHeight);
        }

        if (mContent != null) {
            measureContentView(mContent, widthMeasureSpec, heightMeasureSpec);
            if (isDebug()) {
                ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) mContent.getLayoutParams();
                PtrCLog.d(LOG_TAG, "onMeasure content, width: %s, height: %s, margin: %s %s %s %s",
                        getMeasuredWidth(), getMeasuredHeight(),
                        lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin);
                PtrCLog.d(LOG_TAG, "onMeasure, currentPos: %s, lastPos: %s, top: %s",
                        mPtrIndicator.getCurrentPosY(), mPtrIndicator.getLastPosY(), mContent.getTop());
            }
        }
        if (mFooterView != null) {
            measureChildWithMargins(mFooterView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams lp = (MarginLayoutParams) mFooterView.getLayoutParams();
            mFooterHeight = mFooterView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            mPtrIndicator.setFooterHeight(mFooterHeight);
        }
    }

    private void measureContentView(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin, lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean flag, int i, int j, int k, int l) {
        layoutChildren();
    }

    private void layoutChildren() {
        int offset = mPtrIndicator.getCurrentPosY();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        if (mHeaderView != null) {
            MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            // enhance readability(header is layout above screen when first init)
            final int top = -(mHeaderHeight - paddingTop - lp.topMargin - offset);
            final int right = left + mHeaderView.getMeasuredWidth();
            final int bottom = top + mHeaderView.getMeasuredHeight();
            mHeaderView.layout(left, top, right, bottom);
            if (isDebug()) {
                PtrCLog.d(LOG_TAG, "onLayout header: %s %s %s %s", left, top, right, bottom);
            }
        }
        if (mFooterView != null) {
            MarginLayoutParams lp = (MarginLayoutParams) mFooterView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            // enhance readability(footer is layout bottom screen when first init)
            final int top = getMeasuredHeight() + paddingTop + lp.topMargin + offset;//????
            final int right = left + mFooterView.getMeasuredWidth();
            final int bottom = top + mFooterView.getMeasuredHeight();
            mFooterView.layout(left, top, right, bottom);
            if (isDebug()) {
                PtrCLog.d(LOG_TAG, "onLayout footer: %s %s %s %s", left, top, right, bottom);
            }
        }
        if (mContent != null) {
            if (isPinContent()) {
                offset = 0;
            }
            MarginLayoutParams lp = (MarginLayoutParams) mContent.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            final int top = paddingTop + lp.topMargin + offset;
            final int right = left + mContent.getMeasuredWidth();
            final int bottom = top + mContent.getMeasuredHeight();
            if (isDebug()) {
                PtrCLog.d(LOG_TAG, "onLayout content: %s %s %s %s", left, top, right, bottom);
            }
            mContent.layout(left, top, right, bottom);
        }

    }

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    private boolean isDebug() {
        return DEBUG && DEBUG_LAYOUT;
    }

    public boolean dispatchTouchEventSupper(MotionEvent e) {
        return super.dispatchTouchEvent(e);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (!isEnabled() || mContent == null || mHeaderView == null) {
            return dispatchTouchEventSupper(e);
        }
        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mPtrIndicator.onRelease();
                if (mPtrIndicator.hasLeftStartPosition() && PtrIndicator.isPullHeader) {//处理下拉刷新
                    onRelease(false);
                    if (mPtrIndicator.hasMovedAfterPressedDown()) {
                        sendCancelEvent();
                        return true;
                    }
                    return dispatchTouchEventSupper(e);
                } else if (mPtrIndicator.hasLeftBottomPosition() && PtrIndicator.isPullFooter) {//处理上拉加载
                    onRelease2(false);//上拉加载
                    if (mPtrIndicator.hasMovedAfterPressedDown()) {
                        sendCancelEvent();
                        return true;
                    }
                    return dispatchTouchEventSupper(e);
                } else {
                    return dispatchTouchEventSupper(e);
                }

            case MotionEvent.ACTION_DOWN:
                mHasSendCancelEvent = false;
                mPtrIndicator.onPressDown(e.getX(), e.getY());
                mScrollChecker.abortIfWorking();
                mPreventForHorizontal = false;
                // The cancel event will be sent once the position is moved.
                // So let the event pass to children.
                // fix #93, #102
                dispatchTouchEventSupper(e);
                return true;

            case MotionEvent.ACTION_MOVE:
                mLastMoveEvent = e;
                mPtrIndicator.onMove(e.getX(), e.getY());
                float offsetX = mPtrIndicator.getOffsetX();
                float offsetY = mPtrIndicator.getOffsetY();
                if (mDisableWhenHorizontalMove && !mPreventForHorizontal && (Math.abs(offsetX) > mPagingTouchSlop && Math.abs(offsetX) > Math.abs(offsetY))) {
                    if (mPtrIndicator.isInStartPosition()) {
                        mPreventForHorizontal = true;
                    }
                }
                if (mPreventForHorizontal || offsetY == 0) {
                    return dispatchTouchEventSupper(e);
                }
                boolean moveDown = offsetY > 0;
                boolean moveUp = offsetY < 0;
                boolean canMoveDown = mPtrIndicator.hasLeftBottomPosition();
                boolean canMoveUp = mPtrIndicator.hasLeftStartPosition();
                if (PtrIndicator.isPullFooter || (moveUp && mPtrLoadHandler != null &&!PtrIndicator.isPullHeader)) {
                    PtrIndicator.isPullHeader = false;//分辨正在拉头部或者尾部
                    PtrIndicator.isPullFooter = true;
                    // disable move when footer not reach bottom<检测尾部>
                    if (moveUp && mPtrLoadHandler != null && !mPtrLoadHandler.checkCanDoLoad(this, mContent, mFooterView)) {
                        PtrIndicator.isPullHeader = false;//分辨正在拉头部或者尾部
                        PtrIndicator.isPullFooter = false;
                        return dispatchTouchEventSupper(e);
                    }
                    if ((moveDown && canMoveDown) || moveUp) {
                        movePos2(offsetY);
                        return true;
                    }
                }
                if (PtrIndicator.isPullHeader || (moveDown && mPtrRefreshHandler != null && !PtrIndicator.isPullFooter)) {
                    PtrIndicator.isPullHeader = true;//分辨正在拉头部或者尾部
                    PtrIndicator.isPullFooter = false;
                    // disable move when header not reach top
                    if (moveDown && mPtrRefreshHandler != null && !mPtrRefreshHandler.checkCanDoRefresh(this, mContent, mHeaderView)) {
                        PtrIndicator.isPullHeader = false;//分辨正在拉头部或者尾部
                        PtrIndicator.isPullFooter = false;
                        return dispatchTouchEventSupper(e);
                    }
                    if ((moveUp && canMoveUp) || moveDown) {
                        movePos(offsetY);
                        return true;
                    }
                }
        }
        return dispatchTouchEventSupper(e);
    }

    /**
     * if deltaY < 0, move the content up
     * //关于FooterView ，HeaderView的处理
     *
     * @param deltaY
     */
    private void movePos2(float deltaY) {
        // has reached the bottom
        if ((deltaY >= 0 && mPtrIndicator.isInBottomPosition())) {
            if (DEBUG) {
                PtrCLog.e(LOG_TAG, String.format("has reached the bottom"));
            }
            return;
        }
        int to = mPtrIndicator.getCurrentPosY()+(int) deltaY;
        if(-to>3*mFooterHeight)    return;    // // TODO: 2017/9/1  限制最大移动高度
        // over bottom
        if (mPtrIndicator.willOverBottom(to)) {
            if (DEBUG) {
                PtrCLog.e(LOG_TAG, String.format("over bottom"));
            }
            to = PtrIndicator.POS_START;
        }
        mPtrIndicator.setCurrentPos(to);
        int change = to - mPtrIndicator.getLastPosY();
        updatePos2(change);
    }
    private void updatePos2(int change) {
        if (change == 0) return;
        boolean isUnderTouch = mPtrIndicator.isUnderTouch();
        // once moved, cancel event will be sent to child
        if (isUnderTouch && !mHasSendCancelEvent && mPtrIndicator.hasMovedAfterPressedDown()) {
            mHasSendCancelEvent = true;
            sendCancelEvent();
        }

        // leave initiated position or just refresh complete  刚刚离开，UI进行准备显示阶段
        if ((mPtrIndicator.hasJustLeftBottomPosition()&& mStatus == PTR_STATUS_INIT) ||
                (mPtrIndicator.goUpCrossFinishPosition()&& mStatus == PTR_STATUS_COMPLETE && isEnabledNextPtrAtOnce())) {
            mStatus = PTR_STATUS_PREPARE;
            mPtrUIFooterHolder.onUIRefreshPrepare(this);
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHeader: onUIRefreshPrepare, mFlag %s", mFlag);
            }
        }

        // back to initiated position
        if (mPtrIndicator.hasJustBackToBottomPosition()) {
            tryToNotifyReset();

            // recover event to children
            if (isUnderTouch) {
                sendDownEvent();
            }
        }

        // Pull to Refresh<pull to load>
        if (mStatus == PTR_STATUS_PREPARE) {
            // reach fresh height while moving from top to bottom
            if (isUnderTouch && !isAutoRefresh() && mPullToLoad && mPtrIndicator.crossLoadLineFromTopToBottom()) {
                tryToPerformLoad();
            }
            // reach header height while auto refresh
            if (performAutoRefreshButLater() && mPtrIndicator.hasJustReachedHeaderHeightFromTopToBottom()) {
                tryToPerformLoad();
            }
        }

        if (DEBUG) {
            PtrCLog.v(LOG_TAG, "updatePos: change: %s, current: %s last: %s, top: %s, headerHeight: %s",
                    change, mPtrIndicator.getCurrentPosY(), mPtrIndicator.getLastPosY(), mContent.getTop(), mHeaderHeight);
        }

            mFooterView.offsetTopAndBottom(change);
        if (!isPinContent()) {
            mContent.offsetTopAndBottom(change);
        }
        invalidate();

        if (mPtrUIFooterHolder.hasHandler()) {
            mPtrUIFooterHolder.onUIPositionChange(this, isUnderTouch, mStatus, mPtrIndicator);
        }
        onPositionChange(isUnderTouch, mStatus, mPtrIndicator);
    }
    private void movePos(float deltaY)  {
        // has reached the top
        if ((deltaY < 0 && mPtrIndicator.isInStartPosition())) {
            if (DEBUG) {
                PtrCLog.e(LOG_TAG, String.format("has reached the top"));
            }
            return;
        }

        int to = mPtrIndicator.getCurrentPosY() + (int) deltaY;
        if(to>3*mHeaderHeight)  return;
        // over top
        if (mPtrIndicator.willOverTop(to)) {
            if (DEBUG) {
                PtrCLog.e(LOG_TAG, String.format("over top"));
            }
            to = PtrIndicator.POS_START;
        }
        mPtrIndicator.setCurrentPos(to);
        int change = to - mPtrIndicator.getLastPosY();
        updatePos(change);
    }
    private void updatePos(int change)  {
        if (change == 0) {
            return;
        }

        boolean isUnderTouch = mPtrIndicator.isUnderTouch();
        // once moved, cancel event will be sent to child
        if (isUnderTouch && !mHasSendCancelEvent && mPtrIndicator.hasMovedAfterPressedDown()) {
            mHasSendCancelEvent = true;
            sendCancelEvent();
        }

        // leave initiated position or just refresh complete
        if ((mPtrIndicator.hasJustLeftStartPosition() && mStatus == PTR_STATUS_INIT) ||
                (mPtrIndicator.goDownCrossFinishPosition() && mStatus == PTR_STATUS_COMPLETE && isEnabledNextPtrAtOnce())) {
            mStatus = PTR_STATUS_PREPARE;
            mPtrUIHeaderHolder.onUIRefreshPrepare(this);
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHeader: onUIRefreshPrepare, mFlag %s", mFlag);
            }
        }

        // back to initiated position
        if (mPtrIndicator.hasJustBackToStartPosition()) {
            tryToNotifyReset();

            // recover event to children
            if (isUnderTouch) {
                sendDownEvent();
            }
        }

        // Pull to Refresh
        if (mStatus == PTR_STATUS_PREPARE) {
            // reach fresh height while moving from top to bottom
            if (isUnderTouch && !isAutoRefresh() && mPullToRefresh && mPtrIndicator.crossRefreshLineFromTopToBottom()) {
                tryToPerformRefresh();
            }
            // reach header height while auto refresh
            if (performAutoRefreshButLater() && mPtrIndicator.hasJustReachedHeaderHeightFromTopToBottom()) {
                tryToPerformRefresh();
            }
        }

        if (DEBUG) {
            PtrCLog.v(LOG_TAG, "updatePos: change: %s, current: %s last: %s, top: %s, headerHeight: %s",
                    change, mPtrIndicator.getCurrentPosY(), mPtrIndicator.getLastPosY(), mContent.getTop(), mHeaderHeight);
        }

        //做最大滑动距离判定
        mHeaderView.offsetTopAndBottom(change);
        if (!isPinContent()) {
            mContent.offsetTopAndBottom(change);
        }
        invalidate();

        if (mPtrUIHeaderHolder.hasHandler()) {
            mPtrUIHeaderHolder.onUIPositionChange(this, isUnderTouch, mStatus, mPtrIndicator);
        }
        onPositionChange(isUnderTouch, mStatus, mPtrIndicator);
    }

    protected void onPositionChange(boolean isInTouching, byte status, PtrIndicator mPtrIndicator) {
    }

    @SuppressWarnings("unused")
    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    private void onRelease(boolean stayForLoading) {
        tryToPerformRefresh();
        if (mStatus == PTR_STATUS_LOADING) {
            // keep header for fresh
            if (mKeepHeaderWhenRefresh) {
                // scroll header back
                if (mPtrIndicator.isOverOffsetToKeepHeaderWhileLoading() && !stayForLoading) {
                    mScrollChecker.tryToScrollTo(mPtrIndicator.getOffsetToKeepHeaderWhileLoading(), mDurationToClose,false);
                } else {
                    // do nothing
                }
            } else {
                tryScrollBackToTopWhileLoading();
            }
        } else {
            if (mStatus == PTR_STATUS_COMPLETE) {
                notifyUIRefreshComplete(false);
            } else {
                tryScrollBackToTopAbortRefresh();
            }
        }
    }
    private void onRelease2(boolean stayForLoading) {
        tryToPerformLoad();//回调类接口
        if (mStatus == PTR_STATUS_LOADING) {
            // keep footer for fresh
            if (mKeepFooterWhenRefresh) {// TODO: 2017/8/25
                // scroll footer back
                if (mPtrIndicator.isOverOffsetToKeepFooterWhileLoading() && !stayForLoading) {
                    //表示移动到啥位置《底层封装好的》
                    mScrollChecker.tryToScrollTo(-mPtrIndicator.getOffsetToKeepFooterWhileLoading(), mDurationToClose,true);
                }
            } else {
                tryScrollBackToBottomWhileLoading();
            }
        } else {
            if (mStatus == PTR_STATUS_COMPLETE) {
                notifyUILoadComplete(false);
            } else {
                tryScrollBackToBottomAbortLoad();
            }
        }
    }

    /**
     * please DO REMEMBER resume the hook
     *
     * @param hook
     */

    public void setRefreshCompleteHook(PtrUIHandlerHook hook) {
        mRefreshCompleteHook = hook;
        hook.setResumeAction(new Runnable() {
            @Override
            public void run() {
                if (DEBUG) {
                    PtrCLog.d(LOG_TAG, "mRefreshCompleteHook resume.");
                }
                notifyUIRefreshComplete(true);
            }
        });
    }
    public void setLoadCompleteHook(PtrUIHandlerHook hook) {
        mLoadCompleteHook = hook;
        hook.setResumeAction(new Runnable() {
            @Override
            public void run() {
                if (DEBUG) {
                    PtrCLog.d(LOG_TAG, "mRefreshCompleteHook resume.");
                }
                notifyUILoadComplete(false);
            }
        });
    }

    /**
     * Scroll back to  if is not under touch
     */
    private void tryScrollBackToTop() {
        if (!mPtrIndicator.isUnderTouch()) {
            mScrollChecker.tryToScrollTo(PtrIndicator.POS_START, mDurationToCloseHeader,false);
        }
    }
    private void tryScrollBackToBottom() {
        if (!mPtrIndicator.isUnderTouch()) {
            mScrollChecker.tryToScrollTo(PtrIndicator.POS_START, mDurationToCloseHeader,true);
        }
    }
    /**
     * just make easier to understand
     */
    private void tryScrollBackToTopWhileLoading() {
        tryScrollBackToTop();
    }

    private void tryScrollBackToBottomWhileLoading() {
        tryScrollBackToBottom();
    }

    /**
     * just make easier to understand
     */
    private void tryScrollBackToTopAfterComplete() {
        tryScrollBackToTop();
    }
    private void tryScrollBackToBottomAfterComplete() {
        tryScrollBackToBottom();
    }

    /**
     * just make easier to understand
     */
    private void tryScrollBackToTopAbortRefresh() {
        tryScrollBackToTop();
    }
    private void tryScrollBackToBottomAbortLoad() {
        tryScrollBackToBottom();
    }


    private boolean tryToPerformRefresh() {
        if (mStatus != PTR_STATUS_PREPARE) {
            return false;
        }
        if ((mPtrIndicator.isOverOffsetToKeepHeaderWhileLoading() && isAutoRefresh()) ||
                mPtrIndicator.isOverOffsetToRefresh()) {
            mStatus = PTR_STATUS_LOADING;
            performRefresh();
        }
        return false;
    }
    private boolean tryToPerformLoad() {
        if (mStatus != PTR_STATUS_PREPARE) {
            return false;
        }

//        if ((mPtrIndicator.isOverOffsetToKeepHeaderWhileLoading() && isAutoRefresh()) ||
//                mPtrIndicator.isOverOffsetToRefresh()) {
//            mStatus = PTR_STATUS_LOADING;
//            performRefresh();
//        }

        if ( mPtrIndicator.isOverOffsetToLoad()) {
            mStatus = PTR_STATUS_LOADING;
            performLoad();
        }
        return false;
    }

    private void performRefresh() {
        mLoadingStartTime = System.currentTimeMillis();
        if (mPtrUIHeaderHolder.hasHandler()) {
            mPtrUIHeaderHolder.onUIRefreshBegin(this);
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHeader: onUIRefreshBegin");
            }
        }
        if (mPtrRefreshHandler != null) {
            mPtrRefreshHandler.onRefreshBegin(this);
        }
    }
    private void performLoad() {
        mLoadingStartTime = System.currentTimeMillis();
        if (mPtrUIFooterHolder.hasHandler()) {
            mPtrUIFooterHolder.onUIRefreshBegin(this);
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHeader: onUIRefreshBegin");
            }
        }
        if (mPtrLoadHandler != null) {
            mPtrLoadHandler.onLoadBegin(this);
        }
    }

    /**
     * If at the top and not in loading, reset
     */
    private boolean tryToNotifyReset() {
        if ((mStatus == PTR_STATUS_COMPLETE || mStatus == PTR_STATUS_PREPARE) ) {
            if(mPtrIndicator.isInStartPosition()){
                if (mPtrUIHeaderHolder.hasHandler()) {
                    mPtrUIHeaderHolder.onUIReset(this);
                    if (DEBUG) {
                        PtrCLog.i(LOG_TAG, "PtrUIHeader: onUIReset");
                    }
                }
                mStatus = PTR_STATUS_INIT;
                clearFlag();

            }else if(mPtrIndicator.isInBottomPosition()) {//判断是否到底尾部《有毛病》
                if (mPtrUIFooterHolder.hasHandler()) {
                    mPtrUIFooterHolder.onUIReset(this);
                    if (DEBUG) {
                        PtrCLog.i(LOG_TAG, "PtrUIHeader: onUIReset");
                    }
                }
                mStatus = PTR_STATUS_INIT;
                clearFlag();
            }

            return true;
        }
        return false;
    }
    protected void onPtrScrollAbort() {
        if (mPtrIndicator.hasLeftStartPosition() && isAutoRefresh()) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "call onRelease after scroll abort");
            }
            onRelease(true);
        }


    }
    protected void onPtrScrollFinish() {
        if (mPtrIndicator.hasLeftStartPosition() && isAutoRefresh()) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "call onRelease after scroll finish");
            }
            onRelease(true);
        }
    }

    /**
     * Detect whether is refreshing.
     *
     * @return
     */
    public boolean isRefreshing() {
        return mStatus == PTR_STATUS_LOADING;
    }

    /**
     * Call this when data is loaded.
     * The UI will perform complete at once or after a delay, depends on the time elapsed is greater then {@link #mLoadingMinTime} or not.
     */
    final public void refreshComplete() {
        if (DEBUG) {
            PtrCLog.i(LOG_TAG, "refreshComplete");
        }

        if (mRefreshCompleteHook != null) {
            mRefreshCompleteHook.reset();
        }

        int delay = (int) (mLoadingMinTime - (System.currentTimeMillis() - mLoadingStartTime));
        if (delay <= 0) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "performRefreshComplete at once");
            }
            performRefreshComplete();
        } else {
            postDelayed(mPerformRefreshCompleteDelay, delay);
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "performRefreshComplete after delay: %s", delay);
            }
        }
    }
    final public void loadComplete() {
        if (DEBUG) {
            PtrCLog.i(LOG_TAG, "refreshComplete");
        }

        if (mLoadCompleteHook != null) {
            mLoadCompleteHook.reset();
        }

        int delay = (int) (mLoadingMinTime - (System.currentTimeMillis() - mLoadingStartTime));
        if (delay <= 0) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "performRefreshComplete at once");
            }
            performLoadComplete();
        } else {
            postDelayed(mPerformLoadCompleteDelay, delay);
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "performRefreshComplete after delay: %s", delay);
            }
        }
    }

    /**
     * Do refresh complete work when time elapsed is greater than {@link #mLoadingMinTime}
     */
    private void performRefreshComplete() {
        mStatus = PTR_STATUS_COMPLETE;
        // if is auto refresh do nothing, wait scroller stop
        if (mScrollChecker.mIsRunning && isAutoRefresh()) {
            // do nothing
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "performRefreshComplete do nothing, scrolling: %s, auto refresh: %s",
                        mScrollChecker.mIsRunning, mFlag);
            }
            return;
        }
        notifyUIRefreshComplete(false);

    }
    private void performLoadComplete() {
        mStatus = PTR_STATUS_COMPLETE;
        // if is auto refresh do nothing, wait scroller stop
        if (mScrollChecker.mIsRunning && isAutoRefresh()) {
            // do nothing
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "performRefreshComplete do nothing, scrolling: %s, auto refresh: %s",
                        mScrollChecker.mIsRunning, mFlag);
            }
            return;
    }
        notifyUILoadComplete(false);
    }

    /**
     * Do real refresh work. If there is a hook, execute the hook first.
     *
     * @param ignoreHook
     */
    private void notifyUIRefreshComplete(boolean ignoreHook) {
        /**
         * After hook operation is done, {@link #notifyUIRefreshComplete} will be call in resume action to ignore hook.
         */
        if (mPtrIndicator.hasLeftStartPosition() && !ignoreHook && mRefreshCompleteHook != null) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "notifyUIRefreshComplete mRefreshCompleteHook run.");
            }

            mRefreshCompleteHook.takeOver();
            return;
        }
        if (mPtrUIHeaderHolder.hasHandler()) {
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHeader: onUIRefreshComplete");
            }
            mPtrUIHeaderHolder.onUIRefreshComplete(this);
        }
        mPtrIndicator.onUIRefreshComplete();
        tryScrollBackToTopAfterComplete();
        tryToNotifyReset();
    }
    private void notifyUILoadComplete(boolean ignoreHook) {
        /**
         * After hook operation is done, {@link #notifyUIRefreshComplete} will be call in resume action to ignore hook.
         */
        if (mPtrIndicator.hasLeftBottomPosition() && !ignoreHook && mLoadCompleteHook != null) {//hook
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "notifyUIRefreshComplete mRefreshCompleteHook run.");
            }
            mLoadCompleteHook.takeOver();
            return;
        }
        if (mPtrUIFooterHolder.hasHandler()) {
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHeader: onUIRefreshComplete");
            }
            mPtrUIFooterHolder.onUIRefreshComplete(this);
        }
        mPtrIndicator.onUIRefreshComplete();
        tryScrollBackToBottomAfterComplete();
        tryToNotifyReset();
    }

    public void autoRefresh() {
        autoRefresh(true, mDurationToCloseHeader);
    }
    public void autoRefresh(boolean atOnce) {
        autoRefresh(atOnce, mDurationToCloseHeader);
    }

    private void clearFlag() {
        // remove auto fresh flag
        mFlag = mFlag & ~MASK_AUTO_REFRESH;
    }

    public void autoRefresh(boolean atOnce, int duration) {
        if (mStatus != PTR_STATUS_INIT) return;
        mFlag |= atOnce ? FLAG_AUTO_REFRESH_AT_ONCE : FLAG_AUTO_REFRESH_BUT_LATER;
        mStatus = PTR_STATUS_PREPARE;
        if (mPtrUIHeaderHolder.hasHandler()) {
            mPtrUIHeaderHolder.onUIRefreshPrepare(this);
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHeader: onUIRefreshPrepare, mFlag %s", mFlag);
            }
        }
        mScrollChecker.tryToScrollTo(mPtrIndicator.getOffsetToRefresh(), duration,false);
        if (atOnce) {
            mStatus = PTR_STATUS_LOADING;
            performRefresh();
        }
    }

    public boolean isAutoRefresh() {
        return (mFlag & MASK_AUTO_REFRESH) > 0;
    }

    private boolean performAutoRefreshButLater() {
        return (mFlag & MASK_AUTO_REFRESH) == FLAG_AUTO_REFRESH_BUT_LATER;
    }

    public boolean isEnabledNextPtrAtOnce() {
        return (mFlag & FLAG_ENABLE_NEXT_PTR_AT_ONCE) > 0;
    }

    /**
     * If @param enable has been set to true. The user can perform next PTR at once.
     *
     * @param enable
     */
    public void setEnabledNextPtrAtOnce(boolean enable) {
        if (enable) {
            mFlag = mFlag | FLAG_ENABLE_NEXT_PTR_AT_ONCE;
        } else {
            mFlag = mFlag & ~FLAG_ENABLE_NEXT_PTR_AT_ONCE;
        }
    }

    public boolean isPinContent() {
        return (mFlag & FLAG_PIN_CONTENT) > 0;
    }

    /**
     * The content view will now move when {@param pinContent} set to true.
     *
     * @param pinContent
     */
    public void setPinContent(boolean pinContent) {
        if (pinContent) {
            mFlag = mFlag | FLAG_PIN_CONTENT;
        } else {
            mFlag = mFlag & ~FLAG_PIN_CONTENT;
        }
    }

    /**
     * It's useful when working with viewpager.
     *
     * @param disable
     */
    public void disableWhenHorizontalMove(boolean disable) {
        mDisableWhenHorizontalMove = disable;
    }

    /**
     * loading will last at least for so long
     *
     * @param time
     */
    public void setLoadingMinTime(int time) {
        mLoadingMinTime = time;
    }

    /**
     * Not necessary any longer. Once moved, cancel event will be sent to child.
     *
     * @param yes
     */
    @Deprecated
    public void setInterceptEventWhileWorking(boolean yes) {

    }

    @SuppressWarnings({"unused"})
    public View getContentView() {
        return mContent;
    }

    public void setPtrRefreshHandler(PtrRefreshHandler ptrRefreshHandler) {
        mPtrRefreshHandler = ptrRefreshHandler;
    }

    public void setPtrLoadHandler(PtrLoadHandler ptrLoadHandler) {
        mPtrLoadHandler = ptrLoadHandler;
    }

    public void addPtrUIHeader(PtrUIHeader ptrUIHeader) {
        PtrUIHeaderHolder.addHandler(mPtrUIHeaderHolder, ptrUIHeader);
    }

    public void addPtrUIFooter(PtrUIFooter ptrUIFooter) {
        PtrUIFooterHolder.addHandler(mPtrUIFooterHolder, ptrUIFooter);
    }


    @SuppressWarnings({"unused"})
    public void removePtrUIHeader(PtrUIHeader ptrUIHeader) {
        mPtrUIHeaderHolder = PtrUIHeaderHolder.removeHandler(mPtrUIHeaderHolder, ptrUIHeader);
    }

    public void removePtrUIFooter(PtrUIFooter ptrUIFooter) {
        mPtrUIFooterHolder = PtrUIFooterHolder.removeHandler(mPtrUIFooterHolder, ptrUIFooter);
    }

    public void setPtrIndicator(PtrIndicator slider) {
        if (mPtrIndicator != null && mPtrIndicator != slider) {
            slider.convertFrom(mPtrIndicator);
        }
        mPtrIndicator = slider;
    }

    @SuppressWarnings({"unused"})
    public float getResistance() {
        return mPtrIndicator.getResistance();
    }

    public void setResistance(float resistance) {
        mPtrIndicator.setResistance(resistance);
    }

    @SuppressWarnings({"unused"})
    public float getDurationToClose() {
        return mDurationToClose;
    }

    /**
     * The duration to return back to the refresh position
     *
     * @param duration
     */
    public void setDurationToClose(int duration) {
        mDurationToClose = duration;
    }

    @SuppressWarnings({"unused"})
    public long getDurationToCloseHeader() {
        return mDurationToCloseHeader;
    }

    /**
     * The duration to close time
     *
     * @param duration
     */
    public void setDurationToCloseHeader(int duration) {
        mDurationToCloseHeader = duration;
    }

    public void setRatioOfHeight(float ratio) {
        mPtrIndicator.setRatioOfHeight(ratio);
    }

    public int getOffsetToRefresh() {
        return mPtrIndicator.getOffsetToRefresh();
    }

    @SuppressWarnings({"unused"})
    public void setOffsetToRefresh(int offset) {
        mPtrIndicator.setOffsetToRefresh(offset);
    }

    @SuppressWarnings({"unused"})
    public float getRatioOfHeight() {
        return mPtrIndicator.getRatio();
    }

    @SuppressWarnings({"unused"})
    public int getOffsetToKeepHeaderWhileLoading() {
        return mPtrIndicator.getOffsetToKeepHeaderWhileLoading();
    }

    @SuppressWarnings({"unused"})
    public void setOffsetToKeepHeaderWhileLoading(int offset) {
        mPtrIndicator.setOffsetToKeepHeaderWhileLoading(offset);
    }
    @SuppressWarnings({"unused"})
    public void setOffsetToKeepFooterWhileLoading(int offset) {
        mPtrIndicator.setOffsetToKeepFooterWhileLoading(offset);
    }

    @SuppressWarnings({"unused"})
    public boolean isKeepHeaderWhenRefresh() {
        return mKeepHeaderWhenRefresh;
    }
    @SuppressWarnings({"unused"})
    public boolean isKeepFooterWhenLoad() {
        return mKeepFooterWhenRefresh;
    }

    public void setKeepHeaderWhenRefresh(boolean keepOrNot) {
        mKeepHeaderWhenRefresh = keepOrNot;
    }
    public void setKeepFooterWhenLoad(boolean keepOrNot) {
        mKeepFooterWhenRefresh = keepOrNot;
    }

    public boolean isPullToRefresh() {return mPullToRefresh;}
    public void setPullToRefresh(boolean pullToRefresh) {mPullToRefresh = pullToRefresh;}
    public boolean isPullToLoad() {return mPullToLoad;}
    public void setPullToLoad(boolean pullToLoad) {mPullToLoad = pullToLoad;}

    @SuppressWarnings({"unused"})
    public View getHeaderView() {
        return mHeaderView;
    }
    public void setHeaderView(View header) {
        if (mHeaderView != null && header != null && mHeaderView != header) {
            removeView(mHeaderView);
        }
        ViewGroup.LayoutParams lp = header.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(-1, -2);
            header.setLayoutParams(lp);
        }
        //mHeaderView = header;
        addView(header);
    }
    public void setFooterView(View footer) {
        if (mFooterView != null && footer != null && mFooterView != footer) {
            removeView(mFooterView);
        }
        ViewGroup.LayoutParams lp = footer.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(-1, -2);
            footer.setLayoutParams(lp);
        }
        //mFooterView = footer;
        addView(footer);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p != null && p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    private void sendCancelEvent() {
        if (DEBUG) {
            PtrCLog.d(LOG_TAG, "send cancel event");
        }
        // The ScrollChecker will update position and lead to send cancel event when mLastMoveEvent is null.
        // fix #104, #80, #92
        if (mLastMoveEvent == null) {
            return;
        }
        MotionEvent last = mLastMoveEvent;
        MotionEvent e = MotionEvent.obtain(last.getDownTime(), last.getEventTime() + ViewConfiguration.getLongPressTimeout(), MotionEvent.ACTION_CANCEL, last.getX(), last.getY(), last.getMetaState());
        dispatchTouchEventSupper(e);
    }
    private void sendDownEvent() {
        if (DEBUG) {
            PtrCLog.d(LOG_TAG, "send down event");
        }
        final MotionEvent last = mLastMoveEvent;
        MotionEvent e = MotionEvent.obtain(last.getDownTime(), last.getEventTime(), MotionEvent.ACTION_DOWN, last.getX(), last.getY(), last.getMetaState());
        dispatchTouchEventSupper(e);
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        @SuppressWarnings({"unused"})
        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
    class ScrollChecker implements Runnable {

        private int mLastFlingY;
        private Scroller mScroller;
        private boolean mIsRunning = false;
        private int mStart;
        private int mTo;
        private boolean isPullToLoad=false;


        public ScrollChecker() {
            mScroller = new Scroller(getContext());
        }

        public void run() {
            boolean finish = !mScroller.computeScrollOffset() || mScroller.isFinished();
            int curY = mScroller.getCurrY();
            int deltaY = curY - mLastFlingY;
            if (DEBUG) {
                if (deltaY != 0) {
                    PtrCLog.v(LOG_TAG,
                            "scroll: %s, start: %s, to: %s, currentPos: %s, current :%s, last: %s, delta: %s",
                            finish, mStart, mTo, mPtrIndicator.getCurrentPosY(), curY, mLastFlingY, deltaY);
                }
            }
            if (!finish) {
                mLastFlingY = curY;
                if(isPullToLoad)
                    movePos2(deltaY);
                else
                    movePos(deltaY);

                post(this);
            } else {
                finish();
            }
        }
        private void finish() {
            if (DEBUG) {
                PtrCLog.v(LOG_TAG, "finish, currentPos:%s", mPtrIndicator.getCurrentPosY());
            }
            reset();
            onPtrScrollFinish();
        }
        private void reset() {
            mIsRunning = false;
            mLastFlingY = 0;
            removeCallbacks(this);
            isPullToLoad=false;
            PtrIndicator.isPullFooter=false;
            PtrIndicator.isPullHeader=false;
        }
        private void destroy() {
            reset();
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
        }

        public void abortIfWorking() {
            if (mIsRunning) {
                if (!mScroller.isFinished()) {
                    mScroller.forceFinished(true);
                }
                onPtrScrollAbort();
                reset();
            }
        }
        public void tryToScrollTo(int to, int duration,boolean isPullToLoad) {
            this.isPullToLoad=isPullToLoad;//判断上拉与否
            if (mPtrIndicator.isAlreadyHere(to)) {
                return;
            }
            mStart = mPtrIndicator.getCurrentPosY();
            mTo = to;
            int distance = to- mStart;
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "tryToScrollTo: start: %s, distance:%s, to:%s", mStart, distance, to);
            }
            removeCallbacks(this);
            mLastFlingY = 0;

            // fix #47: Scroller should be reused, https://github.com/liaohuqiu/android-Ultra-Pull-To-Refresh/issues/47
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
            mScroller.startScroll(0, 0, 0, distance, duration);
            post(this);
            mIsRunning = true;
        }
    }
}
