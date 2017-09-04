package in.srain.cube.views.ptr;

import in.srain.cube.views.ptr.indicator.PtrIndicator;

/**
 * A single linked list to wrap PtrUIFooter
 */
class PtrUIFooterHolder implements PtrUIFooter {

    private PtrUIFooter mHandler;
    private PtrUIFooterHolder mNext;

    private boolean contains(PtrUIFooter handler) {
        return mHandler != null && mHandler == handler;
    }

    private PtrUIFooterHolder() {

    }

    public boolean hasHandler() {
        return mHandler != null;
    }

    private PtrUIFooter getHandler() {
        return mHandler;
    }

    public static void addHandler(PtrUIFooterHolder head, PtrUIFooter handler) {

        if (null == handler) {
            return;
        }
        if (head == null) {
            return;
        }
        if (null == head.mHandler) {
            head.mHandler = handler;
            return;
        }

        PtrUIFooterHolder current = head;
        for (; ; current = current.mNext) {

            // duplicated
            if (current.contains(handler)) {
                return;
            }
            if (current.mNext == null) {
                break;
            }
        }

        PtrUIFooterHolder newHolder = new PtrUIFooterHolder();
        newHolder.mHandler = handler;
        current.mNext = newHolder;
    }

    public static PtrUIFooterHolder create() {
        return new PtrUIFooterHolder();
    }

    public static PtrUIFooterHolder removeHandler(PtrUIFooterHolder head, PtrUIFooter handler) {
        if (head == null || handler == null || null == head.mHandler) {
            return head;
        }

        PtrUIFooterHolder current = head;
        PtrUIFooterHolder pre = null;
        do {

            // delete current: link pre to next, unlink next from current;
            // pre will no change, current move to next element;
            if (current.contains(handler)) {

                // current is head
                if (pre == null) {

                    head = current.mNext;
                    current.mNext = null;

                    current = head;
                } else {

                    pre.mNext = current.mNext;
                    current.mNext = null;
                    current = pre.mNext;
                }
            } else {
                pre = current;
                current = current.mNext;
            }

        } while (current != null);

        if (head == null) {
            head = new PtrUIFooterHolder();
        }
        return head;
    }

    @Override
    public void onUIReset(PtrFrameLayout frame) {
        PtrUIFooterHolder current = this;
        do {
            final PtrUIFooter handler = current.getHandler();
            if (null != handler) {
                handler.onUIReset(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshPrepare(PtrFrameLayout frame) {
        if (!hasHandler()) {
            return;
        }
        PtrUIFooterHolder current = this;
        do {
            final PtrUIFooter handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshPrepare(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshBegin(PtrFrameLayout frame) {
        PtrUIFooterHolder current = this;
        do {
            final PtrUIFooter handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshBegin(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshComplete(PtrFrameLayout frame) {
        PtrUIFooterHolder current = this;
        do {
            final PtrUIFooter handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshComplete(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {
        PtrUIFooterHolder current = this;
        do {
            final PtrUIFooter handler = current.getHandler();
            if (null != handler) {
                handler.onUIPositionChange(frame, isUnderTouch, status, ptrIndicator);
            }
        } while ((current = current.mNext) != null);
    }
}
