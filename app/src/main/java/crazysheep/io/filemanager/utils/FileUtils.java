package crazysheep.io.filemanager.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.List;

import crazysheep.io.filemanager.R;
import crazysheep.io.filemanager.model.MultiFileInfoDto;
import crazysheep.io.filemanager.model.SingleFileInfoDto;
import rx.Observable;
import rx.functions.Action1;

/**
 * file utils
 *
 * Created by crazysheep on 15/11/16.
 */
public class FileUtils {

    /**
     * get extension from file
     *
     * @param file The target file to parse extension
     * */
    public static String getExtension(@NonNull File file) {
        return MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
    }

    /**
     * get mimetype from file's extension
     *
     * @param file The target file to parse mimetype
     * */
    public static String getMimeType(@NonNull File file) {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(getExtension(file));
    }

    /**
     * check if target is image file
     *
     * @param file The target file to check if it's a image file
     * */
    public static boolean isPicture(@NonNull File file) {
        String mimetype = getMimeType(file);
        return !TextUtils.isEmpty(mimetype) && mimetype.startsWith("image/");
    }

    /**
     * for file byte count for human readable
     * */
    public static String formatFileSize(long bytecount) {
        if (bytecount <= 0) {
            return "0kb";
        } else {
            StringBuilder sb = new StringBuilder();
            if(bytecount >= 1024 * 1024 * 1024) { // GB
                float gbSize = bytecount * 1f / (1024 * 1024 * 1024);

                return sb.append(String.format("%.2f", gbSize)).append("GB").toString();
            } else if(bytecount >= 1024 * 1024) { // MB
                float mbSize = bytecount * 1f / (1024 * 1024);

                return sb.append(String.format("%.2f", mbSize)).append("MB").toString();
            } else if(bytecount >= 1024) { // KB
                float kbSize = bytecount * 1f / 1024;

                return sb.append(String.format("%.2f", kbSize)).append("KB").toString();
            } else { // byte
                float byteSize = bytecount * 1f;

                return sb.append(String.format("%.2f", byteSize)).append("b").toString();
            }
        }
    }

    /**
     * parse single file info
     * */
    public static SingleFileInfoDto parseFileInfo(@NonNull File file) {
        return SingleFileInfoDto.parseInfoFromFile(file);
    }

    /**
     * parse multi files info
     * */
    public static MultiFileInfoDto parseFileInfo(@NonNull List<File> files) {
        final MultiFileInfoDto multiInfoDto = new MultiFileInfoDto();
        Observable.from(files)
                .subscribe(new Action1<File>() {
                    @Override
                    public void call(File file) {
                        multiInfoDto.filecount++;
                        SingleFileInfoDto fileInfo = SingleFileInfoDto.parseInfoFromFile(file);
                        multiInfoDto.addSingleFileInfo(fileInfo);
                    }
                });

        return multiInfoDto;
    }

    public static long sizeOfFile(@NonNull File file) {
        if(!file.exists())
            return 0l;

        return org.apache.commons.io.FileUtils.sizeOf(file);
    }

    /**
     * get color stand for storage
     * */
    public static int colorStorage(@NonNull Context context, long available, long total) {
        float fact = available * 1f / total;
        int color;
        if(fact > 1f)
            return ContextCompat.getColor(context, R.color.text_dark_grey);
        else if(fact > 0.9f)
            color = ContextCompat.getColor(context, R.color.storage_dangerous);
        else if(fact > 0.6f)
            color = ContextCompat.getColor(context, R.color.storage_warn);
        else
            color = ContextCompat.getColor(context, R.color.storage_good);

        return color;
    }

}
