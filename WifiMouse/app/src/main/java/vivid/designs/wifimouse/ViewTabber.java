package vivid.designs.wifimouse;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.TabLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class ViewTabber extends LinearLayout {
    TabLayout tabTitles;
    FrameLayout tabsContainer;
    ArrayList<View> tabViews = new ArrayList<>();
    ArrayList<TabLayout.Tab> tabList = new ArrayList<>();
    int curTab = -1;

    public ViewTabber(Context c) { super(c); init(c); }
    private void init(Context c) {
        this.setOrientation(VERTICAL);

        tabTitles = new TabLayout(c);
        tabTitles.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tabTitles.setBackgroundColor(Color.WHITE);
        tabTitles.setSelectedTabIndicatorHeight(7);
        tabTitles.setSelectedTabIndicatorColor(Color.parseColor("#ff3333"));
        this.addView(tabTitles);

        tabsContainer = new FrameLayout(c);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        layoutParams.weight = 1;
        tabsContainer.setLayoutParams(layoutParams);
        this.addView(tabsContainer);

        View tabShadow = new View(c);
        int dp = 3;
        final float scale = c.getResources().getDisplayMetrics().density;
        int px = (int) (dp * scale + 0.5f);
        tabShadow.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px));
        tabShadow.setBackgroundResource(R.drawable.v_shadow);
        tabsContainer.addView(tabShadow);

        tabTitles.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                curTab = tabList.indexOf(tab);
                updateTabs();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateTabs() {
        if(curTab == -1)
            return;

        for(int i=0; i<tabViews.size(); i++) {
            View tabView = tabViews.get(i);
            tabView.setVisibility(curTab == i? VISIBLE:INVISIBLE);
            tabView.setClickable(curTab == i? true:false);
        }
    }

    public void addTab(View v, String name) {
        v.setClickable(false);
        v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tabsContainer.addView(v);
        tabViews.add(v);

        TabLayout.Tab newTab = tabTitles.newTab().setText(name);
        tabList.add(newTab);
        tabTitles.addTab(newTab);

        if(curTab == -1)
            curTab = 0;
        updateTabs();
    }
}
