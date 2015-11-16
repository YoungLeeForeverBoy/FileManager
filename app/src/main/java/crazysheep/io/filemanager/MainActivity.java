package crazysheep.io.filemanager;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import crazysheep.io.filemanager.adapter.FilesAdapter;
import crazysheep.io.filemanager.asynctask.FileScannerTask;
import crazysheep.io.filemanager.model.FileItemModel;
import crazysheep.io.filemanager.prefs.SettingsPrefs;
import crazysheep.io.filemanager.utils.FileUtils;
import crazysheep.io.filemanager.utils.PermissionsUtils;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int REQUEST_CODE_READ_EXTERNAL_PERMISSION = 1;

    @Bind(R.id.file_rv) RecyclerView mFileRv;
    private LinearLayoutManager mLayoutMgr;
    private FilesAdapter mFileAdapter;
    private SettingsPrefs mSettingsPrefs;

    private File mCurrentDir;
    private LinkedList<ScanDirBean> mFileStack = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mSettingsPrefs = new SettingsPrefs(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initUI();
    }

    private void initUI() {
        mLayoutMgr = new LinearLayoutManager(this);
        mFileRv.setLayoutManager(mLayoutMgr);
        mFileAdapter = new FilesAdapter(this, null, mSettingsPrefs.getShowHiddenFiles()
                ? FilesAdapter.MODE_SHOW_HIDDEN_FILES : FilesAdapter.MODE_NOT_SHOW_HIDDEN_FILES);
        mFileRv.setAdapter(mFileAdapter);
        mFileAdapter.setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position, View view) {
                // click item file
                FileItemModel itemModel = mFileAdapter.getItem(position);
                File file = new File(itemModel.filepath);
                if(file.isDirectory()) {
                    // save last directory first visible position and offset
                    int firstVisibleItemPosition = mLayoutMgr.findFirstVisibleItemPosition();
                    ScanDirBean dirBean = new ScanDirBean(mCurrentDir, firstVisibleItemPosition,
                            mLayoutMgr.findViewByPosition(firstVisibleItemPosition).getTop());
                    mFileStack.push(dirBean);

                    doScanDir(new ScanDirBean(file, 0, 0));
                } else {
                    Intent openIntent = new Intent(Intent.ACTION_VIEW);
                    openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openIntent.setDataAndType(Uri.fromFile(file), FileUtils.getMimeType(file));
                    try {
                        startActivity(openIntent);
                    } catch (ActivityNotFoundException anfe) {
                        anfe.printStackTrace();

                        Snackbar.make(mFileRv, R.string.msg_can_not_open_file,
                                Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });
        mFileAdapter.setOnItemLongClickListener(new FilesAdapter.OnItemLongClickListener() {
            @Override
            public boolean onLongClick(final int position, View view) {
                // TODO item long click

                return true;
            }
        });

        if(PermissionsUtils.checkPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            doScanDir(new ScanDirBean(Environment.getExternalStorageDirectory(), 0, 0));
        } else {
            PermissionsUtils.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE,
                    REQUEST_CODE_READ_EXTERNAL_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_READ_EXTERNAL_PERMISSION: {
                for(int i = 0; i < permissions.length; i++) {
                    if(permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)
                            && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        doScanDir(new ScanDirBean(Environment.getExternalStorageDirectory(), 0, 0));
                    }
                }
            }break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(mFileStack.size() > 0) {
            doScanDir(mFileStack.pop());
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        menu.findItem(R.id.action_show_hidden_files).setTitle(mSettingsPrefs.getShowHiddenFiles()
                        ? R.string.action_not_show_hidden_files : R.string.action_show_hidden_files);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_show_hidden_files: {
                mSettingsPrefs.setShowHiddenFiles(!mSettingsPrefs.getShowHiddenFiles());
                mFileAdapter.setMode(mSettingsPrefs.getShowHiddenFiles()
                        ? FilesAdapter.MODE_SHOW_HIDDEN_FILES
                                : FilesAdapter.MODE_NOT_SHOW_HIDDEN_FILES);

                // update menu item title
                item.setTitle(mSettingsPrefs.getShowHiddenFiles()
                        ? R.string.action_not_show_hidden_files : R.string.action_show_hidden_files);
            }break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camara) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void doScanDir(final ScanDirBean dirBean) {
        if(dirBean.dir.isDirectory() && dirBean.dir.exists()) {
            mCurrentDir = dirBean.dir;

            new FileScannerTask(new FileScannerTask.OnScannerListener() {

                @Override
                public void onScanDone(List<FileItemModel> files) {
                    mFileAdapter.setData(files);

                    mLayoutMgr.scrollToPositionWithOffset(dirBean.lastTopPosition,
                            dirBean.lastTopPositionOffset);
                }
            }).execute(mCurrentDir);
        }
    }

    private static class ScanDirBean {

        public File dir;
        public int lastTopPosition = 0;
        public int lastTopPositionOffset = 0;

        public ScanDirBean(@NonNull File dir, int topPosition, int topPositionOffset) {
            this.dir = dir;
            this.lastTopPosition = topPosition;
            this.lastTopPositionOffset = topPositionOffset;
        }
    }

}
