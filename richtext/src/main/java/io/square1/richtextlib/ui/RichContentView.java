/*
 * Copyright (c) 2015. Roberto  Prato <https://github.com/robertoprato>
 *
 *  *
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package io.square1.richtextlib.ui;

import android.annotation.TargetApi;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.ArrayList;

import io.square1.richtextlib.R;
import io.square1.richtextlib.v2.RichTextV2;
import io.square1.richtextlib.v2.content.DocumentElement;
import io.square1.richtextlib.v2.content.RichDocument;
import io.square1.richtextlib.v2.content.RichTextDocumentElement;


import io.square1.richtextlib.spans.ClickableSpan;
import io.square1.richtextlib.spans.RichTextSpan;
import io.square1.richtextlib.spans.URLSpan;
import io.square1.richtextlib.spans.UnsupportedContentSpan;
import io.square1.richtextlib.spans.UrlBitmapDownloader;
import io.square1.richtextlib.spans.YouTubeSpan;


/**
 * Created by roberto on 20/09/15.
 */
public class RichContentView extends FrameLayout implements RichContentViewDisplay {


    private UrlBitmapDownloader mBitmapManager;

    private RichTextDocumentElement mText;
    private RichTextSpan[] mSpans;

    private boolean mAttachedToWindow;

    private Appearance mAppearance;

    private Layout mLayout;

    private int mLastMeasuredWidth;



    private OnSpanClickedObserver mOnSpanClickedObserver;



    public RichContentView(Context context) {
        super(context);
        init(context, null, -1, -1);

    }

    public RichContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, -1, -1);
    }

    public RichContentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, -1);
    }

    @Override
    public void addSubView(View view) {
        addView(view);
    }

    public void setText(RichTextDocumentElement builder){
        if(mText != builder) {
            mText = builder;
            mSpans = mText.getSpans();
            mLayout = null;
            for(RichTextSpan span : mSpans){
                span.onSpannedSetToView(this);
            }


            requestLayout();
            invalidate();
        }
    }



    @Override
    public void performLayout(){
        if (mLayout != null) {
            mLayout = null;
            requestLayout();
            invalidate();
        }
    }

    @Override
    public void setUrlBitmapDownloader(UrlBitmapDownloader downloader) {
        mBitmapManager = downloader;
    }

    @Override
    public void setRichTextContentChanged(RichTextContentChanged richTextContentChanged) {

    }



    public void setOnSpanClickedObserver( OnSpanClickedObserver observer){
        mOnSpanClickedObserver = observer;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RichContentView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);

    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        setWillNotDraw(false);

        mAppearance = new Appearance(context);

        mText = new RichTextDocumentElement();
        mSpans = mText.getSpans();

        mLastMeasuredWidth = 0;

        parseCustomAttributes(context, attrs);


        performLayout();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {


        int widthSize = MeasureSpec.getSize(widthMeasureSpec);


        if(mLayout == null || (mLastMeasuredWidth != widthSize)){
            mLastMeasuredWidth = widthSize;
            mLayout = makeLayout(widthSize - getPaddingLeft() - getPaddingRight());
        }

        if (mLayout != null) {

            setMeasuredDimension(widthSize,
                    getPaddingTop() + getPaddingBottom() + mLayout.getHeight());

           // setMeasuredDimension(widthSize, mLayout.getHeight());
        }else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }


    private Layout makeLayout(int width){

        StaticLayout result = new StaticLayout(mText,
                mAppearance.textPaint(null),
                width,
                Layout.Alignment.ALIGN_NORMAL,
                mAppearance.getSpacingMult(),
                mAppearance.getLineSpacingAdd(),
                false);

        return result;
    }


    @Override
    public void onDraw(Canvas canvas){
        canvas.save();
        if (mLayout != null) {
            canvas.translate(getPaddingLeft(), getPaddingTop());
            mLayout.draw(canvas);
        }
        canvas.restore();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();

        // If action has finished
        if(mLayout != null && (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN)) {

            // Locate the area that was pressed
            int x = (int) event.getX();
            int y = (int) event.getY();
            x -= getPaddingLeft();
            y -= getPaddingTop();
            x += getScrollX();
            y += getScrollY();

            // Locate the URL text
            int line = mLayout.getLineForVertical(y);
            int off = mLayout.getOffsetForHorizontal(line, x);

            Animatable[] animatables =  mText.getSpans(off, off, Animatable.class);
            if(animatables.length > 0) return false;

            if (animatables.length != 0 && action == MotionEvent.ACTION_UP) {
                if(animatables[0].isRunning()){
                    animatables[0].stop();
                }else{
                    animatables[0].start();
                }
            }
            // Find the URL that was pressed
            ClickableSpan[] link = mText.getSpans(off, off, ClickableSpan.class);
            // If we've found a URL
            if (link.length != 0 && action == MotionEvent.ACTION_UP) {
                onSpansClicked(link);
            }

            return true;
        }

        return super.onTouchEvent(event);
    }



    @Override
    public UrlBitmapDownloader getDownloader() {
        return mBitmapManager;
    }

    @Override
    public boolean viewAttachedToWindow() {
        return mAttachedToWindow;
    }


    @Override
    public void onAttachedToWindow(){
        super.onAttachedToWindow();

        mAttachedToWindow = true;

        for (RichTextSpan span : mSpans) {
                span.onAttachedToWindow(this);
        }

    }

    @Override
    public void onDetachedFromWindow(){
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
        for(RichTextSpan span : mSpans){
            span.onDetachedFromWindow(this);
        }
    }


    public LayoutParams generateDefaultLayoutParams(Point position,int width, int height ){
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width,height);
        params.leftMargin = position.x;
        params.topMargin = position.y;

        return params;
    }

    public void onSpansClicked(ClickableSpan[] spans) {

        if(spans == null) return;

        for(ClickableSpan span : spans){

            //if handled externally lets just continue
            if(mOnSpanClickedObserver != null &&
                   mOnSpanClickedObserver.onSpanClicked(span) == true)
                continue;

            if(span instanceof YouTubeSpan){

                String id = ((YouTubeSpan)span).getYoutubeId();

                this.getContext().
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://www.youtube.com/watch?v=" + id)));
            }
            else if(span instanceof URLSpan){

                String url = ((URLSpan)span).getURL();
                try {
                    this.getContext().
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }catch (Exception exc){
                    Toast.makeText(getContext(), R.string.error_opening_message, Toast.LENGTH_LONG).show();
                }

            }else if (span instanceof UnsupportedContentSpan){
                String url = ((UnsupportedContentSpan)span).getURL();
                FallbackWebDialog dialog = new FallbackWebDialog(getContext(),url);
                dialog.setCancelable(true);
                dialog.show();
            }
        }

    }


    private void parseCustomAttributes(Context context, AttributeSet attrs) {

        if(attrs == null){
            return;
        }

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.RichContentView,
                0, 0);
        try {

            if (a.hasValue(R.styleable.RichContentView_android_fontFamily)) {

                String customFont = a.getString(R.styleable.RichContentView_android_fontFamily);

                Typeface typeface = getTypeFace(customFont);
                if (typeface != null) {
                    mAppearance.setTextTypeFace(typeface);
                }
            }

            if (a.hasValue(R.styleable.RichContentView_android_textSize)) {
                int textSize = a.getDimensionPixelSize(R.styleable.RichContentView_android_textSize, 15);
                mAppearance.setTextFontSize(textSize);
            }

            if (a.hasValue(R.styleable.RichContentView_android_textColor)) {
                int color = a.getColor(R.styleable.RichContentView_android_textColor, Color.BLACK);
                mAppearance.setTextColor(color);
            }

            if (a.hasValue(R.styleable.RichContentView_android_textColorLink)) {
                int color = a.getColor(R.styleable.RichContentView_android_textColorLink, Color.BLUE);
                mAppearance.setLinkColor(color);
            }

            if (a.hasValue(R.styleable.RichContentView_richQuoteBackgroundColor)) {
                int color = a.getColor(R.styleable.RichContentView_richQuoteBackgroundColor, Color.TRANSPARENT);
                mAppearance.setQuoteBackgroundColor(color);
            }

            if (a.hasValue(R.styleable.RichContentView_richQuoteDrawable)) {
                Drawable quote = a.getDrawable(R.styleable.RichContentView_richQuoteDrawable);
                mAppearance.setQuoteSign(quote);
            }

            if (a.hasValue(R.styleable.RichContentView_richQuoteFontFamily)) {

                String customQuoteFont = a.getString(R.styleable.RichContentView_richQuoteFontFamily);
                Typeface quoteTypeFace = getTypeFace(customQuoteFont);
                if (quoteTypeFace != null) {
                    mAppearance.setTextQuoteTypeFace(quoteTypeFace);
                }
            }

            if (a.hasValue(R.styleable.RichContentView_richQuoteTextColor)) {
                int color = a.getColor(R.styleable.RichContentView_richQuoteTextColor, 0);
                mAppearance.setTextQuoteColor(color);
            }

            if (a.hasValue(R.styleable.RichContentView_richQuoteTextSize)) {
                int textSize = a.getDimensionPixelSize(R.styleable.RichContentView_richQuoteTextSize, 15);
                mAppearance.setTextQuoteFontSize(textSize);
            }

            if (a.hasValue(R.styleable.RichContentView_richHeaderTextColor)) {
                int color = a.getColor(R.styleable.RichContentView_richHeaderTextColor, 0);
                mAppearance.setTextHeaderColor(color);
            }

            if(a.hasValue(R.styleable.RichContentView_richLineSpacingMultiplier)){
                float spacingMult = a.getFloat(R.styleable.RichContentView_richLineSpacingMultiplier, 1.0f);
                mAppearance.setSpacingMult(spacingMult);
            }

            if(a.hasValue(R.styleable.RichContentView_richLineSpacingExtra)){
                float lineSpacingAdd = a.getDimensionPixelSize(R.styleable.RichContentView_richLineSpacingExtra, (int) 0);
                mAppearance.setLineSpacingAdd(lineSpacingAdd);
            }

            CharSequence sequence =  "";

            if(a.hasValue(R.styleable.RichContentView_android_text)){
                sequence = a.getText(R.styleable.RichContentView_android_text);
            }else if (isInEditMode()) {
                sequence = getResources().getString(R.string.sample_html_tags);
            }

            if(TextUtils.isEmpty(sequence) == false){
                RichDocument richDocument = RichTextV2.fromHtml(getContext(),sequence.toString());
                ArrayList<DocumentElement> elementArrayList = richDocument.getElements();
                for(DocumentElement documentElement : elementArrayList){
                    if(documentElement instanceof RichTextDocumentElement){
                        setText((RichTextDocumentElement) documentElement);
                        break;
                    }
                }
            }


        }finally {
            a.recycle();
        }
    }

    public boolean setFontFamily(String customFont) {

        if(TextUtils.isEmpty(customFont)){
            return false;
        }

        try {

            Typeface tf  = Typeface.createFromAsset(getContext().getAssets(), customFont);
            mAppearance.setTextTypeFace(tf);

        } catch (Exception e) {

            return false;

        }

        return true;
    }

    private Typeface getTypeFace(String customTypeFace){

        if(TextUtils.isEmpty(customTypeFace)){
            return null;
        }

        try {

            Typeface tf  = Typeface.createFromAsset(getContext().getAssets(), customTypeFace);
            return tf;

        } catch (Exception e) {
            return null;
        }

    }

    public Point getSpanOrigin(Object span) {

        Rect parentTextViewRect = new Rect();


        double startOffsetOfClickedText = mText.getSpanStart(span);
        double endOffsetOfClickedText = mText.getSpanEnd(span);
        double startXCoordinatesOfClickedText = mLayout.getPrimaryHorizontal((int) startOffsetOfClickedText);
        double endXCoordinatesOfClickedText = mLayout.getPrimaryHorizontal((int) endOffsetOfClickedText);


        // Get the rectangle of the clicked text
        int currentLineStartOffset = mLayout.getLineForOffset((int) startOffsetOfClickedText);
        int currentLineEndOffset = mLayout.getLineForOffset((int) endOffsetOfClickedText);
        boolean keywordIsInMultiLine = currentLineStartOffset != currentLineEndOffset;
        mLayout.getLineBounds(currentLineStartOffset, parentTextViewRect);


        // Update the rectangle position to his real position on screen
        int[] parentTextViewLocation = {0, 0};
        getLocationOnScreen(parentTextViewLocation);

        double parentTextViewTopAndBottomOffset = (
                parentTextViewLocation[1] -
                        getScrollY() +
                        getPaddingTop()
        );
        parentTextViewRect.top += parentTextViewTopAndBottomOffset;
        parentTextViewRect.bottom += parentTextViewTopAndBottomOffset;

        parentTextViewRect.left += (
                parentTextViewLocation[0] +
                        startXCoordinatesOfClickedText +
                        getPaddingLeft() -
                        getScrollX()
        );

        parentTextViewRect.right = (int) (
                parentTextViewRect.left +
                        endXCoordinatesOfClickedText -
                        startXCoordinatesOfClickedText
        );

        int x = (parentTextViewRect.left + parentTextViewRect.right) / 2;
        int y = parentTextViewRect.bottom;
        if (keywordIsInMultiLine) {
            x = parentTextViewRect.left;
        }

        return  new Point(x,y);
    }

    @Override
    public int getPaddingLeft() {
        return super.getPaddingLeft();
    }

    @Override
    public int getPaddingRight() {
        return super.getPaddingRight();

    }

    @Override
    public void invalidateDrawable(Drawable who) {
        super.invalidateDrawable(who);
        this.invalidate();
    }


    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        super.scheduleDrawable(who,what,when);

    }


    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        super.unscheduleDrawable(who, what);
    }

    @Override
    public Appearance getStyle(){
        return mAppearance;
    }
}
