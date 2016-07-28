package com.youth.banner;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.annotation.AnimatorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class Banner extends FrameLayout implements ViewPager.OnPageChangeListener {
    public String tag="banner";
    public static final int NOT_INDICATOR=0;
    public static final int CIRCLE_INDICATOR=1;
    public static final int NUM_INDICATOR=2;
    public static final int NUM_INDICATOR_TITLE=3;
    public static final int CIRCLE_INDICATOR_TITLE=4;
    public static final int LEFT=5;
    public static final int CENTER=6;
    public static final int RIGHT=7;
    private int mIndicatorMargin = 5;
    private int mIndicatorWidth = 8;
    private int mIndicatorHeight = 8;
    private int mIndicatorSelectedResId = R.drawable.gray_radius;
    private int mIndicatorUnselectedResId = R.drawable.white_radius;
    private int mIndicatorAnimatorResId = R.animator.scale_with_alpha;
    private int mIndicatorAnimatorReverseResId = 0;
    private int bannerStyle=CIRCLE_INDICATOR;
    private int count;
    private int currentItem;
    private int delayTime=2000;
    private int gravity=-1;
    private boolean isAutoPlay=true;
    private List<ImageView> imageViews;
    private List<ImageView> indicatorImages;
    private Context context;
    private ViewPager viewPager;
    private LinearLayout indicator;
    private Handler handler = new Handler();
    private OnBannerClickListener listener;
    private OnLoadImageListener imageListener;
    private String[] titles;
    private TextView bannerTitle , numIndicator;
    private int lastPosition=1;

    public Banner(Context context) {
        this(context, null);
    }
    public Banner(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Banner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        imageViews = new ArrayList<ImageView>();
        indicatorImages = new ArrayList<ImageView>();
        initView(context, attrs);
    }
    private void handleTypedArray(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Banner);
        mIndicatorWidth =typedArray.getDimensionPixelSize(R.styleable.Banner_indicator_width, 8);
        mIndicatorHeight =typedArray.getDimensionPixelSize(R.styleable.Banner_indicator_height, 8);
        mIndicatorMargin =typedArray.getDimensionPixelSize(R.styleable.Banner_indicator_margin, 5);
        mIndicatorSelectedResId =typedArray.getResourceId(R.styleable.Banner_indicator_drawable_selected, R.drawable.gray_radius);
        mIndicatorUnselectedResId =typedArray.getResourceId(R.styleable.Banner_indicator_drawable_unselected, R.drawable.white_radius);
        mIndicatorAnimatorResId = typedArray.getResourceId(R.styleable.Banner_indicator_animator, R.animator.scale_with_alpha);
        mIndicatorAnimatorReverseResId = typedArray.getResourceId(R.styleable.Banner_indicator_animator_reverse, 0);
        typedArray.recycle();
    }
    private void initView(Context context, AttributeSet attrs) {
        imageViews.clear();
        View view = LayoutInflater.from(context).inflate(R.layout.banner, this, true);
        viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        indicator = (LinearLayout) view.findViewById(R.id.indicator);
        bannerTitle = (TextView) view.findViewById(R.id.bannerTitle);
        numIndicator = (TextView) view.findViewById(R.id.numIndicator);
        handleTypedArray(context, attrs);
    }
    public void setDelayTime(int delayTime) {
        this.delayTime=delayTime;
    }
    public void setIndicatorGravity(int type) {
        switch (type){
            case LEFT:
                this.gravity=Gravity.LEFT|Gravity.CENTER_VERTICAL;
                break;
            case CENTER:
                this.gravity=Gravity.CENTER;
                break;
            case RIGHT:
                this.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL;
                break;
        }
    }
    public void setBannerTitle(String[] titles) {
        this.titles=titles;
        if (bannerStyle==CIRCLE_INDICATOR_TITLE||bannerStyle==NUM_INDICATOR_TITLE) {
            if (titles != null && titles.length > 0) {
                bannerTitle.setVisibility(View.VISIBLE);
                indicator.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
            }else{
                numIndicator.setBackgroundResource(R.drawable.black_background);
            }
        }
    }
    public void setBannerStyle(int bannerStyle) {
        this.bannerStyle=bannerStyle;
        switch (bannerStyle){
            case CIRCLE_INDICATOR:
                indicator.setVisibility(View.VISIBLE);
                break;
            case NUM_INDICATOR:
                numIndicator.setVisibility(View.VISIBLE);
                numIndicator.setBackgroundResource(R.drawable.black_background);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0,0,10,10);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                numIndicator.setLayoutParams(layoutParams);
                numIndicator.setPadding(5,6,5,6);
                break;
            case NUM_INDICATOR_TITLE:
                numIndicator.setVisibility(View.VISIBLE);
                break;
            case CIRCLE_INDICATOR_TITLE:
                indicator.setVisibility(View.VISIBLE);
                break;
        }
    }
    public void setImages(Object[] imagesUrl) {
        if (setImageArray(imagesUrl, null)) return;
        setData();
    }
    public void setImages(Object[] imagesUrl,OnLoadImageListener imageListener) {
        if (setImageArray(imagesUrl, imageListener)) return;
        setData();
    }
    public void setImages(List<?> imagesUrl){
        if (setImageList(imagesUrl, null)) return;
        setData();
    }
    public void setImages(List<?> imagesUrl,OnLoadImageListener imageListener) {
        if (setImageList(imagesUrl, imageListener)) return;
        setData();
    }
    private boolean setImageArray(Object[] imagesUrl, OnLoadImageListener imageListener) {
        if (imagesUrl==null||imagesUrl.length<=0) {
            Log.e(tag,"Please set the images data.");
            return true;
        }
        count = imagesUrl.length;
        createIndicator();
        imageViews.clear();
        for (int i = 0; i <= count + 1; i++) {
            ImageView iv = new ImageView(context);
            iv.setScaleType(ScaleType.CENTER_CROP);
            Object url=null;
            if (i == 0) {
                url=imagesUrl[count - 1];
            } else if (i == count + 1) {
                url=imagesUrl[0];
            } else {
                url=imagesUrl[i - 1];
            }
            imageViews.add(iv);
            if(imageListener!=null){
                imageListener.OnLoadImage(iv,url);
            }else{
                Glide.with(context).load(url).into(iv);
            }
        }
        return false;
    }
    private boolean setImageList(List<?> imagesUrl, OnLoadImageListener imageListener) {
        if (imagesUrl==null||imagesUrl.size()<=0) {
            Log.e(tag,"Please set the images data.");
            return true;
        }
        count = imagesUrl.size();
        createIndicator();
        imageViews.clear();
        for (int i = 0; i <= count + 1; i++) {
            ImageView iv = new ImageView(context);
            iv.setScaleType(ScaleType.CENTER_CROP);
            Object url=null;
            if (i == 0) {
                url=imagesUrl.get(count - 1);
            } else if (i == count + 1) {
                url=imagesUrl.get(0);
            } else {
                url=imagesUrl.get(i - 1);
            }
            imageViews.add(iv);
            if(imageListener!=null){
                imageListener.OnLoadImage(iv,url);
            }else{
                Glide.with(context).load(url).into(iv);
            }
        }
        return false;
    }
    private void createIndicator() {
        indicatorImages.clear();
        indicator.removeAllViews();
        for (int i = 0; i < count; i++) {
            ImageView imageView = new ImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mIndicatorWidth,mIndicatorHeight);
            params.leftMargin = mIndicatorMargin;
            params.rightMargin = mIndicatorMargin;
            if(i==0){
                imageView.setImageResource(mIndicatorSelectedResId);
            }else{
                imageView.setImageResource(mIndicatorUnselectedResId);
            }
            indicator.addView(imageView, params);
            indicatorImages.add(imageView);
        }
    }


    private void setData() {
        viewPager.setAdapter(new BannerPagerAdapter());
        viewPager.setFocusable(true);
        viewPager.setCurrentItem(1);
        currentItem = 1;
        viewPager.addOnPageChangeListener(this);
        if (gravity!=-1)
            indicator.setGravity(gravity);
        if (isAutoPlay)
            startAutoPlay();
    }
    public void isAutoPlay(boolean isAutoPlay) {
        this.isAutoPlay=isAutoPlay;
    }
    private void startAutoPlay() {
        isAutoPlay = true;
        handler.removeCallbacks(task);
        handler.postDelayed(task, delayTime);
    }

    private final Runnable task = new Runnable() {

        @Override
        public void run() {
            if (isAutoPlay) {
                currentItem = currentItem % (count + 1) + 1;
                if (currentItem == 1) {
                    viewPager.setCurrentItem(currentItem, false);
                    handler.post(task);
                } else {
                    viewPager.setCurrentItem(currentItem);
                    handler.postDelayed(task, delayTime);
                }
            } else {
                handler.postDelayed(task, delayTime);
            }
        }
    };

    class BannerPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return imageViews.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            container.addView(imageViews.get(position));
            final ImageView view=imageViews.get(position);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener!=null){
                        listener.OnBannerClick(v,position);
                    }
                }
            });
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(imageViews.get(position));
        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        switch (state) {
            case 1:
                isAutoPlay = false;
                break;
            case 2:
                isAutoPlay = true;
                break;
            case 0:
                if (viewPager.getCurrentItem() == 0) {
                    viewPager.setCurrentItem(count, false);
                } else if (viewPager.getCurrentItem() == count + 1) {
                    viewPager.setCurrentItem(1, false);
                }
                currentItem = viewPager.getCurrentItem();
                isAutoPlay = true;
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        indicatorImages.get((lastPosition - 1+count)%count).setImageResource(mIndicatorUnselectedResId);
        indicatorImages.get((position - 1+count)%count).setImageResource(mIndicatorSelectedResId);
        lastPosition=position;
        if (position==0) position=1;
        switch (bannerStyle){
            case CIRCLE_INDICATOR:
                break;
            case NUM_INDICATOR:
                if (position>count) position=count;
                numIndicator.setText(position+"/"+count);
                break;
            case NUM_INDICATOR_TITLE:
                if (position>count) position=count;
                numIndicator.setText(position+"/"+count);
                if (titles!=null&&titles.length>0){
                    if (position>titles.length) position=titles.length;
                    bannerTitle.setText(titles[position-1]);
                }
                break;
            case CIRCLE_INDICATOR_TITLE:
                if (titles!=null&&titles.length>0){
                    if (position>titles.length) position=titles.length;
                    bannerTitle.setText(titles[position-1]);
                }
                break;
        }

    }


    public void setOnBannerClickListener(OnBannerClickListener listener) {
        this.listener = listener;
    }

    public void setOnBannerImageListener(OnLoadImageListener imageListener) {
        this.imageListener = imageListener;
    }

    public interface OnBannerClickListener {
        void OnBannerClick(View view, int position);
    }
    public interface OnLoadImageListener {
        void OnLoadImage(ImageView view,Object url);
    }
}
