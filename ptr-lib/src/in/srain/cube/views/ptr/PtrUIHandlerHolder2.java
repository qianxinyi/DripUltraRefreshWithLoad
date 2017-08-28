package in.srain.cube.views.ptr;

import in.srain.cube.views.ptr.indicator.PtrIndicator;

/**
 * A single linked list to wrap PtrUIHandler2
 */
class PtrUIHandlerHolder2 implements PtrUIHandler2 {

    private PtrUIHandler2 mHandler;
    private PtrUIHandlerHolder2 mNext;

    private boolean contains(PtrUIHandler2 handler) {
        return mHandler != null && mHandler == handler;
    }

    private PtrUIHandlerHolder2() {

    }

    public boolean hasHandler() {
        return mHandler != null;
    }

    private PtrUIHandler2 getHandler() {
        return mHandler;
    }

    public static void addHandler(PtrUIHandlerHolder2 head, PtrUIHandler2 handler) {

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

        PtrUIHandlerHolder2 current = head;
        for (; ; current = current.mNext) {

            // duplicated
            if (current.contains(handler)) {
                return;
            }
            if (current.mNext == null) {
                break;
            }
        }

        PtrUIHandlerHolder2 newHolder = new PtrUIHandlerHolder2();
        newHolder.mHandler = handler;
        current.mNext = newHolder;
    }

    public static PtrUIHandlerHolder2 create() {
        return new PtrUIHandlerHolder2();
    }

    public static PtrUIHandlerHolder2 removeHandler(PtrUIHandlerHolder2 head, PtrUIHandler2 handler) {
        if (head == null || handler == null || null == head.mHandler) {
            return head;
        }

        PtrUIHandlerHolder2 current = head;
        PtrUIHandlerHolder2 pre = null;
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
            head = new PtrUIHandlerHolder2();
        }
        return head;
    }

    @Override
    public void onUIReset(PtrFrameLayout frame) {
        PtrUIHandlerHolder2 current = this;
        do {
            final PtrUIHandler2 handler = current.getHandler();
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
        PtrUIHandlerHolder2 current = this;
        do {
            final PtrUIHandler2 handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshPrepare(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshBegin(PtrFrameLayout frame) {
        PtrUIHandlerHolder2 current = this;
        do {
            final PtrUIHandler2 handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshBegin(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshComplete(PtrFrameLayout frame) {
        PtrUIHandlerHolder2 current = this;
        do {
            final PtrUIHandler2 handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshComplete(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {
        PtrUIHandlerHolder2 current = this;
        do {
            final PtrUIHandler2 handler = current.getHandler();
            if (null != handler) {
                handler.onUIPositionChange(frame, isUnderTouch, status, ptrIndicator);
            }
        } while ((current = current.mNext) != null);
    }
}
