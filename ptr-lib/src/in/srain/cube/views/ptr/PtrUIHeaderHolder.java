package in.srain.cube.views.ptr;

/**
 * A single linked list to wrap PtrUIHeader
 */
class PtrUIHeaderHolder implements PtrUIHeader {

    private PtrUIHeader mHandler;
    private PtrUIHeaderHolder mNext;

    private boolean contains(PtrUIHeader handler) {
        return mHandler != null && mHandler == handler;
    }

    private PtrUIHeaderHolder() {

    }

    public boolean hasHandler() {
        return mHandler != null;
    }

    private PtrUIHeader getHandler() {
        return mHandler;
    }

    public static void addHandler(PtrUIHeaderHolder head, PtrUIHeader handler) {

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

        PtrUIHeaderHolder current = head;
        for (; ; current = current.mNext) {

            // duplicated
            if (current.contains(handler)) {
                return;
            }
            if (current.mNext == null) {
                break;
            }
        }

        PtrUIHeaderHolder newHolder = new PtrUIHeaderHolder();
        newHolder.mHandler = handler;
        current.mNext = newHolder;
    }

    public static PtrUIHeaderHolder create() {
        return new PtrUIHeaderHolder();
    }

    public static PtrUIHeaderHolder removeHandler(PtrUIHeaderHolder head, PtrUIHeader handler) {
        if (head == null || handler == null || null == head.mHandler) {
            return head;
        }

        PtrUIHeaderHolder current = head;
        PtrUIHeaderHolder pre = null;
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
            head = new PtrUIHeaderHolder();
        }
        return head;
    }

    @Override
    public void onUIReset(PtrFrameLayout frame) {
        PtrUIHeaderHolder current = this;
        do {
            final PtrUIHeader handler = current.getHandler();
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
        PtrUIHeaderHolder current = this;
        do {
            final PtrUIHeader handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshPrepare(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshBegin(PtrFrameLayout frame) {
        PtrUIHeaderHolder current = this;
        do {
            final PtrUIHeader handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshBegin(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshComplete(PtrFrameLayout frame) {
        PtrUIHeaderHolder current = this;
        do {
            final PtrUIHeader handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshComplete(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {
        PtrUIHeaderHolder current = this;
        do {
            final PtrUIHeader handler = current.getHandler();
            if (null != handler) {
                handler.onUIPositionChange(frame, isUnderTouch, status, ptrIndicator);
            }
        } while ((current = current.mNext) != null);
    }
}
