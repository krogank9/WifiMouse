package vivid.designs.wifimouse;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class FixedAspectRatioRelativeLayout extends RelativeLayout
{
    float aspectRatio = 1.0f;
    String keepFixed;
    public FixedAspectRatioRelativeLayout(Context context)
    {
        super(context);
    }

    public FixedAspectRatioRelativeLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        aspectRatio = attrs.getAttributeFloatValue(null, "aspectRatio", 1.0f);
        keepFixed = attrs.getAttributeValue(null, "keepFixed");
        if(keepFixed == null)
            keepFixed = "";
    }

    public FixedAspectRatioRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        aspectRatio = attrs.getAttributeFloatValue(null, "aspectRatio", 1.0f);
        keepFixed = attrs.getAttributeValue(null, "keepFixed");
        if(keepFixed==null)
            keepFixed = "";
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
    {
        float width = View.MeasureSpec.getSize(widthMeasureSpec);
        float height = View.MeasureSpec.getSize(heightMeasureSpec);

        if(width == 0 || height == 0)
            width = height = Math.max(width, height);
        if(width == 0 && height == 0) {
            super.onMeasure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY));
            return;
        }

        float curAspectRatio = width/height;

        if( keepFixed.equals("height") || (!keepFixed.equals("width") && curAspectRatio > aspectRatio) ) {
            // zoom down width
            width = height*aspectRatio;
        } else if (keepFixed.equals("width") || curAspectRatio < aspectRatio){
            // zoom down height
            height = width/aspectRatio;
        }

        super.onMeasure(
                View.MeasureSpec.makeMeasureSpec((int)width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((int)height, View.MeasureSpec.EXACTLY));
    }
}