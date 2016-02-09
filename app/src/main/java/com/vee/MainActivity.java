package com.vee;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.vee.adapter.ApplicationAdapter;
import com.vee.temp.AppInfo;
import com.vee.temp.CustomItemAnimator;
import com.vee.temp.DetailActivity;
import com.vee.temp.UploadHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private FrameLayout fl;

    ViewGroup layout;
    private long lastPressTime = 0;
    private long thisTime = 0;
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int DRAWER_ITEM_SWITCH = 1;
    private static final int DRAWER_ITEM_OPEN_SOURCE = 10;

    private List<AppInfo> applicationList = new ArrayList<AppInfo>();

    private Drawer drawer;

    private ApplicationAdapter mAdapter;
    private FloatingActionButton mFabButton;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ProgressBar mProgressBar;
    Toolbar toolbar;
    private static UploadHelper.UploadComponentInfoTask uploadComponentInfoTask = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView(savedInstanceState);


//        layout = (ViewGroup) this.findViewById(R.id.flyout);
        toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Handle ProgressBar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        // Fab Button
        mFabButton = (FloatingActionButton) findViewById(R.id.fab_normal);
        mFabButton.setVisibility(View.GONE);
//        mFabButton.setImageDrawable(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_file_upload).color(Color.WHITE).actionBar());
//        mFabButton.setOnClickListener(fabClickListener);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new CustomItemAnimator());
        //mRecyclerView.setItemAnimator(new ReboundItemAnimator());

        mAdapter = new ApplicationAdapter(new ArrayList<AppInfo>(), R.layout.row_application, MainActivity.this);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.theme_accent));
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new InitializeApplicationsTask().execute();
            }
        });
        //show progress
        mRecyclerView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        initSP( savedInstanceState);

        new InitializeApplicationsTask().execute();

        if (savedInstanceState != null) {
            if (uploadComponentInfoTask != null) {
                if (uploadComponentInfoTask.isRunning) {
                    uploadComponentInfoTask.showProgress(this);
                }
            }
        }

        final SharedPreferences pref = getSharedPreferences("com.mikepenz.applicationreader", 0);

        drawer = new DrawerBuilder(this)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new SwitchDrawerItem().withOnCheckedChangeListener(new OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton compoundButton, boolean b) {
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putBoolean("autouploadenabled", b);
                                editor.apply();
                            }
                        }).withName(R.string.drawer_switch).withChecked(pref.getBoolean("autouploadenabled", false))
                ).addStickyDrawerItems(
                        new SecondaryDrawerItem()
                                .withName(R.string.drawer_opensource)
                                .withIdentifier(DRAWER_ITEM_OPEN_SOURCE)
                                .withIcon(FontAwesome.Icon.faw_github)
                                .withSelectable(false)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int i, IDrawerItem drawerItem) {
                        if (drawerItem.getIdentifier() == DRAWER_ITEM_OPEN_SOURCE) {
                            new LibsBuilder()
                                    .withFields(R.string.class.getFields())
                                    .withVersionShown(true)
                                    .withLicenseShown(true)
                                    .withActivityTitle(getString(R.string.drawer_opensource))
                                    .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                                    .start(MainActivity.this);
                        }
                        return false;
                    }
                })
                .withSelectedItem(-1)
                .withSavedInstance(savedInstanceState)
                .build();


    }


    public void initView(Bundle savedInstanceState) {

    }


    @Override
    public void onBackPressed() {
        Log.i(TAG, " - - -onBackPressed - - ");
        thisTime= System.currentTimeMillis();
        if(thisTime - lastPressTime > CONSTANT.exitConfirmTime){
            lastPressTime = System.currentTimeMillis();
            Snackbar sb = Snackbar.make(toolbar, "再按一次退出",Snackbar.LENGTH_SHORT);
//          Snackbar sb = Snackbar.make(layout, "再按一次退出",Snackbar.LENGTH_SHORT).show();
//          Snackbar sb = Snackbar.make(layout, "再按一次退出", Snackbar.LENGTH_LONG).setAction("", null);

            sb.getView().setBackgroundColor(0xfff44336);
            sb.show();
        }else{
            finish();
        }
    }




    /****************************************************************/

    /**
     * sample onClickListener with an AsyncTask as action
     */
    View.OnClickListener fabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            uploadComponentInfoTask = UploadHelper.getInstance(MainActivity.this, applicationList).uploadAll();
        }
    };
    /**
     * A simple AsyncTask to load the list of applications and display them
     */
    private class InitializeApplicationsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            mAdapter.clearApplications();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            applicationList.clear();

            //Query the applications
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> ril = getPackageManager().queryIntentActivities(mainIntent, 0);
            for (ResolveInfo ri : ril) {
                applicationList.add(new AppInfo(MainActivity.this, ri));
            }
            Collections.sort(applicationList);

            for (AppInfo appInfo : applicationList) {
                //load icons before shown. so the list is smoother
                appInfo.getIcon();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //handle visibility
            mRecyclerView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            //set data for list
            mAdapter.addApplications(applicationList);
            mSwipeRefreshLayout.setRefreshing(false);

            super.onPostExecute(result);
        }
    }
    public void animateActivity(AppInfo appInfo, View appIcon) {
        Intent intnt = new Intent(this, DetailActivity.class);
        intnt.putExtra("appInfo", appInfo.getComponentName());
        startActivity(intnt);
//        ActivityOptionsCompat transitionActivityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(this, Pair.create((View) mFabButton, "fab"), Pair.create(appIcon, "appIcon"));
//                ActivityOptionsCompat transitionActivityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(this, Pair.create(null, "fab"), Pair.create(appIcon, "appIcon"));
//
//        startActivity(i, transitionActivityOptions.toBundle());
    }
    public void initSP(Bundle savedInstanceState) {

    }
}


//            这个地方第一个参数    传进去的是tv    但是实际上你无论传进去什么值 snackbar都一定是从屏幕的最底端出现的    原因在源码
//            分析那边可以看到
//            Snackbar.make(tv, "再按一次退出", Snackbar.LENGTH_LONG).setAction("", new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    tv.setText("aleady click snackbar");
//                }
//            }).show();

