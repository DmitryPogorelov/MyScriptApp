package com.example.myscript;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
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
    int cameraFacing;
    private Size previewSize;
    private String camIDtoUse;
    private CameraDevice myCam;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private CameraCaptureSession gCameraCaptureSession;

    HandlerThread backgroundThread;
    Handler backgroundHandler;

    private TextureView currTextureView;
    private ImageButton btnCamShoot, btnReturn;

    //Название папки для сохранения файлов
    private File galleryFolder;
    private String fullFileName;
    private String fullFileNameWithPath;

    //Константа-название передаваемого пути файла
    public static final String PICTURE_NAME = "picture_name";

    //Переменная для хранения row_id, который пришел из формы редактирования
    private int rowToUpdate = -1;
    private String fieldTitle, fieldContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_view);
        //Убираем ActionBar
        ActionBar currActionBar = getSupportActionBar();
        currActionBar.hide();
        //Привязываем переменные к элементам
        currTextureView = findViewById(R.id.camTextureView);
        btnCamShoot = findViewById(R.id.btnCamShoot);
        btnReturn = findViewById(R.id.btnReturn);

        //Привязываем ClickListenerы
        btnReturn.setOnClickListener(returnButtonClkListener);
        btnCamShoot.setOnClickListener(shootButtonClkListener);

        //Создаем папку для фотографий
        createImageGallery();

        camManager = (CameraManager)getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        currTextureView.setSurfaceTextureListener(surfaceTextureListener);

    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {

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

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
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
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
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
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    //Обработчик нажатия на кнопку возврата
    private View.OnClickListener returnButtonClkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    //ОБработчик нажатия на кнопку съёмки
    private View.OnClickListener shootButtonClkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onTakePhotoButtonClicked();


            Bundle varSet = getIntent().getExtras();

            //Если был переход из формы редактирования, то восстанавливаем переменную с ROW_ID
            if (varSet != null && varSet.containsKey(MyScriptDB.ROW_ID))
                rowToUpdate = varSet.getInt(MyScriptDB.ROW_ID);

            if (varSet != null && varSet.containsKey(ScriptEdit.TITLE_FIELD_CONTENT))
                fieldTitle = varSet.getString(ScriptEdit.TITLE_FIELD_CONTENT);

            if (varSet != null && varSet.containsKey(ScriptEdit.CONTENT_FIELD_CONTENT))
                fieldContent = varSet.getString(ScriptEdit.CONTENT_FIELD_CONTENT);

            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), ScriptEdit.class);

            //Сохраняем в новый Intent все нужные значения
            if (rowToUpdate > -1)
                intent.putExtra(MyScriptDB.ROW_ID, rowToUpdate);

            intent.putExtra(ScriptEdit.TITLE_FIELD_CONTENT, fieldTitle);
            intent.putExtra(ScriptEdit.CONTENT_FIELD_CONTENT, fieldContent);

            intent.putExtra(PICTURE_NAME, fullFileNameWithPath);

            startActivity(intent);
        }
    };

    //
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
    private void createImageGallery() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name));
        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
        }
    }

    private File createImageFile(File galleryFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp;
        fullFileName = imageFileName + ".png";
        return File.createTempFile(imageFileName, ".png", galleryFolder);
    }

    public void onTakePhotoButtonClicked() {
        //lock();
        FileOutputStream outputPhoto = null;
        try {
            outputPhoto = new FileOutputStream(createImageFile(galleryFolder));
            boolean res = currTextureView.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
            if (res)
                fullFileNameWithPath = galleryFolder.getAbsolutePath() + "/" + fullFileName;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            //unlock();
            try {
                if (outputPhoto != null) {
                    outputPhoto.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void lock() {
        try {
            gCameraCaptureSession.capture(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlock() {
        try {
            gCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
