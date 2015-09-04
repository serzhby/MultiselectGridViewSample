package by.serzh.multiselectgridview;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridView;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class MultiselectGridView extends GridView {

    private static final int START_SELECT_DELAY = 200;
    private static final int START_SELECT_MODE_DELAY = 500;
    private static int AUTO_SCROLL_DELTA;
    private static int DRAG_THRESHOLD;

    private static int SCROLL_EDGE;

    private boolean isInSelectMode = false;
    private boolean isSelecting = false;
    private int prevX;
    private int prevY;

    private long prevTouchTime;
    private int touchedId = -1;
    private int startTouchId = -1;
    private boolean wasDragged;

    private ValueAnimator slowScrollUpAnimator;
    private ValueAnimator slowScrollDownAnimator;

    private CountDownTimer startSelectModeCountDown = new CountDownTimer(START_SELECT_MODE_DELAY, START_SELECT_MODE_DELAY + 1) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            startSelect();
        }
    };

    private MultiselectGridViewAdapter adapter;

    public MultiselectGridView(Context context) {
        super(context);
        init();
    }

    public MultiselectGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MultiselectGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public MultiselectGridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        DRAG_THRESHOLD = dpToPx(4);
        AUTO_SCROLL_DELTA = dpToPx(2);
        SCROLL_EDGE = dpToPx(50);
    }

    public void setAdapter(MultiselectGridViewAdapter adapter) {
        this.adapter = adapter;
        super.setAdapter(adapter);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if(adapter instanceof MultiselectGridViewAdapter) {
            this.adapter = (MultiselectGridViewAdapter) adapter;
            setAdapter((MultiselectGridViewAdapter) adapter);
        } else {
            throw new IllegalArgumentException("Adapter must be of type " + MultiselectGridViewAdapter.class.getSimpleName());
        }
    }

    public void startSelect() {
        isInSelectMode = true;
        if(adapter != null) {
            adapter.isInSelectMode = true;
            adapter.notifyDataSetChanged();
        }
    }

    public void finishSelect() {
        isInSelectMode = false;
        if(adapter != null) {
            adapter.selected.clear();
            adapter.isInSelectMode = false;
            adapter.notifyDataSetChanged();
        }
        stopSlowScrollDown();
        stopSlowScrollUp();
    }

    public boolean isInSelectMode() {
        return isInSelectMode;
    }

    public Collection<Integer> getSelectedItems() {
        Set<Integer> result = new TreeSet<>();
        result.addAll(adapter.selected);
        return result;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {

        if(adapter == null) {
            return super.onTouchEvent(event);
        }

        int x = (int) event.getX();
        int y = (int) event.getY();


        if(!isInSelectMode) {

            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                startSelectModeCountDown.start();

                prevX = x;
                prevY = y;
            } else if(event.getAction() == MotionEvent.ACTION_MOVE) {
                if(prevX > 0 && prevY > 0 && Math.abs(prevX - x) > DRAG_THRESHOLD || Math.abs(prevY - y) > DRAG_THRESHOLD) {
                    startSelectModeCountDown.cancel();
                }
                prevX = x;
                prevY = y;
            } else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                startSelectModeCountDown.cancel();
                prevX = -1;
                prevY = -1;
            }
            return super.onTouchEvent(event);

        } else {

            if (prevTouchTime != 0 && System.currentTimeMillis() - prevTouchTime > START_SELECT_DELAY) {
                isSelecting = true;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {

                if(prevX >= 0 && prevY >= 0
                        && event.getAction() == MotionEvent.ACTION_MOVE
                        && (Math.abs(prevX - x) > DRAG_THRESHOLD || Math.abs(prevY - y) > DRAG_THRESHOLD)) {
                    wasDragged = true;
                }

                if (isSelecting) {
                    View touchedView = findTouchedView(x, y);
                    if (touchedView != null) {
                        int current = (Integer) touchedView.getTag();
                        handleElementTouch(current);
                        touchedId = current;
                    }

                    if(y > getHeight() - SCROLL_EDGE) {
                        startSlowScrollDown();
                    } else {
                        stopSlowScrollDown();
                    }

                    if(y < SCROLL_EDGE) {
                        startSlowScrollUp();
                    } else {
                        stopSlowScrollUp();
                    }
                }
                prevX = x;
                prevY = y;
                prevTouchTime = System.currentTimeMillis();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if(!wasDragged) {
                    View view = findTouchedView(x, y);
                    if(view != null) {
                        int current = (Integer) view.getTag();
                        if(adapter.selected.contains(current)) {
                            adapter.selected.remove(current);
                        } else {
                            adapter.selected.add(current);
                        }
                        adapter.notifyDataSetChanged();
                    }
                }

                isSelecting = false;
                prevTouchTime = 0;
                touchedId = -1;
                startTouchId = -1;
                startSelectModeCountDown.cancel();
                prevX = -1;
                prevY = -1;
                wasDragged = false;
            }

            return isSelecting || super.onTouchEvent(event);
        }

    }

    private void handleElementTouch(int current) {
        if (startTouchId < 0) {
            startTouchId = current;
        }

        if(current > startTouchId) {
            if (current != touchedId && touchedId >= 0) {

                if (current < touchedId) {
                    for (int i = touchedId; i > current; i--) {
                        adapter.selected.remove(i);
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    for (int i = touchedId; i <= current; i++) {
                        adapter.selected.add(i);
                        adapter.notifyDataSetChanged();
                    }
                }

            } else if (!adapter.selected.contains(current)) {
                adapter.selected.add(current);
                adapter.notifyDataSetChanged();
            }
        } else if(current < startTouchId) {
            if (current != touchedId && touchedId >= 0) {

                if (current < touchedId) {
                    for (int i = touchedId; i > current; i--) {
                        adapter.selected.add(i);
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    for (int i = touchedId; i <= current; i++) {
                        adapter.selected.remove(i);
                        adapter.notifyDataSetChanged();
                    }
                }


            } else if (!adapter.selected.contains(current)) {
                adapter.selected.add(current);
                adapter.notifyDataSetChanged();
            }
        } else if(touchedId >= 0 && startTouchId >= 0) {
            if (startTouchId < touchedId) {
                for (int i = touchedId; i > startTouchId; i--) {
                    adapter.selected.remove(i);
                    adapter.notifyDataSetChanged();
                }
            } else {
                for (int i = touchedId; i < startTouchId; i++) {
                    adapter.selected.remove(i);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    private View findTouchedView(int x, int y) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getLeft() < x && child.getRight() > x
                    && child.getTop() < y && child.getBottom() > y) {
                return child;
            }
        }
        return null;
    }

    private int dpToPx(float dp) {
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int) px;
    }

    private void startSlowScrollUp() {
        if(slowScrollUpAnimator == null) {
            slowScrollUpAnimator = ValueAnimator.ofInt(0, 1);
            slowScrollUpAnimator.setDuration(20);
            slowScrollUpAnimator.setRepeatCount(ValueAnimator.INFINITE);
            slowScrollUpAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    //scrollListBy(-AUTO_SCROLL_DELTA);
                    smoothScrollBy(-AUTO_SCROLL_DELTA, 0);
                }
            });
        } else if(!slowScrollUpAnimator.isRunning()) {
            slowScrollUpAnimator.start();
        }
    }

    private void stopSlowScrollUp() {
        if(slowScrollUpAnimator != null) {
            slowScrollUpAnimator.cancel();
        }
    }

    private void startSlowScrollDown() {
        if(slowScrollDownAnimator == null) {
            slowScrollDownAnimator = ValueAnimator.ofInt(0, 1);
            slowScrollDownAnimator.setDuration(20);
            slowScrollDownAnimator.setRepeatCount(ValueAnimator.INFINITE);
            slowScrollDownAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    //scrollListBy(AUTO_SCROLL_DELTA);
                    smoothScrollBy(AUTO_SCROLL_DELTA, 0);
                }
            });
        } else if(!slowScrollDownAnimator.isRunning()) {
            slowScrollDownAnimator.start();
        }
    }

    private void stopSlowScrollDown() {
        if(slowScrollDownAnimator != null) {
            slowScrollDownAnimator.cancel();
        }
    }
}
