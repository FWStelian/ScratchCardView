package com.goibibo.libs.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.goibibo.libs.utils.BitmapUtils;
import com.goibiboutils.views.R;


public class ScratchRelativeLayoutView extends RelativeLayout {
  public static final float STROKE_WIDTH = 12.0F;
  private float mX;
  private float mY;
  private static final float TOUCH_TOLERANCE = 4.0F;
  private Bitmap mScratchBitmap;
  private Canvas mCanvas;
  private Path mErasePath;
  private Path mTouchPath;
  private Paint mBitmapPaint;
  private Paint mErasePaint;
  private Paint mGradientBgPaint;
  private BitmapDrawable mDrawable;
  private ScratchRelativeLayoutView.IRevealListener mRevealListener;
  private float mRevealPercent;
  private int mThreadCount = 0;

  public ScratchRelativeLayoutView(Context context) {
    super(context);
    this.init();
  }

  public ScratchRelativeLayoutView(Context context, AttributeSet set) {
    super(context, set);
    this.init();
  }

  public ScratchRelativeLayoutView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    this.init();
  }

  public void setStrokeWidth(int multiplier) {
    this.mErasePaint.setStrokeWidth((float) multiplier * 12.0F);
  }

  private void init() {
    this.mTouchPath = new Path();
    this.mErasePaint = new Paint();
    this.mErasePaint.setAntiAlias(true);
    this.mErasePaint.setDither(true);
    this.mErasePaint.setColor(-65536);
    this.mErasePaint.setStyle(Paint.Style.STROKE);
    this.mErasePaint.setStrokeJoin(Paint.Join.BEVEL);
    this.mErasePaint.setStrokeCap(Paint.Cap.ROUND);
    this.setStrokeWidth(6);
    this.mGradientBgPaint = new Paint();
    this.mErasePath = new Path();
    this.mBitmapPaint = new Paint(4);
    Bitmap scratchBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_scratch_pattern);
    this.mDrawable = new BitmapDrawable(this.getResources(), scratchBitmap);
    this.mDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    this.setEraserMode();
  }

  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    this.mScratchBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    this.mCanvas = new Canvas(this.mScratchBitmap);
    Rect rect = new Rect(0, 0, this.mScratchBitmap.getWidth(), this.mScratchBitmap.getHeight());
    this.mDrawable.setBounds(rect);
    int startGradientColor = ContextCompat.getColor(this.getContext(), R.color.scratch_start_gradient);
    int endGradientColor = ContextCompat.getColor(this.getContext(), R.color.scratch_end_gradient);
    this.mGradientBgPaint.setShader(new LinearGradient(0.0F, 0.0F, 0.0F, (float) this.getHeight(), startGradientColor, endGradientColor, Shader.TileMode.MIRROR));
    this.mCanvas.drawRect(rect, this.mGradientBgPaint);
    this.mDrawable.draw(this.mCanvas);
  }

  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    canvas.drawBitmap(this.mScratchBitmap, 0.0F, 0.0F, this.mBitmapPaint);
    canvas.drawPath(this.mErasePath, this.mErasePaint);
  }

  private void touch_start(float x, float y) {
    this.mErasePath.reset();
    this.mErasePath.moveTo(x, y);
    this.mX = x;
    this.mY = y;
  }

  public void clear() {
    int[] bounds = this.getLayoutBounds();
    int left = bounds[0];
    int top = bounds[1];
    int right = bounds[2];
    int bottom = bounds[3];
    int width = right - left;
    int height = bottom - top;
    int centerX = left + width / 2;
    int centerY = top + height / 2;
    left = centerX - width / 2;
    top = centerY - height / 2;
    right = left + width;
    bottom = top + height;
    Paint paint = new Paint();
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    this.mCanvas.drawRect((float) left, (float) top, (float) right, (float) bottom, paint);
    this.checkRevealed();
    this.invalidate();
  }

  private void touch_move(float x, float y) {
    float dx = Math.abs(x - this.mX);
    float dy = Math.abs(y - this.mY);
    if (dx >= 4.0F || dy >= 4.0F) {
      this.mErasePath.quadTo(this.mX, this.mY, (x + this.mX) / 2.0F, (y + this.mY) / 2.0F);
      this.mX = x;
      this.mY = y;
      this.drawPath();
    }

    this.mTouchPath.reset();
    this.mTouchPath.addCircle(this.mX, this.mY, 30.0F, Path.Direction.CW);
  }

  private void drawPath() {
    this.mErasePath.lineTo(this.mX, this.mY);
    this.mCanvas.drawPath(this.mErasePath, this.mErasePaint);
    this.mTouchPath.reset();
    this.mErasePath.reset();
    this.mErasePath.moveTo(this.mX, this.mY);
    this.checkRevealed();
  }

  public void reveal() {
    this.clear();
  }

  private void touch_up() {
    this.drawPath();
  }

  public boolean onTouchEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();
    switch (event.getAction()) {
      case 0:
        this.touch_start(x, y);
        this.invalidate();
        break;
      case 1:
        this.touch_up();
        this.invalidate();
        break;
      case 2:
        this.touch_move(x, y);
        this.invalidate();
    }

    return true;
  }

  public int getColor() {
    return this.mErasePaint.getColor();
  }

  public Paint getErasePaint() {
    return this.mErasePaint;
  }

  public void setEraserMode() {
    this.getErasePaint().setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
  }

  public void setRevealListener(ScratchRelativeLayoutView.IRevealListener listener) {
    this.mRevealListener = listener;
  }

  public boolean isRevealed() {
    return this.mRevealPercent == 1.0F;
  }

  private void checkRevealed() {
    if (!this.isRevealed() && this.mRevealListener != null) {
      int[] bounds = this.getLayoutBounds();
      int left = bounds[0];
      int top = bounds[1];
      int width = bounds[2] - left;
      int height = bounds[3] - top;
      if (this.mThreadCount > 1) {
        Log.d("Captcha", "Count greater than 1");
        return;
      }

      ++this.mThreadCount;
      (new AsyncTask<Integer, Void, Float>() {
        protected Float doInBackground(Integer... params) {
          Float var7;
          try {
            int left = params[0];
            int top = params[1];
            int width = params[2];
            int height = params[3];
            Bitmap croppedBitmap = Bitmap.createBitmap(ScratchRelativeLayoutView.this.mScratchBitmap, left, top, width, height);
            var7 = BitmapUtils.getTransparentPixelPercent(croppedBitmap);
          } finally {
            ScratchRelativeLayoutView.this.mThreadCount--;
          }

          return var7;
        }

        public void onPostExecute(Float percentRevealed) {
          if (!ScratchRelativeLayoutView.this.isRevealed()) {
            float oldValue = ScratchRelativeLayoutView.this.mRevealPercent;
            ScratchRelativeLayoutView.this.mRevealPercent = percentRevealed;
            if (oldValue != percentRevealed) {
              ScratchRelativeLayoutView.this.mRevealListener.onRevealPercentChangedListener(ScratchRelativeLayoutView.this, percentRevealed);
            }

            if (ScratchRelativeLayoutView.this.isRevealed()) {
              ScratchRelativeLayoutView.this.mRevealListener.onRevealed(ScratchRelativeLayoutView.this);
            }
          }

        }
      }).execute(new Integer[]{left, top, width, height});
    }

  }

  public int[] getLayoutBounds() {
    int paddingLeft = this.getPaddingLeft();
    int paddingTop = this.getPaddingTop();
    int paddingRight = this.getPaddingRight();
    int paddingBottom = this.getPaddingBottom();
    int vwidth = this.getWidth() - paddingLeft - paddingRight;
    int vheight = this.getHeight() - paddingBottom - paddingTop;
    int centerX = vwidth / 2;
    int centerY = vheight / 2;
    Drawable drawable = this.getBackground();
    Rect bounds = drawable.getBounds();
    int width = drawable.getIntrinsicWidth();
    int height = drawable.getIntrinsicHeight();
    if (width <= 0) {
      width = bounds.right - bounds.left;
    }

    if (height <= 0) {
      height = bounds.bottom - bounds.top;
    }

    if (height > vheight) {
      height = vheight;
    }

    if (width > vwidth) {
      width = vwidth;
    }

    int left = paddingLeft;
    int top = paddingTop;
    width = vwidth;
    height = vheight;
    return new int[]{left, top, left + width, top + height};
  }

  public interface IRevealListener {
    void onRevealed(ScratchRelativeLayoutView var1);

    void onRevealPercentChangedListener(ScratchRelativeLayoutView var1, float var2);
  }
}