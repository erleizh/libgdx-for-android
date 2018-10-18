package com.erlei.videorecorder.recorder;

import android.graphics.Bitmap;

import java.io.File;

public interface IVideoRecorder {

    void startRecord();

    void stopRecord();

    boolean isRecording();

    /**
     * 拍照
     */
    void takePicture(TakePictureCallback callback);


    interface TakePictureCallback {

        /**
         * @param picture 图片文件
         *                在UI线程调用
         */
        void onPictureTaken(File picture);

        /**
         * @param bitmap bitmap
         * @return File 返回一个图片存储地址 , 如果返回null , 那么表示不需要保存为文件, 将不会调用 onPictureTaken
         * 此方法工作在后台线程
         */
        File onPictureTaken(Bitmap bitmap);

    }

}
