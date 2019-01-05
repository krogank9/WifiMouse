package vivid.designs.wifimouse;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class PercentSizedRelativeLayout extends RelativeLayout {
    float pctSize = 1.0f;
    public PercentSizedRelativeLayout(Context context)
    {
        super(context);
    }

    public PercentSizedRelativeLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        pctSize = attrs.getAttributeFloatValue(null, "percentSize", 1.0f);
    }

    public PercentSizedRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        pctSize = attrs.getAttributeFloatValue(null, "percentSize", 1.0f);
    }

    @Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
    {
        float width = View.MeasureSpec.getSize(widthMeasureSpec);
        float height = View.MeasureSpec.getSize(heightMeasureSpec);

        width *= pctSize;
        height *= pctSize;

        width = height = Math.min(width, height);

        super.onMeasure(
                View.MeasureSpec.makeMeasureSpec((int)width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((int)height, View.MeasureSpec.EXACTLY));
    }
}
