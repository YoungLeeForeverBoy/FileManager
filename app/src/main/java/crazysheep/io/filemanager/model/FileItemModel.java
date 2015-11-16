package crazysheep.io.filemanager.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;

/**
 * file item model
 *
 * Created by crazysheep on 15/11/12.
 */
@ParcelablePlease
public class FileItemModel implements Parcelable {

    public static final int TYPE_DIR = 0;
    public static final int TYPE_FILE = 1;

    public static final int ILLEGAL_SUBFILE_COUNT = -1;
    public static final long ILEGAL_FILE_BYTE_COUNT = -1;

    public String filename;
    public String filepath;
    public int filetype;
    public boolean isHidden;
    public int subfileCount; // sub file count for directory
    public long fileByteCount; // file byte count for single file
    public long fileLastModified;

    public boolean isDir() {
        return filetype == TYPE_DIR;
    }

    public boolean isHidden() {
        return isHidden;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        FileItemModelParcelablePlease.writeToParcel(this, dest, flags);
    }

    public static final Creator<FileItemModel> CREATOR = new Creator<FileItemModel>() {
        public FileItemModel createFromParcel(Parcel source) {
            FileItemModel target = new FileItemModel();
            FileItemModelParcelablePlease.readFromParcel(target, source);
            return target;
        }

        public FileItemModel[] newArray(int size) {
            return new FileItemModel[size];
        }
    };
}
