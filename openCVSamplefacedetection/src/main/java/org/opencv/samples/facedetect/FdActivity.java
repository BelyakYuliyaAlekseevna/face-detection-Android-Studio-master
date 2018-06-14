package org.opencv.samples.facedetect;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
public class FdActivity extends AppCompatActivity implements CvCameraViewListener2 {

    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private static final Scalar    FACE_RECT_COLOR1     = new Scalar(0, 0, 255, 255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    private MenuItem               mItemFace50;
    private MenuItem               mItemFace40;
    private MenuItem               mItemFace30;
    private MenuItem               mItemFace20;
    private MenuItem               mItemType;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private File                   mCascadeFile1;
    private CascadeClassifier      mJavaDetector;
    private CascadeClassifier      mJavaDetector1;
    private DetectionBasedTracker  mNativeDetector;
    ArrayList<Integer> oneProduct_freshness = new ArrayList<Integer>();
    ArrayList<Integer> oneProduct_color = new ArrayList<Integer>();
    private Button                  barcodeBtn;

    private int 		            cnt_up_c          = 0;
    private int 		            cnt_down_c        = 0;

    private int 		            cnt_up_t          =0;
    private int 		            cnt_down_t        =0;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detectionBasedTracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.cascade_tide_type);

                        InputStream is1 = getResources().openRawResource(R.raw.cascade_color_tide);

                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);

                        mCascadeFile = new File(cascadeDir, "cascade_tide_type.xml");
                        mCascadeFile1 = new File(cascadeDir, "cascade_color_tide.xml");

                        FileOutputStream os = new FileOutputStream(mCascadeFile);
                        FileOutputStream os1 = new FileOutputStream(mCascadeFile1);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        while ((bytesRead = is1.read(buffer)) != -1) {
                            os1.write(buffer, 0, bytesRead);
                        }

                        is.close();
                        os.close();
                        is1.close();
                        os1.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        mJavaDetector1 = new CascadeClassifier(mCascadeFile1.getAbsolutePath());

                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.face_detect_surface_view);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        barcodeBtn = (Button)findViewById(R.id.button_barcode);
        barcodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                barcode();
            }
        });
    }

    public void  barcode(){
        Intent intent = new Intent(this, Barcode.class);
        startActivity(intent);
    }
 /*   @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }*/

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect tide = new MatOfRect();
        MatOfRect tide1 = new MatOfRect();


        int UP_Line = (mRgba.width()/2) - 50;
        int DOWN_Line = (mRgba.width()/2) + 50;
        int UP_Limit = (mRgba.width()/2) - 150;
        int DOWN_Limit = (mRgba.width()/2) + 150;
        int t3=(mRgba.height());
        Imgproc.line(mRgba,new Point(UP_Limit,t3), new Point(UP_Limit,0), new Scalar(0,255,0), 3);
        Imgproc.line(mRgba,new Point(DOWN_Limit,t3), new Point(DOWN_Limit,0), new Scalar(0,255,0), 3);

        Imgproc.line(mRgba,new Point(UP_Line,t3), new Point(UP_Line,0), new Scalar(0,0,255), 3);
        Imgproc.line(mRgba,new Point(DOWN_Line,t3), new Point(DOWN_Line,0), new Scalar(255,0,0), 3);


        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, tide, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, tide);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] tideArray = tide.toArray();
        for (int i = 0; i < tideArray.length; i++){
            Imgproc.rectangle(mRgba, tideArray[i].tl(), tideArray[i].br(), FACE_RECT_COLOR, 3);
            Imgproc.putText(mRgba, "Tide-freshness",new Point(tideArray[i].x,tideArray[i].y-3),Core.FONT_HERSHEY_PLAIN, 2.0 ,new  Scalar(0,255,255),2, Core.LINE_AA, false);
            int cx = tideArray[i].x + tideArray[i].width /2;
            oneProduct_freshness.add(cx);

            if (cx >= UP_Limit && cx <= DOWN_Limit) {

                Product product = new Product();

                if (product.going_UP(UP_Line, UP_Line, oneProduct_freshness)==true) {
                    cnt_up_t +=1;
                }
                else if(product.going_DOWN(DOWN_Line, DOWN_Line, oneProduct_freshness)==true) {
                    cnt_down_t +=1;
                }


                if (product.getState()== 1) {

                    if (product.getDir() == "down" && oneProduct_freshness.get(i) > DOWN_Limit){
                        product.setDone();
                    }

                    else if (product.getDir() == "up" && oneProduct_freshness.get(i) < UP_Limit) {
                        product.setDone();
                    }
                }


            }
        }


        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector1.detectMultiScale(mGray, tide1, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, tide1);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] tide1Array = tide1.toArray();
        for (int j = 0; j < tide1Array.length; j++){
            Imgproc.rectangle(mRgba, tide1Array[j].tl(), tide1Array[j].br(), FACE_RECT_COLOR1, 3);
            Imgproc.putText(mRgba, "Tide-color",new Point(tide1Array[j].x,tide1Array[j].y-3),Core.FONT_HERSHEY_PLAIN, 2.0 ,new  Scalar(255,255,0),2, Core.LINE_AA, false);
            int cx_c = tide1Array[j].x + tide1Array[j].width /2;
            oneProduct_color.add(cx_c);
            if (cx_c >= UP_Limit && cx_c <= DOWN_Limit) {

                Product product_c = new Product();

                if (product_c.going_UP(UP_Line, UP_Line, oneProduct_color)==true) {
                    cnt_up_c +=1;
                }
                else if(product_c.going_DOWN(DOWN_Line, DOWN_Line, oneProduct_color)==true) {
                    cnt_down_c +=1;
                }

                if (product_c.getState()== 1) {

                    if (product_c.getDir() == "down" && oneProduct_color.get(j) > DOWN_Limit){
                        product_c.setDone();
                    }

                    else if (product_c.getDir() == "up" && oneProduct_color.get(j) < UP_Limit) {
                        product_c.setDone();
                    }
                }
            }
        }

        return mRgba;
    }

  /*  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        mItemType   = menu.add(mDetectorName[mDetectorType]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50)
            setMinFaceSize(0.5f);
        else if (item == mItemFace40)
            setMinFaceSize(0.4f);
        else if (item == mItemFace30)
            setMinFaceSize(0.3f);
        else if (item == mItemFace20)
            setMinFaceSize(0.2f);
        else if (item == mItemType) {
            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
            item.setTitle(mDetectorName[tmpDetectorType]);
            setDetectorType(tmpDetectorType);
        }
        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector.stop();
            }
        }
    }*/
}
