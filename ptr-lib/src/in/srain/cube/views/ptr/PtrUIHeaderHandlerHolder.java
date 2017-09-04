package in.srain.cube.views.ptr;

import in.srain.cube.views.ptr.indicator.PtrIndicator;

/**
 * A single linked list to wrap PtrUIHeaderHandler
 */
class PtrUIHeaderHandlerHolder implements PtrUIHeaderHandler {

    private PtrUIHeaderHandler mHandler;
    private PtrUIHeaderHandlerHolder mNext;

    private boolean contains(PtrUIHeaderHandler handler) {
        return mHandler != null && mHandler == handler;
    }

    private PtrUIHeaderHandlerHolder() {

    }

    public boolean hasHandler() {
        return mHandler != null;
    }

    private PtrUIHeaderHandler getHandler() {
        return mHandler;
    }

    public static void addHandler(PtrUIHeaderHandlerHolder head, PtrUIHeaderHandler handler) {

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

        PtrUIHeaderHandlerHolder current = head;
        for (; ; current = current.mNext) {

            // duplicated
            if (current.contains(handler)) {
                return;
            }
            if (current.mNext == null) {
                break;
            }
        }

        PtrUIHeaderHandlerHolder newHolder = new PtrUIHeaderHandlerHolder();
        newHolder.mHandler = handler;
        current.mNext = newHolder;
    }

    public static PtrUIHeaderHandlerHolder create() {
        return new PtrUIHeaderHandlerHolder();
    }

    public static PtrUIHeaderHandlerHolder removeHandler(PtrUIHeaderHandlerHolder head, PtrUIHeaderHandler handler) {
        if (head == null || handler == null || null == head.mHandler) {
            return head;
        }

        PtrUIHeaderHandlerHolder current = head;
        PtrUIHeaderHandlerHolder pre = null;
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
            head = new PtrUIHeaderHandlerHolder();
        }
        return head;
    }

    @Override
    public void onUIReset(PtrFrameLayout frame) {
        PtrUIHeaderHandlerHolder current = this;
        do {
            final PtrUIHeaderHandler handler = current.getHandler();
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
        PtrUIHeaderHandlerHolder current = this;
        do {
            final PtrUIHeaderHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshPrepare(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshBegin(PtrFrameLayout frame) {
        PtrUIHeaderHandlerHolder current = this;
        do {
            final PtrUIHeaderHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshBegin(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshComplete(PtrFrameLayout frame) {
        PtrUIHeaderHandlerHolder current = this;
        do {
            final PtrUIHeaderHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshComplete(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {
        PtrUIHeaderHandlerHolder current = this;
        do {
            final PtrUIHeaderHandler handler = current.getHandler();
            if (null != handler) {
                handler.onUIPositionChange(frame, isUnderTouch, status, ptrIndicator);
            }
        } while ((current = current.mNext) != null);
    }
}
