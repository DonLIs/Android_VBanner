package me.donlis.vbanner;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.core.view.ViewCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import java.lang.reflect.Field;
import java.util.List;

import static me.donlis.vbanner.BannerAdapter.MAX_VALUE;

public class VBannerView<T> extends RelativeLayout implements LifecycleObserver {
    private ViewPager2 mViewPager;
    private BannerAdapter<T> mBannerPagerAdapter;

    //当前页面索引
    private volatile int mCurrentPosition;
    //
    private Handler mHandler;
    //时间间隔
    private int DEFAULT_INTERVAL = 2500;
    //切换动画时间
    private int mScrollDuration = 800;

    //是否自动播放
    private boolean autoPlay = false;

    private boolean isLooping;

    private int startX, startY;

    private boolean disallowIntercept;

    private LinearLayoutManager mLayout;

    //点击事件
    private OnItemClickListener<T> mOnItemClickListener;

    private OnPageChangeListener mOnPageChangeListener;

    public VBannerView(Context context) {
        super(context);
        initView();
    }

    public VBannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public VBannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    /**
     * 初始化
     */
    private void initView() {
        mHandler = new Handler(Looper.getMainLooper());
        //创建ViewPager
        mViewPager = new ViewPager2(getContext());
        measureLayoutRtl(mViewPager);
        mViewPager.setOverScrollMode(OVER_SCROLL_NEVER);
        mViewPager.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mViewPager.setPageTransformer(new CompositePageTransformer());

        addView(mViewPager);
    }

    private void measureLayoutRtl(ViewPager2 viewPager2) {
        if(viewPager2 == null){
            return;
        }
        try {
            Field field = viewPager2.getClass().getDeclaredField("mLayoutManager");
            field.setAccessible(true);
            mLayout = (LinearLayoutManager) field.get(viewPager2);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean isRtl(){
        if(mLayout != null){
            return mLayout.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL;
        }
        return false;
    }

    private ViewPager2.OnPageChangeCallback mOnPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            int size = mBannerPagerAdapter.getListSize();
            mCurrentPosition = BannerUtil.getRealPosition(isCanLoop(), position, size);
            if (size > 0 && isCanLoop() && position == 0 || position == MAX_VALUE - 1) {
                resetCurrentItem(mCurrentPosition);
            }
            if (mOnPageChangeListener != null) {
                if (isRtl()) {
                    mOnPageChangeListener.onSelected(size - 1 - mCurrentPosition);
                }else {
                    mOnPageChangeListener.onSelected(mCurrentPosition);
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
        }
    };

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isLooping = true;
                stop();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                isLooping = false;
                start();
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean canIntercept = mViewPager.isUserInputEnabled() || mBannerPagerAdapter != null && mBannerPagerAdapter.getData().size() <= 1;
        if (!canIntercept) {
            return super.onInterceptTouchEvent(ev);
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = (int) ev.getX();
                startY = (int) ev.getY();
                if (!disallowIntercept) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int endX = (int) ev.getX();
                int endY = (int) ev.getY();
                int disX = Math.abs(endX - startX);
                int disY = Math.abs(endY - startY);
                onHorizontalActionMove(endX, disX, disY);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
            case MotionEvent.ACTION_OUTSIDE:
            default:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    public VBannerView<T> setAdapter(BannerAdapter<T> adapter){
        mBannerPagerAdapter = adapter;
        return this;
    }

    /**
     *
     */
    public void initData(List<T> list) {
        if(mBannerPagerAdapter == null){
            throw new NullPointerException("You must set adapter for BannerSwitchView");
        }
        if (list == null || list.size() == 0) {
            return;
        }
        mCurrentPosition = 0;
        ScrollDurationManger.reflectLayoutManager(mViewPager, getScrollDuration());
        mBannerPagerAdapter.setCanLoop(true);
        mBannerPagerAdapter.setData(list);
        mBannerPagerAdapter.setOnItemClickListener(mOnItemClickListener);
        mViewPager.setAdapter(mBannerPagerAdapter);
        resetCurrentItem(mCurrentPosition);

        mViewPager.unregisterOnPageChangeCallback(mOnPageChangeCallback);
        mViewPager.registerOnPageChangeCallback(mOnPageChangeCallback);
        mViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        mViewPager.setOffscreenPageLimit(list.size());

        if(mOnPageChangeListener != null) {
            mOnPageChangeListener.onInitialize(list.size());
            if (isRtl()) {
                mOnPageChangeListener.onSelected(list.size() - 1 - mCurrentPosition);
            } else {
                mOnPageChangeListener.onSelected(mCurrentPosition);
            }
        }

        start();
    }

    public List<T> getData() {
        return mBannerPagerAdapter.getData();
    }

    public void refreshData(List<T> list) {
        if (list != null && list.size() > 0 && mBannerPagerAdapter != null) {
            stop();
            mCurrentPosition = 0;
            mBannerPagerAdapter.setData(list);
            mBannerPagerAdapter.notifyDataSetChanged();
            mViewPager.setOffscreenPageLimit(list.size());
            if(mOnPageChangeListener != null) {
                mOnPageChangeListener.onInitialize(list.size());
                if (isRtl()) {
                    mOnPageChangeListener.onSelected(list.size() - 1 - mCurrentPosition);
                } else {
                    mOnPageChangeListener.onSelected(mCurrentPosition);
                }
            }
            resetCurrentItem(mCurrentPosition);
            start();
        }
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            handlePosition();
        }
    };

    private void handlePosition() {
        if (mBannerPagerAdapter.getListSize() > 1 && isAutoPlay()) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
            mHandler.postDelayed(mRunnable, getInterval());
        }
    }

    /**
     * 开启播放
     */
    public void start() {
        if (!isLooping && isAutoPlay() && mBannerPagerAdapter != null &&
                mBannerPagerAdapter.getListSize() > 1) {
            mHandler.postDelayed(mRunnable, getInterval());
            isLooping = true;
        }
    }

    /**
     * 停止播放
     *
     * @return
     */
    public void stop() {
        if (isLooping) {
            mHandler.removeCallbacks(mRunnable);
            isLooping = false;
        }
    }

    public boolean isCanLoop() {
        return true;
    }

    public boolean isAutoPlay(){
        return autoPlay;
    }

    public VBannerView<T> setAutoPlay(boolean auto){
        autoPlay = auto;
        return this;
    }

    private void resetCurrentItem(int item) {
        if (isCanLoop() && mBannerPagerAdapter.getListSize() > 1) {
            mViewPager.setCurrentItem(MAX_VALUE / 2 - ((MAX_VALUE / 2) % mBannerPagerAdapter.getListSize()) + 1 + item, false);
        } else {
            mViewPager.setCurrentItem(item, false);
        }
    }

    /**
     * 设置切换时间间隔
     *
     */
    public VBannerView<T> setInterval(int interval) {
        DEFAULT_INTERVAL = interval;
        return this;
    }

    public int getInterval(){
        return DEFAULT_INTERVAL;
    }

    public VBannerView<T> setLifecycleRegistry(Lifecycle lifecycleRegistry) {
        lifecycleRegistry.addObserver(this);
        return this;
    }

    private void onVerticalActionMove(int endY, int disX, int disY) {
        if (disY > disX) {
            if (!isCanLoop()) {
                if (mCurrentPosition == 0 && endY - startY > 0) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else if (mCurrentPosition == getData().size() - 1 && endY - startY < 0) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            } else {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        } else if (disX > disY) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
    }

    private void onHorizontalActionMove(int endX, int disX, int disY) {
        if (disX > disY) {
            if (!isCanLoop()) {
                if (mCurrentPosition == 0 && endX - startX > 0) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else if (mCurrentPosition == getData().size() - 1 && endX - startX < 0) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            } else {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        } else if (disY > disX) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        stop();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        start();
    }

    public VBannerView<T> setScrollDuration(int scrollDuration){
        this.mScrollDuration = scrollDuration;
        return this;
    }

    public int getScrollDuration(){
        return this.mScrollDuration;
    }

    public VBannerView<T> disallowInterceptTouchEvent(boolean disallowIntercept) {
        this.disallowIntercept = disallowIntercept;
        return this;
    }

    public VBannerView<T> setOnItemClickListener(OnItemClickListener<T> listener) {
        this.mOnItemClickListener = listener;
        return this;
    }

    public OnPageChangeListener getOnPageChangeListener() {
        return mOnPageChangeListener;
    }

    public VBannerView<T> setOnPageChangeListener(OnPageChangeListener listener) {
        this.mOnPageChangeListener = listener;
        return this;
    }

    public interface OnItemClickListener<T> {
        void onItemClick(T data, int position);
    }

    public interface OnPageChangeListener {
        void onInitialize(int totalSize);
        void onSelected(int position);
    }
}
