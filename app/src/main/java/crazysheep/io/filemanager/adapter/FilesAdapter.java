package crazysheep.io.filemanager.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import crazysheep.io.filemanager.R;
import crazysheep.io.filemanager.model.FileItemDto;
import crazysheep.io.filemanager.utils.CollectionUtils;
import crazysheep.io.filemanager.utils.DateUtils;
import crazysheep.io.filemanager.utils.FileUtils;
import crazysheep.io.filemanager.utils.PinyinUtils;

/**
 * adapter for file RecyclerView
 *
 * Created by crazysheep on 15/11/12.
 */
public class FilesAdapter extends RecyclerViewBaseAdapter<FilesAdapter.FileHolder, FileItemDto>
        implements SectionIndexer {

    public static final int MODE_SHOW_HIDDEN_FILES = 0;
    public static final int MODE_NOT_SHOW_HIDDEN_FILES = 1;
    private int mCurrentMode = MODE_NOT_SHOW_HIDDEN_FILES;

    public static final int EDIT_MODE_NORMAL = 10;
    public static final int EDIT_MODE_EDITING = 11;
    private int mEditMode = EDIT_MODE_NORMAL;

    private List<FileItemDto> mFiles;
    private SparseArray<Boolean> mChooseFileMap;

    public FilesAdapter(Context context, List<FileItemDto> files, int hiddenMode) {
        super(context, files);

        mCurrentMode = hiddenMode;
        mChooseFileMap = new SparseArray<>();

        sortFiles();
        filterHidden();
        resetItemChooseState();
    }

    @Override
    public void setData(List<FileItemDto> files) {
        mItems = files;
        if(mItems == null)
            mItems = new ArrayList<>();
        sortFiles();
        filterHidden();
        resetItemChooseState();

        notifyDataSetChanged();
    }

    public void setHiddenMode(int mode) {
        mCurrentMode = mode;
        filterHidden();

        resetItemChooseState();

        notifyDataSetChanged();
    }

    public void setEditMode(int mode) {
        if(mEditMode != mode) {
            mEditMode = mode;

            if(mEditMode == EDIT_MODE_EDITING)
                ;// nothing
            else
                resetItemChooseState();

            notifyDataSetChanged();
        }
    }

    public boolean isEditingMode() {
        return mEditMode == EDIT_MODE_EDITING;
    }

    public void toggleItemChoose(int position) {
        if(mEditMode == EDIT_MODE_EDITING) {
            mChooseFileMap.put(position, !mChooseFileMap.get(position));

            notifyItemChanged(position);
        }
    }

    private void resetItemChooseState() {
        mChooseFileMap = new SparseArray<>(mFiles.size());
        for(int i = 0; i < mFiles.size(); i++)
            mChooseFileMap.put(i, false);
    }

    public List<FileItemDto> getChosenItems() {
        List<FileItemDto> chosenItems = new ArrayList<>();
        for(int i = 0; i < mChooseFileMap.size(); i++)
            if(mChooseFileMap.valueAt(i))
                chosenItems.add(mFiles.get(mChooseFileMap.keyAt(i)));

        return chosenItems;
    }

    public void removeItems(@NonNull List<FileItemDto> items) {
        for(FileItemDto item : items) {
            int removeIndex = mFiles.indexOf(item);
            mFiles.remove(item);
            mItems.remove(item);

            notifyItemRemoved(removeIndex);
        }

        resetItemChooseState();
    }

    @Override
    public FileHolder onCreateHolder(ViewGroup parent, int viewType) {
        View convertView = mInflater.inflate(R.layout.layout_file_item, parent, false);

        return new FileHolder(convertView);
    }

    @Override
    public void onBindViewHolder(FileHolder holder, int position) {
        FileItemDto itemModel = mFiles.get(position);
        if(itemModel.isDir())
            holder.mFileCoverIv.setImageResource(R.drawable.ic_folder_blue);
        else
            holder.mFileCoverIv.setImageResource(R.drawable.ic_insert_drive_file);

        holder.mFileNameTv.setText(itemModel.filename);
        if(itemModel.isDir())
            holder.mFileSubCountTv.setText(
                    itemModel.subfileCount != FileItemDto.ILLEGAL_SUBFILE_COUNT
                            ? mContext.getString(R.string.tv_file_sub_count, itemModel.subfileCount)
                                    : null);
        else
            holder.mFileSubCountTv.setText(FileUtils.formatFileSize(itemModel.fileByteCount));
        holder.mFileLastModifiedTimeTv.setText(DateUtils.formatTime(itemModel.fileLastModified));
        if(isEditingMode()) {
            holder.mFileChooseCb.setVisibility(View.VISIBLE);
            holder.mFileChooseCb.setChecked(mChooseFileMap.get(position));
        } else {
            holder.mFileChooseCb.setVisibility(View.GONE);
        }

        if(isFooter(holder.getAdapterPosition()))
            holder.mFooterBlankSpaceV.setVisibility(View.VISIBLE);
        else
            holder.mFooterBlankSpaceV.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public boolean isFooter(int position) {
        return position == getItemCount() - 1;
    }

    private void sortFiles() {
        List<FileItemDto> dirs = new ArrayList<>();
        List<FileItemDto> files = new ArrayList<>();
        for(FileItemDto itemModel : mItems)
            if(itemModel.isDir())
                dirs.add(itemModel);
            else
                files.add(itemModel);

        // first sort directory files
        Collections.sort(dirs, new FileComparator());
        // second sort files
        Collections.sort(files, new FileComparator());

        List<FileItemDto> totalFiles = new ArrayList<>(mItems.size());
        totalFiles.addAll(dirs);
        totalFiles.addAll(files);
        mItems = totalFiles;
    }

    private static class FileComparator implements Comparator<FileItemDto> {

        @Override
        public int compare(FileItemDto lhs, FileItemDto rhs) {
            return lhs.filename.compareTo(rhs.filename);
        }
    }

    private void filterHidden() {
        if(mCurrentMode == MODE_SHOW_HIDDEN_FILES) {
            mFiles = mItems;
        } else {
            mFiles = new ArrayList<>();
            for(FileItemDto itemModel : mItems)
                if(!itemModel.isHidden())
                    mFiles.add(itemModel);
        }
    }

    @Override
    public Object[] getSections() {
        return mContext.getResources().getStringArray(R.array.section_array);
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        String firstChar = mFiles.get(position).filename.trim();
        if(PinyinUtils.isChinese(firstChar))
            firstChar = PinyinUtils.chineneToSpell(mContext, firstChar);
        firstChar = PinyinUtils.getAlpha(firstChar);

        return CollectionUtils.findPosition(getSections(), firstChar);
    }

    ///////////////////////////// ViewHolder //////////////////////////////////

    public static class FileHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.file_cover_iv) ImageView mFileCoverIv;
        @Bind(R.id.file_name_tv) TextView mFileNameTv;
        @Bind(R.id.file_sub_count_tv) TextView mFileSubCountTv;
        @Bind(R.id.file_last_modified_time_tv) TextView mFileLastModifiedTimeTv;
        @Bind(R.id.file_item_choose_cb) CheckBox mFileChooseCb;
        @Bind(R.id.footer_blank_space) View mFooterBlankSpaceV;

        public FileHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
