package com.pdnsoftware.writtendone;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class CameraView extends AppCompatActivity {

    private CameraManager camManager;
    private int cameraFacing;
    private Size previewSize;
    private String camIDtoUse;
    private CameraDevice myCam;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private CameraCaptureSession gCameraCaptureSession;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private TextureView currTextureView;

    //Константа-название данного Активити
    private static final String NAME_INTENT_CAMERAVIEW = "intent_CameraView";

    //Переменная для хранения row_id, который пришел из формы редактирования
    private int rowToUpdate = -1;
    private String fieldTitle, fieldContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_view);

        //Убираем ActionBar
        ActionBar currActionBar = getSupportActionBar();

        if (currActionBar != null)
            currActionBar.hide();

        //Привязываем переменные к элементам
        currTextureView = findViewById(R.id.camTextureView);
        ImageButton btnCamShoot = findViewById(R.id.btnCamShoot);
        ImageButton btnReturn = findViewById(R.id.btnReturn);

        //Привязываем ClickListeners
        btnReturn.setOnClickListener(returnButtonClkListener);
        btnCamShoot.setOnClickListener(shootButtonClkListener);

        //Разбираем входящие параметры формы
        Bundle varSet = getIntent().getExtras();

        //Если был переход из формы редактирования, то восстанавливаем переменную с ROW_ID
        if (varSet != null && varSet.containsKey(MyScriptDB.ROW_ID))
            rowToUpdate = varSet.getInt(MyScriptDB.ROW_ID);

        if (varSet != null && varSet.containsKey(ScriptEdit.TITLE_FIELD_CONTENT))
            fieldTitle = varSet.getString(ScriptEdit.TITLE_FIELD_CONTENT);
        else
            fieldTitle = "";

        if (varSet != null && varSet.containsKey(ScriptEdit.CONTENT_FIELD_CONTENT))
            fieldContent = varSet.getString(ScriptEdit.CONTENT_FIELD_CONTENT);
        else
            fieldContent = "";

        camManager = (CameraManager)getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        currTextureView.setSurfaceTextureListener(surfaceTextureListener);

    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            setUpCamera();
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private void setUpCamera() {
        try {
            for (String cameraId : camManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                        camManager.getCameraCharacteristics(cameraId);

                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        cameraFacing) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    if (streamConfigurationMap != null)
                        previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                    camIDtoUse = cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                camManager.openCamera(camIDtoUse, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            myCam = cameraDevice;
            createPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            myCam = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            myCam = null;
        }
    };

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = currTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = myCam.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            myCam.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (myCam == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();
                                gCameraCaptureSession = cameraCaptureSession;
                                gCameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    //Обработчик нажатия на кнопку возврата
    private final View.OnClickListener returnButtonClkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            backToScriptEditActivity();
        }
    };

    //ОБработчик нажатия на кнопку съёмки
    private final View.OnClickListener shootButtonClkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            onTakePhotoButtonClicked();
            backToScriptEditActivity();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if (currTextureView.isAvailable()) {
            setUpCamera();
            openCamera();
        } else {
            currTextureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    private void closeCamera() {
        if (gCameraCaptureSession != null) {
            gCameraCaptureSession.close();
            gCameraCaptureSession = null;
        }

        if (myCam != null) {
            myCam.close();
            myCam = null;
        }
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }
    //Создаем галлерею
    static File createImageGallery(Context con) {
        try {
            File storageDirectory = con.getDir(con.getResources().getString(R.string.app_folder), Context.MODE_PRIVATE);
            if (storageDirectory == null)
                throw new IOException("Application folder not available!");

            File galleryFolder = new File(storageDirectory, con.getResources().getString(R.string.picture_folder));

            if (!galleryFolder.exists()) {
                boolean wasCreated = galleryFolder.mkdirs();
                if (!wasCreated)
                    return null;
                else
                    return galleryFolder;
            } else
                return galleryFolder;
        }
        catch (IOException e) {
            return null;
        }
    }

    //Создаем галлерею
    static File createThumbnailsGallery(Context con) {
        try {
            File storageDirectory = con.getDir(con.getResources().getString(R.string.app_folder), Context.MODE_PRIVATE);
            if (storageDirectory == null)
                throw new IOException("Application folder not available!");

            File thumbnailFolder = new File(storageDirectory, con.getResources().getString(R.string.thumbnail_folder));

            if (!thumbnailFolder.exists()) {
                boolean wasCreated = thumbnailFolder.mkdirs();
                if (!wasCreated)
                    return null;
                else
                    return thumbnailFolder;
            }
            return thumbnailFolder;
        }
        catch (IOException e) {
            return null;
        }
    }

    static File createImageFile(File galleryFolder) {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp + ".jpg";
        String mPath = galleryFolder.getAbsolutePath() + "/" + imageFileName;

        return new File(mPath);
    }

    private static File createThumbnailFile(File thumbnailFolder) {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "thumbnail_" + timeStamp + ".jpg";
        String mPath = thumbnailFolder.getAbsolutePath() + "/" + imageFileName;

        return new File(mPath);
    }

    private void onTakePhotoButtonClicked() {

        FileOutputStream outputPhoto = null;

        try {

            File galleryFolder = createImageGallery(getApplicationContext());
            if (galleryFolder == null)
                throw new IOException("Pictures directory was not found!");
            File imgFile = createImageFile(galleryFolder);

            outputPhoto = new FileOutputStream(imgFile);

            boolean res = currTextureView.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto);
            if (res) {

                String imgFilePath = imgFile.getAbsolutePath();
                String imgFileName = imgFile.getName();
                //Инициализируем базу для работы с ней
                //Менеджер БД
                MyScriptDBManager myDB = new MyScriptDBManager(getApplicationContext());

                //Создаем новую задачу в БД, если ее не было
                if (rowToUpdate == -1) {
                    ScriptRecord newRec = new ScriptRecord(MyScriptDB.EMPTY_ROW_ID, fieldTitle, fieldContent);
                    rowToUpdate = myDB.insertScript(newRec);
                }

                if (rowToUpdate > 0) {
                    //Добавляем картинку к задаче
                    int pictureRecId = myDB.insertPictureRecord (rowToUpdate, imgFilePath, imgFileName);
                    if (pictureRecId == -1)
                        Toast.makeText(this, getResources().getString(R.string.photoNotAddedToDBError), Toast.LENGTH_LONG).show();
                    else {
                        //Делаем thumbnail
                        File thumbnail = saveThumbnail(imgFile, getApplicationContext());

                        if (thumbnail != null)
                            myDB.addThumbnailName(pictureRecId, thumbnail);
                        else
                            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.errorThumbnailsDirCreated),
                                    Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.errorPicturesDirCreated),
                    Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.errorPicturesDirCreated),
                    Toast.LENGTH_LONG).show();
        }
        finally {
            try {
                if (outputPhoto != null) {
                    outputPhoto.flush();
                    outputPhoto.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Перехват нажатия кнопки Back
    @Override
    public void onBackPressed() {
        backToScriptEditActivity();
    }

    //Создаем новый Intent и возвращаемся в ScriptEdit Activity
    private void backToScriptEditActivity() {

        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), ScriptEdit.class);

        //Сохраняем в новый Intent все нужные значения
        if (rowToUpdate > -1)
            intent.putExtra(MyScriptDB.ROW_ID, rowToUpdate);

        intent.putExtra(ScriptEdit.TITLE_FIELD_CONTENT, fieldTitle);
        intent.putExtra(ScriptEdit.CONTENT_FIELD_CONTENT, fieldContent);
        intent.putExtra(MainActivity.CALLER_ACTIVITY_NAME, NAME_INTENT_CAMERAVIEW);
        startActivity(intent);
        finish();
    }

    //Функция делает правильные thumbnail и сохраняет их в папку
    static File saveThumbnail(File mainPicture, Context con) {
        File thumbnail = null;

        android.support.media.ExifInterface exifObject;
        int orientation;
        Bitmap myBitmap = null;
        Bitmap freshBitmap = null;
        FileOutputStream outputPhoto = null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;

        //Рассчитываем размеры картинок
        int pictWidth, pictHeight;
        int pictHeightInDp = 30;

        //Вычисляем размеры тумбнэйла
        pictHeight = pictWidth = pictHeightInDp * (int)con.getResources().getDisplayMetrics().density;

        if (mainPicture.exists()) {

            myBitmap = BitmapFactory.decodeFile(mainPicture.getAbsolutePath(), options);

            if (myBitmap == null)
                return null;

            try {

                exifObject = new android.support.media.ExifInterface(mainPicture.getAbsolutePath());

                orientation = exifObject.getAttributeInt(android.support.media.ExifInterface.TAG_ORIENTATION, android.support.media.ExifInterface.ORIENTATION_UNDEFINED);
                freshBitmap = ScriptEdit.rotateBitmap(myBitmap, orientation);

            }
            catch (IOException e) {
                e.printStackTrace();
            }

            if (freshBitmap != null) {
                freshBitmap = ThumbnailUtils.extractThumbnail(freshBitmap, pictWidth, pictHeight,
                                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

                outputPhoto = null;

                try {

                    File thumbnailGallery = CameraView.createThumbnailsGallery(con);
                    if (thumbnailGallery == null)
                        return null;

                    File thumbnailFile =  CameraView.createThumbnailFile(thumbnailGallery);

                    outputPhoto = new FileOutputStream(thumbnailFile);

                    boolean res = freshBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto);

                    if (res) {
                        thumbnail = thumbnailFile;
                    }

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                myBitmap = ThumbnailUtils.extractThumbnail(myBitmap, pictWidth, pictHeight,
                                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

                outputPhoto = null;

                try {

                    File thumbnailGallery = CameraView.createThumbnailsGallery(con);
                    if (thumbnailGallery == null)
                        return null;

                    File thumbnailFile =  CameraView.createThumbnailFile(thumbnailGallery);

                    outputPhoto = new FileOutputStream(thumbnailFile);

                    boolean res = myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto);

                    if (res) {
                        thumbnail = thumbnailFile;
                    }

                }
                catch (Exception e) {
                   e.printStackTrace();
                }
            }
        }

        try {
            if (outputPhoto != null) {
                outputPhoto.flush();
                outputPhoto.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (myBitmap != null) myBitmap.recycle();
        if (freshBitmap != null) freshBitmap.recycle();

        return thumbnail;
    }
}