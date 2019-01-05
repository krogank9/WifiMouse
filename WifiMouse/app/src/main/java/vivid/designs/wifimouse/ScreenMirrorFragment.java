package vivid.designs.wifimouse;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ScreenMirrorFragment extends Fragment {
    boolean meVisible = false;
    View baseFrame;
    ImageView screenImageView;
    PanZoomLayout panZoomer;

    long screenFetchId = ScreenMirrorUtils.screenFetchCounter;
    private void updateUi() {
        if(!meVisible)
            return;

        WifiMouseApplication.networkConnection.sendMessage("ScreenMirror");

        ScreenMirrorUtils.curQualityPct = (panZoomer.zoom - panZoomer.MIN_ZOOM) / (panZoomer.MAX_ZOOM - panZoomer.MIN_ZOOM);

        if(screenFetchId != ScreenMirrorUtils.screenFetchCounter && ScreenMirrorUtils.screenMirrorBitmap != null) {
            Rect visibleRect = panZoomer.getChildVisibleRect(ScreenMirrorUtils.screenMirrorBitmap.getWidth(), ScreenMirrorUtils.screenMirrorBitmap.getHeight(), 1.0f);
            if(panZoomer.isZooming() || visibleRect.width() == 0 || visibleRect.height() == 0)
                ScreenMirrorUtils.cropRect = null;
            else
                ScreenMirrorUtils.cropRect = visibleRect;
            screenImageView.setImageBitmap(ScreenMirrorUtils.screenMirrorBitmap);
            screenFetchId = ScreenMirrorUtils.screenFetchCounter;
        }
    }

    public ScreenMirrorFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    boolean panZoomerActionIsClick;
    long panZoomerlastDown = 0;
    long panZoomerLastClick = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflated = inflater.inflate(R.layout.fragment_screen_mirror, container, false);
        screenImageView = (ImageView) inflated.findViewById(R.id.screen_imageview);
        baseFrame = inflated.findViewById(R.id.base_frame);
        panZoomer = (PanZoomLayout) inflated.findViewById(R.id.pan_zoomer);

        panZoomer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                final int action = e.getAction() & MotionEvent.ACTION_MASK;
                long now = System.currentTimeMillis();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        panZoomerlastDown = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_UP:
                        if(panZoomerActionIsClick && ScreenMirrorUtils.screenMirrorBitmap != null && now - panZoomerlastDown < 500) {
                            int width = ScreenMirrorUtils.screenMirrorBitmap.getWidth();
                            int height = ScreenMirrorUtils.screenMirrorBitmap.getHeight();
                            Point mousePos = panZoomer.getPointRelativeToChild(e.getX(), e.getY(), width, height, false);
                            if(mousePos.x >= 0 || mousePos.y >= 0 || mousePos.x < width || mousePos.y < height) {
                                if(now - panZoomerLastClick > 400) // don't move mouse when double clicking
                                    WifiMouseApplication.networkConnection.sendMessage("MouseSetPos "+mousePos.x+","+mousePos.y);
                                WifiMouseApplication.networkConnection.sendMessage("MouseDown 1");
                                WifiMouseApplication.networkConnection.sendMessage("MouseUp 1");
                                panZoomerLastClick = now;
                            }
                        }
                        break;
                }
                panZoomerActionIsClick = action == MotionEvent.ACTION_DOWN;

                return true;
            }
        });

        baseFrame.post(new Runnable() {
            @Override
            public void run() {
                updateUi();
                if(baseFrame != null)
                    baseFrame.postDelayed(this, WifiMouseApplication.networkConnection instanceof BluetoothConnection? 1000:100);
            }
        });
        return inflated;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("onResume", "im back");
        meVisible = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        meVisible = false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        baseFrame = null;
        screenImageView = null;
    }
}
