package in.srain.cube.views.ptr;

import in.srain.cube.views.ptr.indicator.PtrIndicator;

/**
 * A single linked list to wrap PtrUIFooterHandler
 */
class PtrUIFooterHandlerHolder implements PtrUIFooterHandler {

    private PtrUIFooterHandler mHandler;
    private PtrUIFooterHandlerHolder mNext;

    private boolean contains(PtrUIFooterHandler handler) {
        return mHandler != null && mHandler == handler;
    }

    private PtrUIFooterHandlerHolder() {

    }

    public boolean hasHandler() {
        return mHandler != null;
    }

    private PtrUIFooterHandler getHandler() {
        return mHandler;
    }

    public static void addHandler(PtrUIFooterHandlerHolder head, PtrUIFooterHandler handler) {

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

        PtrUIFooterHandlerHolder current = head;
        for (; ; current = current.mNext) {

            // duplicated
            if (current.contains(handler)) {
                return;
            }
            if (current.mNext == null) {
                break;
            }
        }

        PtrUIFooterHandlerHolder newHolder = new PtrUIFooterHandlerHolder();
        newHolder.mHandler = handler;
        current.mNext = newHolder;
    }

    public static PtrUIFooterHandlerHolder create() {
        return new PtrUIFooterHandlerHolder();
    }

    public static PtrUIFooterHandlerHolder removeHandler(PtrUIFooterHandlerHolder head, PtrUIFooterHandler handler) {
        if (head == null || handler == null || null == head.mHandler) {
            return head;
        }

        PtrUIFooterHandlerHolder current = head;
        PtrUIFooterHandlerHolder pre = null;
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
            head = new PtrUIFooterHandlerHolder();
        }
        return head;
    }

    @Override
    public void onUIReset(PtrFrameLayout frame) {
        PtrUIFooterHandlerHolder current = this;
        do {
            final PtrUIFooterHandler handler = current.getHandler();
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
        PtrUIFooterHandlerHolder current = this;
        do {
            final PtrUIFooterHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshPrepare(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshBegin(PtrFrameLayout frame) {
        PtrUIFooterHandlerHolder current = this;
        do {
            final PtrUIFooterHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshBegin(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshComplete(PtrFrameLayout frame) {
        PtrUIFooterHandlerHolder current = this;
        do {
            final PtrUIFooterHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshComplete(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {
        PtrUIFooterHandlerHolder current = this;
        do {
            final PtrUIFooterHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIPositionChange(frame, isUnderTouch, status, ptrIndicator);
            }
        } while ((current = current.mNext) != null);
    }
}
