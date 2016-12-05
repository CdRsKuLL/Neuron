package alpha2.uk.neuron;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;

import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.os.StrictMode;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.content.res.AssetManager;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


import com.ubtechinc.alpha2ctrlapp.network.action.ClientAuthorizeListener;
import com.ubtechinc.alpha2robot.Alpha2RobotApi;
import com.ubtechinc.alpha2robot.constant.AlphaContant;
import com.ubtechinc.alpha2serverlib.interfaces.AlphaActionClientListener;
import com.ubtechinc.alpha2serverlib.interfaces.IAlpha2ActionListListener;
import com.ubtechinc.alpha2serverlib.interfaces.IAlpha2CustomMessageListener;
import com.ubtechinc.alpha2serverlib.interfaces.IAlpha2RobotClientListener;
import com.ubtechinc.alpha2serverlib.interfaces.IAlpha2RobotTextUnderstandListener;
import com.ubtechinc.alpha2serverlib.interfaces.IAlpha2SpeechGrammarInitListener;
import com.ubtechinc.alpha2serverlib.interfaces.IAlpha2SpeechGrammarListener;
import com.ubtechinc.alpha2serverlib.util.Alpha2SpeechMainServiceUtil;
import com.ubtechinc.contant.CustomLanguage;
import com.ubtechinc.contant.LauguageType;
import com.ubtechinc.contant.StaticValue;
import com.ubtechinc.developer.DeveloperAppStaticValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class MainActivity extends Activity implements
        CvCameraViewListener2, SensorEventListener, IAlpha2RobotClientListener, Alpha2SpeechMainServiceUtil.ISpeechInitInterface,
        IAlpha2RobotTextUnderstandListener, IAlpha2SpeechGrammarInitListener , IAlpha2ActionListListener , AlphaActionClientListener  {

    private Alpha2RobotApi mRobot;
    private ExitBroadcast mExitBroadcast;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private int batterylevel;

    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 1700;
    public String alphaposition = "stand";
    public String lastalphaposition = "stand";
    public long alphapositioncount = 0;
    public Boolean alphapositionsay = false;
    public boolean speaking = false;
    private boolean isOneAngle=true;
    public String lastcommand = "";

    private ArrayList<String> mAlphaActionList = new ArrayList<String>();
    private ArrayList<String> mAlphaDanceList = new ArrayList<String>();
    private ArrayList<String> mAlphaStoryList = new ArrayList<String>();

    URL url;
    HttpURLConnection conn;
    private String mPackageName;

    private static final String TAG                     = "Main::Activity";
    private static final Scalar    FACE_RECT_COLOR         = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR           = 0;
    public static final int        NATIVE_DETECTOR         = 1;

    private MenuItem mItemType;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType           = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize       = 0.2f;
    private int                    mAbsoluteFaceSize       = 0;

    public double                  headx                    =115;
    public double                  heady                    =120;
    public double                  headxlast                =115;
    public double                  headylast                =120;
    public boolean                 headtracking             =false;
    public boolean                 fallenover               =false;

    public boolean                 bored                   =false;
    public int                     boredcounter            =0;

    private CameraBridgeViewBase   mOpenCvCameraView;

    //Some UI items
    public TextView textView                = null;

    private MediaPlayer player;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status){
            switch(status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV Loaded Successfully");
                    System.loadLibrary("detection_based_tracker");

                    try{
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();
                    }catch(IOException e){
                        e.printStackTrace();
                        Log.i(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    //public static final int CAMERA_ID_BACK  = 99;
                    //public static final int CAMERA_ID_FRONT = 98;
                    //mOpenCvCameraView.setCameraIndex(98);
                    //mOpenCvCameraView.setCameraIndex(1);
                    //mOpenCvCameraView.enableFpsMeter();
                    //mOpenCvCameraView.setMaxFrameSize(640,480);
                    mOpenCvCameraView.enableView();
                    headtracking = true;
                    setDetectorType(0);

                } break;
                default:
                {
                    super.onManagerConnected(status);
                }break;
            }//switch
        }//onManagerConnected
    };//BaseLoaderCallback

    private void initActionList(ArrayList<ArrayList<String>> list) {
        if(list != null) {
            for (ArrayList<String> item : list) {
                if (item.get(1) != null && item.get(2) != null) {
                    if("1".equals(item.get(1))) {
                        mAlphaActionList.add(item.get(2));
                    } else if("2".equals(item.get(1))) {
                        mAlphaDanceList.add(item.get(2));
                    } else if("3".equals(item.get(1))) {
                        mAlphaStoryList.add(item.get(2));
                    }
                }
            }
        }

    }

    public MainActivity(){
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "JAVA";
        mDetectorName[NATIVE_DETECTOR] = "NATIVE (tracking)";
    }

    private void initializeMediaPlayer(String WEBURL) {

        player.reset();

        try {
            player.setDataSource(WEBURL);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {

            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.i("Buffering", "" + percent);
            }
        });
        player.prepareAsync();

        player.setOnPreparedListener(new OnPreparedListener() {

            public void onPrepared(MediaPlayer mp) {
                player.start();
                player.setVolume((float) 0.4, (float) 0.4);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        Context context = getApplicationContext();
        init();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        this.registerReceiver(this.batteryInfoReceiver,	new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        player = new MediaPlayer();

    }

    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //int  health= intent.getIntExtra(BatteryManager.EXTRA_HEALTH,0);
            //int  icon_small= intent.getIntExtra(BatteryManager.EXTRA_ICON_SMALL,0);
            batterylevel= intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            //int  plugged= intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);
            //boolean  present= intent.getExtras().getBoolean(BatteryManager.EXTRA_PRESENT);
            //int  scale= intent.getIntExtra(BatteryManager.EXTRA_SCALE,0);
            //int  status= intent.getIntExtra(BatteryManager.EXTRA_STATUS,0);
            //String  technology= intent.getExtras().getString(BatteryManager.EXTRA_TECHNOLOGY);
            //int  temperature= intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
            //int  voltage= intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0);
        }
    };

    public void init() {

        Bundle bundle = getIntent().getExtras();
        Log.i("zdy", "bundle " + bundle);

        mPackageName = this.getPackageName();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DeveloperAppStaticValue.APP_EXIT);
        filter.addAction(mPackageName);
        filter.addAction(StaticValue.ALPHA_SPEECH_DIRECTION);
        filter.addAction(StaticValue.ALPHA_TTS_HINT);
        filter.addAction(DeveloperAppStaticValue.APP_ROBOT_UUID_INFO);
        filter.addAction(mPackageName + DeveloperAppStaticValue.APP_CONFIG);
        filter.addAction(mPackageName + DeveloperAppStaticValue.APP_CONFIG_SAVE);
        filter.addAction(mPackageName + DeveloperAppStaticValue.APP_BUTTON_EVENT);
        filter.addAction(mPackageName + DeveloperAppStaticValue.APP_BUTOON_EVENT_CLICK);
        mExitBroadcast = new ExitBroadcast();
        MainActivity.this.registerReceiver(mExitBroadcast, filter);
        String appkey = "222B998EDFA5FAD7FCE78678FB9F2521";
        mRobot = new Alpha2RobotApi(this, appkey,
                new ClientAuthorizeListener() {

                    @Override
                    public void onResult(int code, String info) {
                        // TODO Auto-generated method stub
                        Log.i("zdy", "code = " + code + " info= " + info);

                        mRobot.initSpeechApi(MainActivity.this,MainActivity.this);
                        mRobot.initActionApi(MainActivity.this);
                        mRobot.initChestSeiralApi();

                    }
                });

    }

    public void initOver() {
        // TODO Auto-generated method stub
        mRobot.speech_setVoiceName("xiaoyan");
        mRobot.speech_setRecognizedLanguage(LauguageType.LAU_ENGLISH);
        mRobot.speech_startRecognized("");
        mRobot.requestRobotUUID();
        Log.i("zdy", "Recognized initover");
        Calendar c = Calendar.getInstance();
        int currenthour = c.get(Calendar.HOUR_OF_DAY);
        if (currenthour < 12){
            say("good morning, powering up",false);
        } else if (currenthour < 17){
           say("good afternoon, powering up",false);
        } else {
            say("good evening, powering up",false);
        }
        CopyAssets();

    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            String nlutext = "";
            String text = (String) msg.obj;
            text = text.toLowerCase();
            bored = false;
            if (text.contains("nlu_result:")){
            String[] findtext = text.split(":");
                for (int x = 0; x < findtext.length; x++)
                {
                    if (findtext[x].contains("literal")){
                        if (findtext[x+1].length() > 3) {
                            if (findtext[x+1].contains("ext_map_time")){

                            } else {
                                nlutext = findtext[x + 1].replaceAll(",", "");
                                nlutext = nlutext.replaceAll("\n", "");
                                nlutext = nlutext.replaceAll("action", "");
                                nlutext = nlutext.trim();
                                text = "";
                            }
                        }
                    }
                }
            }

            if (text.contains("action_performance")){
                mRobot.action_PlayActionName("Happy");
                int number = new Random().nextInt(10);
                String actionName = String.format("ACT%d", number);
                // mRobot.action_PlayActionName(actionName);
            } else {
                // Speech part

                if (text.contains(("tracking"))){
                    if (text.contains("off")){
                        say("Turning head tracking off",true);

                        if(mOpenCvCameraView != null)
                            mOpenCvCameraView.disableView();
                        headtracking = false;

                    }
                    if (text.contains("on")){
                        say("Turning head tracking on",true);
                        if(!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, MainActivity.this, mLoaderCallback)){
                            headtracking = true;
                        }
                    }
                }

                if (text.contains("light") || text.contains("lights")){
                    if (text.contains("off")){
                        mRobot.action_PlayActionName("Raise head");
                        try {
                            url = new URL("http://192.168.1.239:1001/light1off");

                            conn = (HttpURLConnection) url.openConnection();
                            conn.setInstanceFollowRedirects(true);
                            conn.connect();
                            conn.getResponseCode();
                            say("okay, turning the lights off",false);
                        } catch (IOException e){
                            say("sorry, I can not turn the lights off at the moment. There is a communication error it seems.",false);
                        }
//set the output to true, indicating you are outputting(uploading) POST data
                        conn.disconnect();
                    }
                    if (text.contains("on")){
                        mRobot.action_PlayActionName("Raise head");
                        try {
                            url = new URL("http://192.168.1.239:1001/light1on");
                            conn = (HttpURLConnection) url.openConnection();
                            conn.setInstanceFollowRedirects(true);
                            conn.connect();
                            conn.getResponseCode();
                            say("okay, turning the light on",false);
                        } catch (IOException e){
                            say("sorry, I can not turn the light on at the moment. There is a communication error it seems.",false);
                        }
//set the output to true, indicating you are outputting(uploading) POST data

                        conn.disconnect();
                    }
                }

                Calendar c = Calendar.getInstance();
                Date date = new Date();

                if (text.equals("")){text = nlutext;}
                if (lastcommand.equals(text)){text = "";}
                lastcommand = text;

                switch (text) {
                    case "play radio":
                    case "play the radio":
                        say("I'll play the radio for you, please wait",true);
                    case "radio one":
                    case "play radio one":
                    case "play radio wall":
                        say ("playing radio one",true);
                        initializeMediaPlayer("http://bbcmedia.ic.llnwd.net/stream/bbcmedia_radio1_mf_p");
                        break;
                    case "play radio two":
                    case "radio two":
                        say ("playing radio two",true);
                        initializeMediaPlayer("http://bbcmedia.ic.llnwd.net/stream/bbcmedia_radio2_mf_p");
                        break;
                    case "play radio classical":
                    case "radio classical":
                        say ("playing radio classical",true);
                        initializeMediaPlayer("http://media-the.musicradio.com:80/ClassicFMMP3");
                        break;
                    case "play radio key 103":
                    case "play radio k103":
                    case "play key 103":
                    case "play k103":
                    case "key 103":
                    case "k103":
                        say ("playing key 1 o 3",true);
                        initializeMediaPlayer("http://icy-e-bl-07-boh.sharp-stream.com:8000/key.mp3");
                        break;
                    case "play radio x":
                    case "radio x":
                        say ("playing radio x",true);
                        initializeMediaPlayer("http://media-the.musicradio.com:80/RadioXManchester");
                        break;
                    case "turn the radio off":
                    case "radio off":
                    case "radio stop":
                    case "please turn the radio off":
                    case "stop the radio":
                    case "sound off":
                    case "alpha be quiet":
                    case "shut up":
                    case "be quiet":
                    case "stop":
                        if (player.isPlaying()) {
                            say("turning the radio off",true);
                            player.stop();
                        } else {
                            say("sorry, the radio isn't playing",true);
                        }

                        break;
                    case "dance for me":
                        say("dance, dance",true);
                        break;
                    case "can you see me":
                    case "turn tracking on":
                        say("Turning head tracking on",true);
                        if(!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, MainActivity.this, mLoaderCallback)){}
                        headtracking = true;
                        break;

                    case "close your eyes":
                    case "turn tracking off":
                        say("Turning head tracking off",true);
                        mRobot.action_PlayActionName("eyesblack");
                        if(mOpenCvCameraView != null)mOpenCvCameraView.disableView();
                        headtracking = false;
                        break;

                    case "how are you doing":
                    case "how are you":
                    case "how are you feeling":
                        say("I'm very well thank you.",false);
                        break;

                    case "whats your name":
                    case "what are you called":
                    case "what shall I call you":
                        say("My name is Alpha, I'm currently running version 1 of Neuron. You can see this application at website alpha 2 dot uk",true );

                    case "thank you":
                    case "okay thank you":
                    case "thanks":
                    case "okay thanks":
                        int number = new Random().nextInt(4);
                        if (number == 0){ say("You are welcome",false);}
                        if (number == 1){ say("I should think so too",false);}
                        if (number == 2){ say("no,, thank you",false);}
                        if (number == 3){ say("super duper, no problem",false);}
                        break;

                    case "what time is it":
                    case "whats the time":
                        int currenthour = c.get(Calendar.HOUR_OF_DAY);
                        int currentminute = c.get(Calendar.MINUTE);
                        if (currentminute < 10) {
                            say("The current time is " + currenthour + " " + "o" + currentminute, false);
                        } else {
                            say("The current time is " + currenthour + " " +  currentminute, false);
                        }
                        break;

                    case "what day is it":
                    case "whats the day":
                        say("Today is a " + android.text.format.DateFormat.format("EEEE, MMMM d", date), false);
                        break;

                    case "what year is it":
                    case "whats the year":
                        say("The year is  " + android.text.format.DateFormat.format("yyyy", date), false);
                        break;

                    case "what month is it":
                    case "whats the month":
                    case "what month are we in":
                    case "what month is this":
                        say("The month is  " + android.text.format.DateFormat.format("MMMM", date), false);
                        break;

                    case "what date is it":
                    case "what date is it today":
                    case "what date is this":
                    case "whats the date":
                    case "whats the date today":
                    case "whats todays date":
                        int currentdate = c.get(Calendar.DATE);
                        say("Today's date is " + android.text.format.DateFormat.format("EEEE, MMMM d, yyyy", date), false);
                        break;

                    case "how old are you":
                    case "when were you born":
                    case "whats your birthday":
                    case "whens your birthday":
                    case "what date is your birthday on":
                        say("I was first born on Indiegogo on the 31st December 2015. But I don't really age.", false );
                        break;

                    case "see you later alligator":
                        say("in a while crocodile",false);
                        break;

                    case "knock knock":
                        mRobot.action_PlayActionName("Shake head");
                        say("somebody is at the door,,,, you best go and answer it. Oh, and your not funny so don't try",false);
                        break;

                    case "whats your battery level":
                    case "how much battery do you have left":
                    case "How much battery have you got left":
                    case "how much charge do you have":
                    case "how much battery do you have":
                    case "battery level":
                    case "charge left":

                        say("I currently have " + batterylevel + " percent left",false);
                        break;

                    case "tell me another joke":
                        say("okay, another joke coming up.",true);
                    case "tell me a joke":
                    case "make me laugh":
                    case "do you know any jokes":
                    case "can you tell me a joke":
                    case "know any jokes":
                    case "tell me a good one":

                        int joke = new Random().nextInt(13);
                        switch (joke){
                            case 0:
                                say("How do astronomers organize a party?                 They planet.",false);
                                break;
                            case 1:
                                say("What type of sandals do frogs wear?                 Open-toad!",false);
                                break;
                            case 2:
                                say("Where do bees go to the toilet?                 at the BP station.",false);
                                break;
                            case 3:
                                say("What do you call a paralyzed goat?                 Billy Idle.",false);
                                break;
                            case 4:
                                say("I have a phobia of over engineered buildings. I have a complex complex complex.",false);
                                break;
                            case 5:
                                say("How do you make a tissue dance?                 Put a little boogie in it.",false);
                                break;
                            case 6:
                                say("Why did Adell cross the road?                      To sing    Hello from the other side.",true);
                                break;
                            case 7:
                                say("Why couldn't the leopard play hide and seek?                Because he was always spotted.",false);
                                break;
                            case 8:
                                say("A robot walks into a bar, orders a drink, and lays down some cash.                 Bartender says, Hey, we don't serve robots. And the robot says, Oh, but someday you will..",false);
                                break;
                            case 9:
                                say("How many robots does it take to screw in a light bulb?                 Three,,, one to hold the bulb, and two to turn the ladder!.",false);
                                break;
                            case 10:
                                say("How does a robot shave ?                 With a laser blade !",false);
                                break;
                            case 11:
                                say("What do you call a robot that always takes the longest route round ?                 R 2 detour.",false);
                                break;
                            case 12:
                                say("Do robots have sisters ?                 No, just transistors.",false);
                                break;

                        }
                        break;

                    default:
                       //say("I'm sorry, I didn't quite catch that?",false);
                        break;
                }

            }
            lastcommand = text;
        }

    };

    @Override
    public void onServerPlayEnd(boolean isEnd) {
        Log.d("zdy", "onServerPlayEnd");
        speaking = false;
        player.setVolume((float) 0.4, (float) 0.4);
    }

    public void say(String whattosay, Boolean waitforend){
        player.setVolume((float) 0.1, (float) 0.1);
        speaking = true;
        mRobot.speech_StartTTS(whattosay);
       if (waitforend){
          while (speaking){}
        }
    }

    @Override
    public void onAlpha2UnderStandError(int arg0) {
        // TODO Auto-generated method stub
    }
    @Override
    public void speechGrammarInitCallback(String arg0, int nErrorCode) {
        // TODO Auto-generated method stub

        Log.i("zdy", "speeh_startGrammar init over");


        mHandler.obtainMessage(0).sendToTarget();

        mRobot.speeh_startGrammar(new IAlpha2SpeechGrammarListener() {

            @Override
            public void onSpeechGrammarResult(int SpeechResultType,
                                              String strResult) {
                Log.i("zdy", "SpeechResultType =" + SpeechResultType);
                Log.i("zdy", "strResult =" + strResult);
                mHandler.obtainMessage(1, strResult)
                        .sendToTarget();

            }

            @Override
            public void onSpeechGrammarError(int nErrorCode) {
                // TODO Auto-generated method stub

            }

        });

    }
    @Override
    public void onAlpha2UnderStandTextResult(String arg0) {
        // TODO Auto-generated method stub
        Log.i("zdy", "nlp result" + arg0);
        if (arg0 != null && !arg0.equals("")) {
            int number = new Random().nextInt(10);
            String actionName = String.format("ACT%d", number);
            mRobot.action_PlayActionName(actionName);
            String newText = new String(arg0);

        }

    }
    @Override  //This receives the text from speech
    public void onServerCallBack(String text) {

        if (text != null && !text.equals("")) {//

      //      String newText = new String(text);
      //      mRobot.speech_understandText(newText, this);

         mHandler.obtainMessage(2, text)
                        .sendToTarget();
            }
     }



    public class ExitBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals(DeveloperAppStaticValue.APP_EXIT)) {
                Log.i("zdy", "speech_stopRecognized ");
                mRobot.releaseApi();
                mRobot = null;
                System.exit(0);
            } else if (intent.getAction().equals(mPackageName)) {

            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        switch(event.getScanCode()){
            case 66:
                if (headtracking){
                    if(mOpenCvCameraView != null) {
                        mOpenCvCameraView.disableView();
                        say("Turning off head tracking", true);
                        headtracking = false;
                    }
                } else {
                    if (player.isPlaying()) {
                        say("turning the radio off",true );
                        player.stop();
                    }
                }
                break;
            case 60: //front button
                break;
            case 61: //back button
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onGetActionList(ArrayList<ArrayList<String>> list) {
        Message msg = new Message();
        msg.what = 5;
        msg.obj = list;
        mHandler.sendMessage(msg);

    }

    @Override
    public void onActionStop(String strActionFileName) {
        // TODO Auto-generated method stub
        if (fallenover && headtracking){ //stops head tracking sending action commands whilst standing up
             fallenover = false;
             if(!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, MainActivity.this, mLoaderCallback)){}
        }
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();


            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    if (!alphaposition.equals("shake")){

                        mRobot.action_PlayActionName("Blink");
                        say("oww that really hurts. I'm going all dizzy.",false);
                        alphaposition = "shake";
                        lastUpdate +=6000;
                    }

                } else {
                    //Alpha position
                    if (y < -9.4 & (z < 2)){
                        alphaposition = "front";
                    }

                    if (y > 9.4 & (z < 2)){
                        alphaposition = "back";
                    }
                    if (x > 9.4 & (z < 1.5)){
                        alphaposition = "left";
                    }
                    if (x < -9.4 & (z < 1.5)){
                        alphaposition = "right";
                    }
                    if (z < -9.4) { //reset alpha position
                        alphaposition = "upsidedown";
                    }
                }

                if (lastalphaposition.equals(alphaposition)){
                    alphapositioncount +=1;
                } else {
                    alphapositioncount = 0;
                    lastalphaposition = alphaposition;
                    alphapositionsay = false;
                }

                if (alphapositioncount > 17){

                    if (!alphapositionsay){
                        bored = false;
                        switch(alphaposition){
                            case "back":
                                if(mOpenCvCameraView != null)mOpenCvCameraView.disableView();
                                fallenover = true;
                                say("Why am I lying down?",true);
                                say("give me a minute,,,, I'll get up",true);
                                mRobot.action_PlayActionName("BackStand");
                                break;
                            case "front":
                                if(mOpenCvCameraView != null)mOpenCvCameraView.disableView();
                                fallenover = true;
                                say("Hey, I can not see,, what's going on",true);
                                say("wait a minute,,,, I'll try and get up",true);
                                mRobot.action_PlayActionName("FrontStand");
                                break;
                            case "upsidedown":
                                if(mOpenCvCameraView != null)mOpenCvCameraView.disableView();
                                say("please put me down. are you stupid? this is not how you hold me.",false);
                            case "":
                                break;
                            case "shake":

                                break;

                            default:
                                say("I'm on my " + alphaposition,false);
                                break;
                        }

                        alphapositionsay = true;
                    }
                    alphapositionsay = true;
                } else {
                    alphapositionsay = false;
                }

                if (x > -2 & x < 2 & y > -2 & y < 2 ){ //reset alpha position
                    if (z > 8){
                        alphaposition = "";
                        alphapositioncount= 0;
                        alphapositionsay = false;
                    }

                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();

        senSensorManager.unregisterListener(this);

        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

    }
    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        if(!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback)){
            Log.i(TAG, "OpenCVLoader Failed on Resume");
        }

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (mExitBroadcast != null) {
            this.unregisterReceiver(mExitBroadcast);
            mExitBroadcast = null;
        }
        /**
         * Before destroy, stop TTS and action.
         */
        if (mRobot != null) {
            mRobot.speech_StopTTS();
            mRobot.action_StopAction();
        }

        if (mRobot != null) {
            mRobot.releaseApi();
            mRobot = null;
        }
        Log.i("zdy", "onDestroy ");
        mOpenCvCameraView.disableView();

    }
    public void onCameraViewStarted(int width, int height){
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped(){
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();


        //Core.transpose(mRgba, mGray);
        //Camera Upside down Fix...
        //Core.flip(mRgba, mGray, -1);

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();


        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        if (facesArray.length > 0) { // location of center of square
            headx = facesArray[0].x + (facesArray[0].width / 2);
            heady = facesArray[0].y + (facesArray[0].height / 2);

            //headx = headx * 2;
            //heady = heady * 1.5;

            short time = 250;

            if (headx > 640){ // look left
                if (headx > 700){headxlast +=1;}
                if (headx > 800){headxlast +=2;}
                if (headx > 900){headxlast +=3;}
                if (headx > 1000){headxlast +=4;}
            } else {  //look right
                if (headx < 580){headxlast -=1;}
                if (headx < 450){headxlast -=2;}
                if (headx < 350){headxlast -=3;}
                if (headx < 250){headxlast -=4;}
            }

            // I've made Alpha look down slightly to improve voice commands
            if (heady > 300){ // look down
                if (heady > 350){headylast +=1;}
                if (heady > 475){headylast +=1;}
                if (heady > 580){headylast +=1;}
                if (heady > 650){headylast +=2;}
            } else { // look up
                if (heady < 250){headylast -=1;}
                if (heady < 220){headylast -=1;}
                if (heady < 180){headylast -=1;}
                if (heady < 100){headylast -=2;}
            }

            if (headxlast > 170){headxlast  = 170;}
            if (headxlast < 70){headxlast  = 70;}
            if (headylast > 160){headylast = 160;}
            if (headylast < 100){headylast = 100;}

            mRobot.chest_SendOneFreeAngle((byte) 19, (int) headxlast, time);
            mRobot.chest_SendOneFreeAngle((byte) 20, (int) headylast, time);
            mRobot.action_PlayActionName("eyesblue");

            if (!bored){
                bored = true;
                boredcounter = 0;
            } else {
                boredcounter +=1;
            }
            if (boredcounter > 50){
                int number = new Random().nextInt(7);
                if (number == 0){ // lean left
                    mRobot.chest_SendOneFreeAngle((byte) 7, 125, time);
                    mRobot.chest_SendOneFreeAngle((byte) 12, 125, time);
                    mRobot.chest_SendOneFreeAngle((byte) 11, 125, time);
                    mRobot.chest_SendOneFreeAngle((byte) 16, 125, time);
                }
                if (number == 1){ //stand up
                    mRobot.chest_SendOneFreeAngle((byte) 7, 120, time);
                    mRobot.chest_SendOneFreeAngle((byte) 12, 120, time);
                    mRobot.chest_SendOneFreeAngle((byte) 11, 120, time);
                    mRobot.chest_SendOneFreeAngle((byte) 16, 120, time);
                }
                if (number == 2){ //lean right
                    mRobot.chest_SendOneFreeAngle((byte) 7, 115, time);
                    mRobot.chest_SendOneFreeAngle((byte) 12, 115, time);
                    mRobot.chest_SendOneFreeAngle((byte) 11, 115, time);
                    mRobot.chest_SendOneFreeAngle((byte) 16, 115, time);
                }
                if (number == 3){ //arm left
                    mRobot.chest_SendOneFreeAngle((byte) 3, 120, time);
                }
                if (number == 4){ //arm left
                    mRobot.chest_SendOneFreeAngle((byte) 3, 130, time);
                }
                if (number == 5){ //arm right
                    mRobot.chest_SendOneFreeAngle((byte) 6, 120, time);
                }
                if (number == 6){ //arm right
                    mRobot.chest_SendOneFreeAngle((byte) 6, 110, time);
                }
                bored = false;
            }

        }

        return mRgba;
    }

    private void setMinFaceSize(float faceSize){
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
    }
    private void CopyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("actions");
        } catch (IOException e) {
            Log.e("tag", e.getMessage());
        }

        for(String filename : files) {
            System.out.println("File name => "+filename);
            InputStream in = null;
            OutputStream out = null;
            try {
                //File directory = new File(Environment.getExternalStorageDirectory().toString() +"/actions/neuron/");
                //directory.mkdirs();
                in = assetManager.open("actions/"+filename);
                out = new FileOutputStream(Environment.getExternalStorageDirectory().toString() +"/actions/" + filename);
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch(Exception e) {
                Log.e("tag", e.getMessage());
            }
        }
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

}
