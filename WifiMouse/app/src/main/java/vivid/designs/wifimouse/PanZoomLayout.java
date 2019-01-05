package vivid.designs.wifimouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class PanZoomLayout extends RelativeLayout {
    public PanZoomLayout(Context c, AttributeSet a) { super(c, a); }

    private View child;

    float zoom = 1.0f;
    float offsetX = 0.0f;
    float offsetY = 0.0f;
    final float MIN_ZOOM = 1.0f;
    final float MAX_ZOOM = 10.0f;

    // convert point on this view to point on resized and moved child
    public Point getPointRelativeToChild(float x, float y, float resizeToX, float resizeToY, boolean crop) {
        if(child.getWidth() == 0 || child.getHeight() == 0 || zoom == 0)
            return new Point(0,0);

        float childRealX = child.getX()*zoom - offsetX;
        float childRealY = child.getY()*zoom - offsetY;

        float childRealWidth = child.getWidth()*zoom;
        float childRealHeight = child.getHeight()*zoom;

        x -= childRealX;
        y -= childRealY;

        if(crop) {
            x = Math.max(0, x);
            y = Math.max(0, y);
            x = Math.min(childRealWidth, x);
            y = Math.min(childRealHeight, y);
        }

        float resizeXRatio = x / childRealWidth;
        float resizeYRatio = y / childRealHeight;
        x = resizeXRatio * resizeToX;
        y = resizeYRatio * resizeToY;

        return new Point((int)x,(int)y);
    }

    public Rect getChildVisibleRect(int resizeToWidth, int resizeToHeight, float padPct) {
        Point topLeft = getPointRelativeToChild(this.getWidth() * -padPct, this.getHeight() * -padPct, resizeToWidth, resizeToHeight, true);
        Point bottomRight = getPointRelativeToChild(this.getWidth() * (1+padPct), this.getHeight() * (1+padPct), resizeToWidth, resizeToHeight, true);

        return new Rect(topLeft.x ,topLeft.y, bottomRight.x, bottomRight.y);
    }

    private boolean lastMotionEventWasPan = false;
    private boolean lastMotionEventWasZoom = false;
    public boolean isPanning() {
        return lastMotionEventWasPan;
    }
    public boolean isZooming() {
        return lastMotionEventWasZoom;
    }

    @Override
    protected void onFinishInflate ()
    {
        super.onFinishInflate();
        child = getChildAt(0);
        if(child == null || getChildCount() > 1)
            throw new IllegalStateException("PanZoomLayout must have 1 child");
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(-offsetX, -offsetY);
        canvas.scale(zoom, zoom);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    public void panToBounds() {
        float left = child.getLeft() * zoom;
        float right = child.getRight() * zoom - this.getWidth();
        float bottom = child.getBottom() * zoom - this.getHeight();
        float top = child.getTop() * zoom;

        // center child when it's too small to pan around
        if(child.getHeight()*zoom < this.getHeight()) {
            offsetY = top - (this.getHeight() - child.getHeight()*zoom)/2;
        }
        else {
            // limit offset so it can't move the child off screen
            offsetY = Math.max(top, offsetY);
            offsetY = Math.min(bottom, offsetY);
        }

        if(child.getWidth()*zoom < this.getWidth()) {
            offsetX = left - (this.getWidth() - child.getWidth()*zoom)/2;
        }
        else {
            offsetX = Math.max(left, offsetX);
            offsetX = Math.min(right, offsetX);
        }
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    private void doZoom(float zoomChange) {
        if(zoomChange == 0)
            return;

        if(zoom != 0) {
            float zoomRatio = (zoom * zoomChange) / zoom;
            // After zooming, change offset pos based on how the focal point moves
            float newFocalX = zoomFocalX*zoomRatio;
            float newFocalY = zoomFocalY*zoomRatio;
            offsetX += newFocalX - zoomFocalX;
            offsetY += newFocalY - zoomFocalY;
        }

        zoom *= zoomChange;
    }

    OnTouchListener touchListener = null;
    @Override
    public void setOnTouchListener(OnTouchListener touchListener) {
        this.touchListener = touchListener;
    }

    float lastX, lastY, lastDist;
    float zoomFocalX, zoomFocalY;
    boolean pointerReleased = false;
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        final int action = (e.getAction() & MotionEvent.ACTION_MASK);

        boolean wasPan = false;
        boolean wasZoom = false;
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                lastDist = spacing(e);
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_DOWN:
                pointerReleased = true;
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_MOVE:
                if(pointerReleased) {
                    pointerReleased = false;
                    lastX = e.getX();
                    lastY = e.getY();
                }

                if(e.getPointerCount() == 1) {
                    wasPan = true;
                    offsetX -= e.getX() - lastX;
                    offsetY -= e.getY() - lastY;
                    panToBounds();
                    lastX = e.getX();
                    lastY = e.getY();
                }
                else {
                    wasZoom = true;
                    float dist = spacing(e);

                    float change = dist/lastDist;

                    if(zoom*change > MAX_ZOOM)
                        change = MAX_ZOOM/zoom;
                    else if(zoom*change < MIN_ZOOM)
                        change = MIN_ZOOM/zoom;

                    zoomFocalX = offsetX + e.getX();
                    zoomFocalY = offsetY + e.getY();

                    doZoom(change);
                    panToBounds();

                    lastDist = dist;
                }
                invalidate();
                break;
        }
        lastMotionEventWasPan = wasPan;
        lastMotionEventWasZoom = wasZoom;

        if(touchListener != null)
            return touchListener.onTouch(this, e);
        else
            return true;
    }
}
