package com.zhiyuan.draglib;

import android.animation.Animator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 可拖拽Gridlayout
 *
 * @author zhiyuan
 * @date 2018/5/7
 */

public class DragGridLayout extends ViewGroup {
    public static final String TAG = "DragGridLayout";

    /** 动画时间，毫秒 */
    private static final int ANIM_DURATION = 100;

    /** 拖拽监听 */
    private DragListener mDragListener = new DragListener();
    private DragLongClickListener mLongClickListener = new DragLongClickListener();

    /**正在拖动的view*/
    private View mCurrentDragView;
    /**当前被移动到的viewview*/
    private View mCurrentAnimToView;

    /** 列数 */
    private int mColumnCount = 4;
    /** 子控件最高的高度 */
    private int mChildMaxHeight = 0;
    /** 子控件最大宽度 */
    private int mChildMaxWidth = 0;
    /** 子控件距离上边距离 */
    private int mChildMarginTop = 0;

    /** 是否正在动画中 */
    private boolean mAnimating = false;

    /** 是否允许拖拽*/
    private boolean mDragEnabled = true;

    /**
     * 布局参数
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        /** 顺序，第一个为0，依次加1，布局时需要使用此参数*/
        int order;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }


    public DragGridLayout(Context context) {
        this(context, null, 0);
    }

    public DragGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragGridLayout(final Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DragGridLayout);
            mChildMarginTop = a.getDimensionPixelSize(R.styleable.DragGridLayout_childMarginTop,
                    mChildMarginTop);
            mColumnCount = a.getInteger(R.styleable.DragGridLayout_columnCount, mColumnCount);
            mDragEnabled = a.getBoolean(R.styleable.DragGridLayout_dragEnabled, mDragEnabled);
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mChildMaxHeight = 0;
        mChildMaxWidth = 0;
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int childCount = getChildCount();
        int height;
        if (childCount == 0) {
            height = 0;
        } else {
            //行数 + 上下padding
            int lineCount = childCount % mColumnCount == 0 ?
                    childCount / mColumnCount : childCount / mColumnCount + 1;
            height = (lineCount) * (mChildMaxHeight + mChildMarginTop)
                    + getPaddingTop() + getPaddingBottom();
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        super.measureChild(child, parentWidthMeasureSpec, parentHeightMeasureSpec);
        int childHeight = MeasureSpec.getSize(child.getMeasuredHeight());
        int childWidth = MeasureSpec.getSize(child.getMeasuredWidth());
        mChildMaxHeight = mChildMaxHeight > childHeight ? mChildMaxHeight : childHeight;
        mChildMaxWidth = mChildMaxWidth > childWidth ? mChildMaxWidth : childWidth;
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //每个item宽度
        int columnWidth = (right - left - getPaddingLeft() - getPaddingRight()) / mColumnCount;
        //列宽超出view宽度的距离
        int widthOffset = (columnWidth - mChildMaxWidth) / 2;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int order = ((LayoutParams)child.getLayoutParams()).order;

            child.setTranslationX(0);
            child.setTranslationY(0);

            int column = order % mColumnCount;
            int row = order / mColumnCount;

            int childLeft = column * columnWidth + widthOffset + getPaddingLeft();
            int childTop = row * (mChildMaxHeight + mChildMarginTop) + getPaddingTop();

            child.layout(childLeft,
                    childTop,
                    childLeft + child.getMeasuredWidth(),
                    childTop + child.getMeasuredHeight());
        }
    }


    @Override
    public void addView(final View child, int index, ViewGroup.LayoutParams params) {
        ((LayoutParams) params).order = newLastOrder();
        super.addView(child, index, params);

        if (mDragEnabled) {
            child.setOnDragListener(mDragListener);
            child.setOnLongClickListener(mLongClickListener);
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }


    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p.width, p.height);
    }

    /**
     * 获取按照顺序的child
     * @return List 被排序的子控件
     */
    public List<View> getChildViewsByOrder() {
        final int childCount = getChildCount();
        List<View> result = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            result.add(getChildAt(i));
        }
        Collections.sort(result, new Comparator<View>() {
            @Override
            public int compare(View o1, View o2) {
                return ((LayoutParams)o1.getLayoutParams()).order -
                        ((LayoutParams)o2.getLayoutParams()).order;
            }
        });
        return result;
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        didChildViewRemoved();
    }

    @Override
    public void removeViewAt(int index) {
        super.removeViewAt(index);
        didChildViewRemoved();
    }

    @Override
    public void removeViews(int start, int count) {
        super.removeViews(start, count);
        didChildViewRemoved();
    }

    @Override
    public void removeViewsInLayout(int start, int count) {
        super.removeViewsInLayout(start, count);
        didChildViewRemoved();

    }

    @Override
    public void removeViewInLayout(View view) {
        super.removeViewInLayout(view);
        didChildViewRemoved();
    }

    /**
     * 创建一个新的最大的顺序
     * @return
     */
    public int newLastOrder() {
        int childCount = getChildCount();
        if (childCount == 0) {
            return 0;
        }
        View lastView = getChildAt(childCount - 1);
        return ((LayoutParams)lastView.getLayoutParams()).order + 1;
    }

    /**
     * 设置是否允许拖动
     * @param enable
     */
    public void setDragEnabled(boolean enable) {
        if (mDragEnabled != enable) {
            final int childCount = getChildCount();
            DragListener dragListener = enable ? mDragListener : null;
            OnLongClickListener longClickListener = enable ? mLongClickListener : null;
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).setOnDragListener(dragListener);
                getChildAt(i).setOnLongClickListener(longClickListener);
            }
            this.mDragEnabled = enable;
        }
    }


    /**
     * 重新排序
     * @param dirView 目标
     */
    private void reorderView(final View dirView) {
        View srcView = mCurrentDragView;
        if (srcView == dirView) {
            return;
        }

        //使用order确定当前两者位置，交换后要交换order
        int startOrder = ((LayoutParams) srcView.getLayoutParams()).order;
        int endOrder = ((LayoutParams) dirView.getLayoutParams()).order;

        if (startOrder == endOrder) {
            return;
        }

        if (mAnimating) {
            Log.i(TAG, "当前正在动画中，忽略移动" + startOrder + " 到 " + endOrder + "请求");
            return;
        }
        mAnimating = true;

        final int totalAnimCount = Math.abs(endOrder - startOrder) + 1;
        Animator.AnimatorListener listener = new Animator.AnimatorListener() {
            private int animCount = totalAnimCount;
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                animCount--;
                if (animCount == 0) {
                    //所有动画结束
                    mAnimating = false;
                    //如果当前没有停留在拖动的view上，移动
                    if (mCurrentAnimToView != dirView) {
                        //最后需要移动的不是当前移动的view
                        reorderView(mCurrentAnimToView);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        };

        Map<View, Integer> newOrder = new HashMap<>(totalAnimCount);
        //查询方向
        int direction = startOrder < endOrder ? 1 : -1;
        //当前移动的view
        int currentMoveItem = startOrder + direction;
        while (currentMoveItem - direction != endOrder) {
            animateStartToEnd(currentMoveItem, currentMoveItem - direction, listener);
            newOrder.put(getPositionView(currentMoveItem), currentMoveItem - direction);
            currentMoveItem = currentMoveItem + direction;
        }
        animateStartToEnd(startOrder, endOrder, listener);
        newOrder.put(getPositionView(startOrder), endOrder);

        //更改新的顺序
        for (Map.Entry<View, Integer> item : newOrder.entrySet()) {
            Log.i(TAG, "原始位置：" + ((LayoutParams)item.getKey().getLayoutParams()).order);
            Log.i(TAG, "新的位置：" + item.getValue());
            ((LayoutParams)item.getKey().getLayoutParams()).order = item.getValue();
        }

        for (int i = 0; i < getChildCount(); i++) {
            Log.i(TAG, "更改后的顺序：" + i + "   index:" +
                    ((LayoutParams) getChildAt(i).getLayoutParams()).order);
        }

    }

    /**
     * 动画将开始位置的view移动到结束view
     * @param startOrder 开始view的位置
     * @param endOrder 结束位置的view
     * @param listener 监听
     */
    private void animateStartToEnd(int startOrder, int endOrder,
                                   Animator.AnimatorListener listener) {
        final View startView = getPositionView(startOrder);
        final View endView = getPositionView(endOrder);
        if (startView == null || endView == null) {
            Log.e(TAG, "原始位置：" + startOrder +
                    " 目标位置：" + endOrder + "原始view或者目标view为空， " +
                    "原始View ：" + startView + " 目标View ：" + endView);
            if (BuildConfig.DEBUG) {
                throw new NullPointerException(
                        "原始位置：" + startOrder +
                        " 目标位置：" + endOrder + "原始view或者目标view为空， " +
                        "原始View ：" + startView + " 目标View ：" + endView);
            }
            return;
        }
        startView.animate()
                .x(endView.getX())
                .y(endView.getY())
                .setListener(listener)
                .setDuration(ANIM_DURATION)
                .start();
    }

    /**
     * 获取某个位置的view
     * @param order
     * @return
     */
    private View getPositionView(int order) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams params = (LayoutParams) getChildAt(i).getLayoutParams();
            if (params.order == order) {
                return getChildAt(i);
            }
        }
        return null;
    }


    /**
     * child view 被移除后调用，重新设置order
     */
    private void didChildViewRemoved() {
        final int childCount = getChildCount();

        //获取目前的排序顺序
        List<View> sortedByOrder = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            sortedByOrder.add(getChildAt(i));
        }
        Collections.sort(sortedByOrder, new Comparator<View>() {
            @Override
            public int compare(View o1, View o2) {
                LayoutParams o1Params = (LayoutParams) o1.getLayoutParams();
                LayoutParams o2Params = (LayoutParams) o2.getLayoutParams();
                return o1Params.order - o2Params.order;
            }
        });

        for (int order = 0; order < childCount; order++) {
            ((LayoutParams)sortedByOrder.get(order).getLayoutParams()).order = order;
        }

    }

    /**
     * 开始拖拽
     */
    private class DragLongClickListener implements View.OnLongClickListener {


        @Override
        public boolean onLongClick(View v) {
            if (!mDragEnabled) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.startDragAndDrop(null,
                        new View.DragShadowBuilder(v), null, 0);
            } else {
                v.startDrag(null,
                        new View.DragShadowBuilder(v), null, 0);
            }
            v.setAlpha(0.1f);
            mCurrentDragView = v;
            return true;
        }
    }




    /**
     * 拖拽事件监听
     */
    private class DragListener implements View.OnDragListener {

        @Override
        public boolean onDrag(View v, DragEvent event) {
            if (!mDragEnabled) {
                return false;
            }

            int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    v.setBackgroundColor(Color.BLUE);
                    v.invalidate();
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:

                    float x = event.getX();
                    float y = event.getY();
                    Log.i(TAG, "拖拽进入：" + v.getTag() + "  x:" + x + "  y:" + y);

                    if (mCurrentDragView != null) {
                        mCurrentAnimToView = v;
                        reorderView(v);
                    }

                    v.setBackgroundColor(Color.GREEN);
                    v.invalidate();
                    break;

                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundColor(Color.YELLOW);
                    v.invalidate();
                    break;
                case DragEvent.ACTION_DROP:
                    v.setBackgroundColor(0xffcccccc);
                    v.invalidate();
                    v.setAlpha(1f);
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundColor(0xffcccccc);
                    v.invalidate();
                    mCurrentDragView = null;
                    v.setAlpha(1f);
                    break;
                default:
                    break;
            }

            return true;
        }
    }


}
