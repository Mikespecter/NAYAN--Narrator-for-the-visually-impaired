package com.ImageCaptioning;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout;

import com.ImageCaptioning.captioner.R;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.mannan.translateapi.Language;
import com.mannan.translateapi.TranslateAPI;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.Locale;


import static android.content.ContentValues.TAG;
//import static junit.framework.Assert.assertNotNull;
//import static junit.framework.Assert.assertTrue;

public class MainActivity extends Activity {
    private static final String LOG_TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;

    RelativeLayout layout;

    private Button btnCamera;
    private Button btnSelect;
    private Button btnSpeak;
    private ImageView ivCaptured;
    private TextView tvLabel;
    private TextView tvLabel1;
    private TextView tvLabel2;

    private Uri fileUri;
    private ProgressDialog dialog;
    private Bitmap bmp;
    private Captioner captioner;
    private TextToSpeech tts;
    File sdcard = Environment.getExternalStorageDirectory();
    String modelDir = "/sdcard/Captioner";
    String cnn_modelProto = modelDir + "/cnn_deploy.prototxt";
    String lstm_modelProto = modelDir + "/lstm_deploy.prototxt";
    String modelBinary = modelDir + "/cnn_lstm.caffemodel";
    String vocabulary = modelDir + "/vocabulary.txt";
    String recognizedText;
    String translatedText;

    static {
        System.loadLibrary("caffe");
        System.loadLibrary("captioner_jni");
    }



    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission1 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE

            );
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verifyStoragePermissions(this);
        setContentView(R.layout.activity_main);
        tts = new TextToSpeech(this.getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    //tts.setLanguage(new Locale("en","IN"));
                    tts.setLanguage(new Locale("en","IN"));
                    tts.setPitch(1.3f);
                    tts.setSpeechRate(0.9f);
                    tts.speak("Welcome to NAYAN - The Narrator mobile application. Swipe left on screen for importing image from gallery. Swipe right on screen to click the image from camera. Swipe top on screen for reading the text again.", TextToSpeech.QUEUE_ADD ,null,null);
                }
            }
        });

        //tts.setLanguage(Locale.US);
        //tts.speak("a boy is playing football" +
         //       "aa", TextToSpeech.QUEUE_FLUSH, null);

        ivCaptured = (ImageView) findViewById(R.id.ivCaptured);
        tvLabel = (TextView) this.findViewById(R.id.tvlabel);
        tvLabel1 = (TextView) this.findViewById(R.id.tvlabel1);
        tvLabel2 = (TextView) this.findViewById(R.id.tvlabel2);
        btnCamera = (Button) this.findViewById(R.id.id_btnCamera);

     /*   if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA},110);
            btnCamera.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    initPrediction();
                    fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                    Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                    startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
                }
            });
        }
        else {
            btnCamera.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    initPrediction();
                    fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                    Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                    startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
                }
            });
        }
        */
       layout = findViewById(R.id.relativeLayout);

        layout.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {

            public void onSwipeRight() {
                tts.speak("You are clicking image using camera", TextToSpeech.QUEUE_ADD ,null,null);
                initPrediction();
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
            }
            public void onSwipeLeft() {
                tts.speak("You are importing image from gallery", TextToSpeech.QUEUE_ADD ,null,null);
                initPrediction();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
            public void onSwipeTop() {
                speak_out();
            }
        });

        //btnCamera = (Button) this.findViewById(R.id.id_btnCamera);

            btnCamera.setOnClickListener(new Button.OnClickListener() {

                public void onClick(View v) {
                    tts.speak("You are clicking image using camera", TextToSpeech.QUEUE_ADD ,null,null);
                   initPrediction();
                    fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                    Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                   startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
                }
            });


        btnSelect = (Button) this.findViewById(R.id.id_btnSelect);
        btnSelect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                tts.speak("You are importing image from gallery", TextToSpeech.QUEUE_ADD ,null,null);
                initPrediction();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });

        btnSpeak = (Button) this.findViewById(R.id.id_btnSpeak);
        btnSpeak.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                speak_out();
            }
        });


        captioner = new Captioner();
        captioner.setNumThreads(32);
        captioner.loadModel(cnn_modelProto,lstm_modelProto, modelBinary,vocabulary);

        float[] meanValues = {104, 117, 123};
        captioner.setMean(meanValues);
    }


    @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {



            if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT) && resultCode == RESULT_OK) {
                String imgPath;

                if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    imgPath = fileUri.getPath();
                } else {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    imgPath = cursor.getString(columnIndex);
                    cursor.close();
                }


                bmp = BitmapFactory.decodeFile(imgPath);
                Log.d(LOG_TAG, imgPath);
                Log.d(LOG_TAG, String.valueOf(bmp.getHeight()));
                Log.d(LOG_TAG, String.valueOf(bmp.getWidth()));



                dialog = ProgressDialog.show(MainActivity.this, "I am captioning...", "please wait..", true);

                CNNTask cnnTask = new CNNTask( );
                cnnTask.execute(imgPath);
                //TessBaseAPI tessTwo = new TessBaseAPI();
                //tessTwo.init(Environment.getExternalStorageDirectory().toString(), "eng");
                //tessTwo.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
               // tessTwo.setImage(bmp);
                //recognizedText = tessTwo.getUTF8Text();
                //tvLabel.setText(recognizedText);
                //tessTwo.end();
            } else {
                btnCamera.setEnabled(true);
                btnSelect.setEnabled(true);
            }

            super.onActivityResult(requestCode, resultCode, data);
        }

    private void initPrediction() {
        btnCamera.setEnabled(false);
        btnSelect.setEnabled(false);
        tvLabel.setText("");
    }

    private void speak_out() {
        String caption=tvLabel.getText().toString();
        String transText=tvLabel1.getText().toString();
        //tts.speak(caption, TextToSpeech.QUEUE_ADD ,null,null);

        tts.speak("English caption", TextToSpeech.QUEUE_ADD, null);
        tts.speak(caption, TextToSpeech.QUEUE_ADD, null,null);
        tts.speak("Hindi Caption", TextToSpeech.QUEUE_ADD, null,null);
        tts.speak(transText, TextToSpeech.QUEUE_ADD, null,null);
        tts.speak("Text in the document", TextToSpeech.QUEUE_ADD, null,null);
        //tts.setSpeechRate(0.6f);
        tts.speak(recognizedText, TextToSpeech.QUEUE_ADD, null,null);
        //tts.speak(translatedText, TextToSpeech.QUEUE_ADD, null,null);
       // tts.speak(recognizedText, TextToSpeech.QUEUE_FLUSH, null,null);
    }

    private class CNNTask extends AsyncTask<String, Void, String> {
        //private CNNListener listener;
        private long startTime;

        //public CNNTask(CNNListener listener) {
        //    this.listener = listener;
        //}

        @Override
        protected String doInBackground(String... strings) {
            startTime = SystemClock.uptimeMillis();
            return captioner.predictImage(strings[0]);
        }

        @Override
        protected void onPostExecute(String string) {
            Log.i(LOG_TAG, String.format("elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));
            onTaskCompleted(string);
            super.onPostExecute(string);
        }
    }

    /**
     * display the results on screen
     */

    public void onTaskCompleted(String result) {
        ivCaptured.setImageBitmap(bmp);
        //ivCaptured.setPadding(3,3,3,3);
        //ivCaptured.setBackgroundColor(Color.rgb(80, 255, 255));
      //  tvLabel.setText(IMAGENET_CLASSES[result]);
        tvLabel.setMovementMethod(new ScrollingMovementMethod());
        tvLabel.setText(result);
        final String Text = result;
        //tts.speak("English caption", TextToSpeech.QUEUE_ADD, null);
        //tts.speak(result, TextToSpeech.QUEUE_ADD, null,null);


        TranslateAPI translateAPI = new TranslateAPI(
                Language.AUTO_DETECT,   //Source Language
                Language.HINDI,         //Target Language
                result);           //Query Text

        translateAPI.setTranslateListener(new TranslateAPI.TranslateListener() {
            @Override
            public void onSuccess(String translatedText) {
                Log.d(TAG, "onSuccess: "+translatedText);
                tvLabel1.setMovementMethod(new ScrollingMovementMethod());
                tvLabel1.setText(translatedText);
                tts.speak("English caption", TextToSpeech.QUEUE_ADD, null);
                tts.speak(Text, TextToSpeech.QUEUE_ADD, null,null);
                tts.speak("Hindi Caption", TextToSpeech.QUEUE_ADD, null,null);
                tts.speak(translatedText, TextToSpeech.QUEUE_ADD, null,null);
                tts.speak("Text in the document", TextToSpeech.QUEUE_ADD, null,null);
                //tts.setSpeechRate(0.6f);
                tts.speak(recognizedText, TextToSpeech.QUEUE_ADD, null,null);
            }

            @Override
            public void onFailure(String ErrorText) {
                Log.d(TAG, "onFailure: "+ErrorText);
            }
        });


        TessBaseAPI tessTwo = new TessBaseAPI();
        tessTwo.init(Environment.getExternalStorageDirectory().toString(), "eng+hin");
        //tessTwo.init(Environment.getExternalStorageDirectory().toString(), "hin");
        tessTwo.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
        tessTwo.setImage(bmp);
        recognizedText = tessTwo.getUTF8Text();
        //recognizedText = recognizedText.replaceAll("[\\n\\t ]", "");
        tvLabel2.setMovementMethod(new ScrollingMovementMethod());
        tvLabel2.setText(recognizedText);
        //recognizedText = recognizedText.replaceAll("[\\n ]", "");
        tessTwo.end();
       // tts.speak(recognizedText, TextToSpeech.QUEUE_ADD, null,null);

        //if (bmp == null) {
          //  int color= Color.TRANSPARENT;
        //}
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int size = width * height;
        int[] pixels = new int[size];
        //Bitmap bitmap2 = bitmap.copy(Bitmap.Config.ARGB_4444, false);
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int color;
        int r = 0;
        int g = 0;
        int b = 0;
        int a;
        int count = 0;
        for (int i = 0; i < pixels.length; i++) {
            color = pixels[i];
            a = Color.alpha(color);
            if (a > 0) {
                r += Color.red(color);
                g += Color.green(color);
                b += Color.blue(color);
                count++;
            }
        }
        r = r / count;
        g /= count;
        b /= count;
        r = (r << 16) & 0x00FF0000;
        g = (g << 8) & 0x0000FF00;
        b = b & 0x000000FF;
        color = 0xFF000000 | r | g | b;
        String hexColor = Integer.toHexString(color).toUpperCase();
        //        //tvLabel.setText(hexColor);









        //tvLabel.setPadding(5,5,5,5);
        tvLabel.setBackgroundColor(Color.rgb(255, 255, 80));
        tvLabel1.setBackgroundColor(Color.rgb(255, 255, 80));
        tvLabel2.setBackgroundColor(Color.rgb(255, 255, 80));
        //Toast.makeText(getBaseContext(),recognizedText, Toast.LENGTH_LONG).show();
        //tts.speak(recognizedText, TextToSpeech.QUEUE_ADD, null,null);
        btnCamera.setEnabled(true);
        btnSelect.setEnabled(true);
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File("/sdcard/", "Captioner");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}