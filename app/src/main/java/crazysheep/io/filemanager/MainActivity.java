package crazysheep.io.filemanager;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import crazysheep.io.filemanager.adapter.FilesAdapter;
import crazysheep.io.filemanager.adapter.RecyclerViewBaseAdapter;
import crazysheep.io.filemanager.animator.FabAnimatorHelper;
import crazysheep.io.filemanager.animator.FabMenuAnimatorHelper;
import crazysheep.io.filemanager.asynctask.ScanDirTask;
import crazysheep.io.filemanager.io.FileIO;
import crazysheep.io.filemanager.model.FileItemDto;
import crazysheep.io.filemanager.model.FileItemDtoHelper;
import crazysheep.io.filemanager.model.MultiFileInfoDto;
import crazysheep.io.filemanager.model.ScanDirDto;
import crazysheep.io.filemanager.model.SingleFileInfoDto;
import crazysheep.io.filemanager.prefs.SettingsPrefs;
import crazysheep.io.filemanager.utils.ActivityUtils;
import crazysheep.io.filemanager.utils.DateUtils;
import crazysheep.io.filemanager.utils.DialogUtils;
import crazysheep.io.filemanager.utils.FileUtils;
import crazysheep.io.filemanager.utils.SnackBarUtils;
import crazysheep.io.filemanager.utils.StringUtils;
import io.codetail.widget.RevealFrameLayout;
import rx.Observable;
import rx.functions.Action1;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    @Bind(android.R.id.content) View mRootView;
    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.file_rv) RecyclerView mFileRv;
    @Bind(R.id.fab) FloatingActionButton mFab;
    @Bind(R.id.fab_sheet_cv) CardView mFabSheetCv;
    @Bind(R.id.fab_sheet_rfl) RevealFrameLayout mFabRevealFl;
    @Bind(R.id.action_cancel_iv) ImageView mCancelIv;
    @Bind(R.id.action_copy_iv) ImageView mFileCopyIv;
    @Bind(R.id.action_cut_iv) ImageView mFileCutIv;
    @Bind(R.id.action_delete_iv) ImageView mFileDeleteIv;
    @Bind(R.id.action_info_iv) ImageView mFileInfoIv;
    @Bind(R.id.fab_menu_rll) RevealFrameLayout mFabMenuRevealLl;
    @Bind(R.id.fab_menu_fl) View mFabMenuParentFl;
    @Bind(R.id.create_folder_fab) FloatingActionButton mCreateFolderFab;
    @Bind(R.id.edit_mode_fab) FloatingActionButton mEditModeFab;
    @Bind(R.id.search_file_fab) FloatingActionButton mSearchFab;
    @Bind(R.id.external_space_tv) TextView mExternalSpaceTv;

    public static final int REQUEST_CODE_SEARCH = 1000;

    private FabMenuAnimatorHelper.Builder mMenuAnimatorBuilder;
    private FabAnimatorHelper.Builder mFabAnimatorBuilder;

    private LinearLayoutManager mLayoutMgr;
    private FilesAdapter mFileAdapter;
    private SettingsPrefs mSettingsPrefs;

    private File mCurrentDir;
    private LinkedList<ScanDirDto> mFileStack = new LinkedList<>();

    private Snackbar mSnackBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mSettingsPrefs = new SettingsPrefs(this);

        setSupportActionBar(mToolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initUI();
    }

    private void initUI() {
        updateStorageUI();

        mFabAnimatorBuilder = FabAnimatorHelper.wrap(mFab, mFabSheetCv, mFabRevealFl)
                .setExpandedListener(new FabAnimatorHelper.DefaultAnimatorListener() {
                    @Override
                    public void onAnimationEnd() {
                        toggleEditMode(true);
                    }
                })
                .setClosedListener(new FabAnimatorHelper.DefaultAnimatorListener() {
                    @Override
                    public void onAnimationEnd() {
                        toggleEditMode(false);
                    }
                });
        mMenuAnimatorBuilder = FabMenuAnimatorHelper.wrap(mFabMenuRevealLl, mFabMenuParentFl, mFab)
                .addFab(mCreateFolderFab, R.id.create_folder_fab)
                .addFab(mEditModeFab, R.id.edit_mode_fab)
                .addFab(mSearchFab, R.id.search_file_fab)
                .setOnFabMenuItemClickListener(new FabMenuAnimatorHelper.OnFabItemClickListener() {
                    @Override
                    public void onFabItemClick(FloatingActionButton fab, int id) {
                        switch (id) {
                            case R.id.edit_mode_fab: {
                                animateFab();
                            }
                            break;

                            case R.id.create_folder_fab: {
                                showCreateFolderDialog();
                            }
                            break;

                            case R.id.search_file_fab: {
                                // search file
                                ActivityUtils.startResult(getActivity(), REQUEST_CODE_SEARCH,
                                        ActivityUtils.prepare(getActivity(), SearchActivity.class));
                                overridePendingTransition(0, 0);
                            }break;
                        }
                    }
                });
        mFab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!mMenuAnimatorBuilder.isAnimating() && !mMenuAnimatorBuilder.isExpanded())
                    mMenuAnimatorBuilder.expanded();
            }
        });

        mCancelIv.setOnClickListener(this);
        mFileCopyIv.setOnClickListener(this);
        mFileCutIv.setOnClickListener(this);
        mFileDeleteIv.setOnClickListener(this);
        mFileInfoIv.setOnClickListener(this);

        mLayoutMgr = new LinearLayoutManager(this);
        mFileRv.setLayoutManager(mLayoutMgr);
        mFileAdapter = new FilesAdapter(this, null, mSettingsPrefs.getShowHiddenFiles()
                ? FilesAdapter.MODE_SHOW_HIDDEN_FILES : FilesAdapter.MODE_NOT_SHOW_HIDDEN_FILES);
        mFileRv.setAdapter(mFileAdapter);
        mFileAdapter.setOnItemClickListener(new RecyclerViewBaseAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (mFileAdapter.isEditingMode()) {
                    mFileAdapter.toggleItemChoose(position);
                } else {
                    // click item file
                    FileItemDto itemModel = mFileAdapter.getItem(position);
                    File file = new File(itemModel.filepath);
                    if (file.isDirectory()) {
                        // save last directory first visible position and offset
                        pushDirToStack(mCurrentDir);

                        Observable.just(file)
                                .delay(150, TimeUnit.MILLISECONDS)
                                .subscribe(new Action1<File>() {
                                    @Override
                                    public void call(File file) {
                                        doScanDir(new ScanDirDto(file, 0, 0));
                                    }
                                });
                    } else {
                        Intent openIntent = new Intent(Intent.ACTION_VIEW);
                        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        openIntent.setDataAndType(Uri.fromFile(file), FileUtils.getMimeType(file));
                        try {
                            startActivity(openIntent);
                        } catch (ActivityNotFoundException anfe) {
                            anfe.printStackTrace();

                            SnackBarUtils.show(mRootView, R.string.msg_can_not_open_file,
                                    Snackbar.LENGTH_LONG);
                        }
                    }
                }
            }
        });
        mFileAdapter.setOnItemLongClickListener(new RecyclerViewBaseAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(View view, int position) {
                // quick go to edit mode
                if (!mFileAdapter.isEditingMode()) {
                    toggleEditMode(true);
                    mFileAdapter.toggleItemChoose(position);
                    animateFab();
                }

                return true;
            }
        });
        mFileRv.setItemAnimator(new DefaultItemAnimator());

        // request permission: READ_EXTERNAL_STORAGE
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                new PermissionsResultAction() {
                    @Override
                    public void onGranted() {
                        doScanDir(new ScanDirDto(Environment.getExternalStorageDirectory(), 0, 0));
                    }

                    @Override
                    public void onDenied(String permission) {
                        SnackBarUtils.show(mRootView, getString(R.string.msg_permission_denied),
                                Snackbar.LENGTH_LONG);
                    }
                });
    }

    private void updateStorageUI() {
        // external storage info, tint different color
        String usedStr = FileUtils.formatFileSize(FileIO.externalUsed());
        String totalStr = FileUtils.formatFileSize(FileIO.externalTotal());
        String rawStr = getString(R.string.tv_external_storage, usedStr, totalStr);
        mExternalSpaceTv.setText(StringUtils.highlight(usedStr, rawStr,
                FileUtils.colorStorage(this, FileIO.externalUsed(), FileIO.externalTotal())));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SEARCH: {
                    pushDirToStack(mCurrentDir);

                    String path = data.getStringExtra(SearchActivity.EXTRA_FILE_PATH);
                    doScanDir(new ScanDirDto(new File(path), 0, 0));
                }break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_cancel_iv: {
                reverseAnimateFab();
            }break;

            case R.id.action_copy_iv: {
                if(hasChosenFiles()) {
                    // back to normal mode
                    reverseAnimateFab();

                    // copy file
                    final List<File> sources = FileItemDtoHelper.changeItems2Files(
                            mFileAdapter.getChosenItems());
                    String actionMsg = getString(R.string.msg_choose_directory_to_copy_files,
                            sources.size());
                    mSnackBar = Snackbar.make(mRootView, actionMsg, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.action_paste, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    copyFile(sources, mCurrentDir);
                                }
                            });
                    mSnackBar.getView().setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            SnackBarUtils.dismiss(mSnackBar);
                        }
                    });
                    mSnackBar.setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            mFabAnimatorBuilder.resetAndShowFab();
                        }

                        @Override
                        public void onShown(Snackbar snackbar) {
                            mFabAnimatorBuilder.hideFab();
                        }
                    });
                    mSnackBar.show();
                }
            }break;

            case R.id.action_cut_iv: {
                if(hasChosenFiles()) {
                    // back to normal mode
                    reverseAnimateFab();

                    // move file
                    final List<File> sources = FileItemDtoHelper.changeItems2Files(
                            mFileAdapter.getChosenItems());
                    String actionMsg = getString(R.string.msg_choose_directory_to_move_files,
                            sources.size());
                    mSnackBar = Snackbar.make(mRootView, actionMsg, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.action_paste, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    moveFile(sources, mCurrentDir);
                                }
                            });
                    mSnackBar.getView().setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            SnackBarUtils.dismiss(mSnackBar);
                        }
                    });
                    mSnackBar.setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            mFabAnimatorBuilder.resetAndShowFab();
                        }

                        @Override
                        public void onShown(Snackbar snackbar) {
                            mFabAnimatorBuilder.hideFab();
                        }
                    });
                    mSnackBar.show();
                }
            }break;

            case R.id.action_delete_iv: {
                if(hasChosenFiles()) {
                    final List<FileItemDto> chooseFiles = mFileAdapter.getChosenItems();
                    String dialogContent = chooseFiles.size() == 1
                            ? getString(R.string.tv_delete_sing_file, chooseFiles.get(0).filename)
                                    : getString(R.string.tv_delete_files, chooseFiles.size());
                    DialogUtils.showConfirmDialog(this, getString(R.string.tv_title_delete_files),
                            dialogContent,
                            new DialogUtils.ButtonAction() {
                                @Override
                                public String getTitle() {
                                    return getString(R.string.ok);
                                }

                                @Override
                                public void onClick(DialogInterface dialog) {
                                    DialogUtils.dismissDialog((Dialog)dialog);

                                    requestToDeleteChosenFilesAndNotify();
                                }
                            },
                            new DialogUtils.ButtonAction() {
                                @Override
                                public String getTitle() {
                                    return getString(R.string.cancel);
                                }

                                @Override
                                public void onClick(DialogInterface dialog) {
                                    DialogUtils.dismissDialog((Dialog) dialog);
                                }
                            });
                }
            }break;

            case R.id.action_info_iv: {
                if(hasChosenFiles()) {
                    View contentView = LayoutInflater.from(this).inflate(
                            R.layout.dialog_file_info, null);
                    TextView fileTypeTv = ButterKnife.findById(contentView, R.id.file_type_tv);
                    TextView filePathTv = ButterKnife.findById(contentView, R.id.file_path_tv);
                    final TextView fileSizeTv = ButterKnife.findById(contentView, R.id.file_size_tv);
                    fileSizeTv.setText(R.string.tv_file_size_calculating);
                    TextView fileLastModifiedTv = ButterKnife.findById(contentView,
                            R.id.file_last_modified_time_tv);
                    // show file info dialog
                    if(mFileAdapter.getChosenItems().size() == 1) {
                        File file = new File(mFileAdapter.getChosenItems().get(0).filepath);
                        SingleFileInfoDto fileInfoDto = FileUtils.parseFileInfo(file);

                        fileTypeTv.setText(file.isDirectory()
                                ? getString(R.string.tv_file_dir) : fileInfoDto.filetype);
                        filePathTv.setText(fileInfoDto.filepath);
                        fileLastModifiedTv.setText(DateUtils.formatTime(fileInfoDto.lastmodified));
                    } else if(mFileAdapter.getChosenItems().size() > 1) {
                        List<File> files = new ArrayList<>(mFileAdapter.getChosenItems().size());
                        for(FileItemDto item : mFileAdapter.getChosenItems())
                            files.add(new File(item.filepath));
                        MultiFileInfoDto fileInfoDto = FileUtils.parseFileInfo(files);

                        fileTypeTv.setText(getString(R.string.tv_multi_files,
                                fileInfoDto.filecount));
                        filePathTv.setText(getString(R.string.tv_multi_file_path,
                                fileInfoDto.fileinfoList.get(0).filepath,
                                fileInfoDto.fileinfoList.get(1).filepath));
                    }
                    // calculate file size
                    FileIO.size(FileItemDtoHelper.changeItems2Files(mFileAdapter.getChosenItems()),
                            new FileIO.OnIOSizeListener() {
                                @Override
                                public void onSizeOf(long size) {
                                    fileSizeTv.setText(FileUtils.formatFileSize(size));
                                }

                                @Override
                                public void onError(String err) {
                                    DialogUtils.showSingleConfirmDialog(getActivity(),
                                            getString(R.string.tv_opps), err);
                                }
                            });

                    DialogUtils.showCustomDialog(this, getString(R.string.tv_file_info),
                            contentView);
                }
            }break;
        }
    }

    private void toggleEditMode(boolean goEditMode) {
        if(goEditMode) {
             mFileAdapter.setEditMode(FilesAdapter.EDIT_MODE_EDITING);
        } else {
            mFileAdapter.setEditMode(FilesAdapter.EDIT_MODE_NORMAL);
        }

        invalidateOptionsMenu(); // recreate options menu
    }

    private boolean hasChosenFiles() {
        if(mFileAdapter.getChosenItems().size() == 0) {
            SnackBarUtils.show(mRootView, R.string.msg_have_not_choose_file,
                    Snackbar.LENGTH_LONG);

            return false;
        }

        return true;
    }

    private void animateFab() {
        mFabAnimatorBuilder.expanded();
    }

    private void reverseAnimateFab() {
        mFabAnimatorBuilder.closed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(mMenuAnimatorBuilder.isExpanded()) {
            mMenuAnimatorBuilder.closed();
        } else if(mFabAnimatorBuilder.isAnimating()) {
            // nothing
        } else if(mFabAnimatorBuilder.isExpanded()) {
            reverseAnimateFab();
        } else if(mFileAdapter.isEditingMode()) {
            mFileAdapter.setEditMode(FilesAdapter.EDIT_MODE_NORMAL);

            invalidateOptionsMenu();
        } else if(mFileStack.size() > 0) {
            doScanDir(mFileStack.pop());
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mFabAnimatorBuilder.isAnimating() || super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if(!mFileAdapter.isEditingMode())
            menu.findItem(R.id.action_show_hidden_files)
                    .setTitle(mSettingsPrefs.getShowHiddenFiles()
                            ? R.string.action_not_show_hidden_files
                            : R.string.action_show_hidden_files)
                    .setVisible(true);
        else
            menu.findItem(R.id.action_show_hidden_files).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_show_hidden_files: {
                mSettingsPrefs.setShowHiddenFiles(!mSettingsPrefs.getShowHiddenFiles());
                mFileAdapter.setHiddenMode(mSettingsPrefs.getShowHiddenFiles()
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
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void requestToDeleteChosenFilesAndNotify() {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                new PermissionsResultAction() {
                    @Override
                    public void onGranted() {
                        deleteFile(mFileAdapter.getChosenItems());
                    }

                    @Override
                    public void onDenied(String permission) {
                        SnackBarUtils.show(mRootView, getString(R.string.msg_permission_denied),
                                Snackbar.LENGTH_LONG);
                    }
                });
    }

    private void showCreateFolderDialog() {
        DialogUtils.showInputDialog(getActivity(), getString(R.string.tv_create_folder),
                getString(R.string.hint_input_new_folder_name),
                new DialogUtils.InputCallback() {
                    @Override
                    public void onInput(DialogInterface dialog, final String s) {
                        final File newFolder = new File(mCurrentDir, s);
                        if (newFolder.exists()) {
                            SnackBarUtils.show(mRootView, R.string.msg_folder_is_exist,
                                    Snackbar.LENGTH_LONG);

                            return;
                        }

                        if (newFolder.mkdirs()) {
                            // refresh current dir
                            refreshCurDir();

                            // TODO for good UX, auto select the new folder
                        }
                    }
                });
    }

    private void doScanDir(final ScanDirDto dirBean) {
        if(dirBean.dir.isDirectory() && dirBean.dir.exists()) {
            mCurrentDir = dirBean.dir;

            ScanDirTask.doScan(mCurrentDir, new ScanDirTask.OnScanListener() {
                @Override
                public void onScanDone(List<FileItemDto> items) {
                    mFileAdapter.setData(items);

                    mLayoutMgr.scrollToPositionWithOffset(dirBean.lastTopPosition,
                            dirBean.lastTopPositionOffset);
                }

                @Override
                public void onError(String err) {
                    if(!TextUtils.isEmpty(err))
                        SnackBarUtils.show(mRootView, err, Snackbar.LENGTH_LONG);
                }
            });
        }
    }

    private void pushDirToStack(@NonNull File dir) {
        // save last directory first visible position and offset
        int firstVisibleItemPosition = mLayoutMgr.findFirstVisibleItemPosition();
        ScanDirDto dirBean = new ScanDirDto(dir, firstVisibleItemPosition,
                mLayoutMgr.findViewByPosition(firstVisibleItemPosition).getTop());
        mFileStack.push(dirBean);
    }

    private void refreshCurDir() {
        doScanDir(new ScanDirDto(mCurrentDir, 0, 0));
    }

    private void moveFile(@NonNull List<File> sources, @NonNull File targetDir) {
        FileIO.move(sources, targetDir, new FileIO.OnIOActionListener() {
            @Override
            public void onSuccess() {
                refreshCurDir();
            }

            @Override
            public void onError(String err) {
                DialogUtils.showSingleConfirmDialog(getActivity(),
                        getString(R.string.tv_opps), err);
            }
        });
    }

    private void copyFile(@NonNull List<File> sources, @NonNull File targetDir) {
        FileIO.copy(sources, targetDir, new FileIO.OnIOActionListener() {
            @Override
            public void onSuccess() {
                refreshCurDir();
            }

            @Override
            public void onError(String err) {
                DialogUtils.showSingleConfirmDialog(getActivity(),
                        getString(R.string.tv_opps), err);
            }
        });
    }

    private void deleteFile(@NonNull final List<FileItemDto> sources) {
        final Dialog loadingDlg = DialogUtils.showLoadingDialog(getActivity(),
                getString(R.string.tv_please_waiting),
                false, false);
        FileIO.delete(FileItemDtoHelper.changeItems2Files(sources), new FileIO.OnIOActionListener() {
            @Override
            public void onSuccess() {
                DialogUtils.dismissDialog(loadingDlg);

                mFileAdapter.removeItems(sources);
            }

            @Override
            public void onError(String err) {
                DialogUtils.dismissDialog(loadingDlg);

                DialogUtils.showSingleConfirmDialog(getActivity(),
                        getString(R.string.tv_opps), err);
            }
        });
    }

}
