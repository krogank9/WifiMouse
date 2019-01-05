package vivid.designs.wifimouse;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

public class MyImageView extends android.support.v7.widget.AppCompatImageView {
    public MyImageView(Context c, AttributeSet a) { super(c, a); }

    private int lastImgWidth, lastImgHeight;

    // AdjustViewBounds always on causes lag & image tears
    @Override
    public void setImageBitmap(Bitmap bm) {
        if(bm != null && (bm.getWidth() != lastImgWidth || bm.getHeight() != lastImgHeight)) {
            this.setAdjustViewBounds(true);
        }
        else
            this.setAdjustViewBounds(false);

        super.setImageBitmap(bm);
    }
}
