package com.wsn.wirelesscontroller;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.bluetooth.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.*;
import java.util.UUID;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.widget.Toast;

import java.nio.charset.Charset;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN";

    private TextView tvJoyStickDebug;
    // large circle of joystick
    private ImageView ivJoytickBase;
    // small circle of joystick
    private ImageView ivJoyStick;
    // joystick area (left side)
    private RelativeLayout rlJoystickArea;

    private float pos_x, pos_y = 0;
    private boolean right, left, up, down = false;
    private boolean A,B,X,Y,blue = false;
    private boolean start, select = false;

//    private String blueText = "null";

    private ImageView ivA,ivB,ivX,ivY,ivBlue;
    private Button btnBlue;
    private Button btnConnect;
    private Button btnStart, btnSelect;
    private RadioButton     ledConnected;

    private Vibrator mVib; //heh

    private boolean isConnected = false;

    private float js_origin_X, js_origin_Y = 0;



    private SendInputThread mSendInputThread;


    //Bluetooth variables
//    BluetoothAdapter mBluetoothAdapter;
//    private static final int REQUEST_ENABLE_BT = 10;
//    Context context;
//    MyBluetoothService mBluetoothConnection;
//    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
//    BluetoothDevice mBTDevice;


    Bluetooth mBluetooth;




//    /**
//     * Broadcast Receiver that detects bond state changes (Pairing status changes)
//     */
//    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//
//            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
//                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                //3 cases:
//                //case1: bonded already
//                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
//                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
//                    //inside BroadcastReceiver4
//                    mBTDevice = mDevice;
//                    startConnection();
//                }
//                //case2: creating a bone
//                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
//                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
//                }
//                //case3: breaking a bond
//                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
//                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
//                }
//            }
//        }
//    };


    /**
     * each bit of this short number represents an action on the gamepad
     * either 0 (off) or 1 (on)
     * [ 0000 00SS XYBA UDLR ]
     *
     * example:
     *  - pressing the A and X button, along
     *    with moving the joystick down results in
     *
     *      [ 0000 0000 1001 0100 ] Dec: 148  Hex: 94
     *
     *  - this 16 bit number will then be sent over to the retropie as a hex value
     *  - there is space to add other things aswell, such as player number, gyro data etc.
     *
     */
    private short encoded_input = 0;

    //Bit positions in the input_array

    private final byte SELECT_BIT_POS   = 9;
    private final byte START_BIT_POS    = 8;
    private final byte X_BIT_POS        = 7;
    private final byte Y_BIT_POS        = 6;
    private final byte B_BIT_POS        = 5;
    private final byte A_BIT_POS        = 4;
    private final byte UP_BIT_POS       = 3;
    private final byte DOWN_BIT_POS     = 2;
    private final byte LEFT_BIT_POS     = 1;
    private final byte RIGHT_BIT_POS    = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(   WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        tvJoyStickDebug = (TextView)this.findViewById(R.id.txt_joystick_debug);
        ivJoytickBase = (ImageView)this.findViewById(R.id.joystick_base);
        ivJoyStick = (ImageView)this.findViewById(R.id.joystick);
        rlJoystickArea = (RelativeLayout)this.findViewById(R.id.joystick_area);

        ivA = (ImageView)findViewById(R.id.btn_A);
        ivB = (ImageView)findViewById(R.id.btn_B);
        ivX = (ImageView)findViewById(R.id.btn_X);
        ivY = (ImageView)findViewById(R.id.btn_Y);
        btnBlue = (Button)findViewById(R.id.bluetooth);
        btnConnect = (Button)findViewById(R.id.bt_discover);
        btnStart = (Button)findViewById(R.id.btn_start);
        btnSelect = (Button)findViewById(R.id.btn_select);
        ledConnected = (RadioButton)findViewById(R.id.led_connected);

        mVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        setHandlers();
        debug();


        // need permission to discover bluetooth devices for android 23 and greater
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    ((TextView) new AlertDialog.Builder(this)
                            .setTitle("Runtime Permissions up ahead")
                            .setMessage(Html.fromHtml("<p>To find nearby bluetooth devices please click \"Allow\" on the runtime permissions popup.</p>" +
                                    "<p>For more info see <a href=\"http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id\">here</a>.</p>"))
                            .setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                                1);
                                    }
                                }
                            })
                            .show()
                            .findViewById(android.R.id.message))
                            .setMovementMethod(LinkMovementMethod.getInstance());       // Make the link clickable. Needs to be called after show(), in order to generate hyperlinks
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    break;
            }
        }


        //bluetooth stuff
        //Broadcasts when bond state changes (ie:pairing)
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
//        registerReceiver(mBroadcastReceiver, filter);
//
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mBluetooth = new Bluetooth(this, new bluetoothInterface() {
            @Override
            public void connected() {
                ledConnected.setChecked(true);
                isConnected = true;
            }

            @Override
            public void disconnected() {
                ledConnected.setChecked(false);
                isConnected = false;
            }
        });

        if(mBluetooth.isEnabled()){
            btnBlue.setText("Turn off");
        } else {
            btnBlue.setText("Turn on");
        }

        startSendingInputThread();

    }

    private void setHandlers(){

        // Handle joystick move it
        ivJoytickBase.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_UP:
                        // send joystick back to origin
                        pos_x = js_origin_X;
                        pos_y = js_origin_Y;

                        ivJoyStick.animate().x(js_origin_X)
                                            .y(js_origin_Y)
                                            .setDuration(0)
                                            .start();

                        Log.d(TAG, "origin  =  "+(js_origin_X+ivJoyStick.getWidth()/2)+" : "+(js_origin_Y+ivJoyStick.getHeight()/2));
                        // reset input
                        dpad_handler(0, 0);
                        break;

                    case MotionEvent.ACTION_DOWN:
                        // user has pressed on the screen, get origin for action up
                        //TODO should change this
                        if (js_origin_X == 0 && js_origin_Y == 0) {
                            js_origin_X = ivJoyStick.getX();
                            js_origin_Y = ivJoyStick.getY();
                            Log.d(TAG,"ox : "+js_origin_X+"  oy : "+js_origin_Y);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // user is moving joystick around (shift image to position)
                        pos_x = motionEvent.getX() - ivJoyStick.getWidth()/2;
                        pos_y = motionEvent.getY() - ivJoyStick.getHeight()/2;

                        // distance from joystick origin
                        double dx = pos_x - js_origin_X;
                        double dy = pos_y - js_origin_Y;

                        double theta;
                        double mag = Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2));

                        // find the angle relative to origin
                        if (dx >= 0 && dy < 0){ // quadrant 1
                            theta = Math.atan(Math.abs(dy/dx));
                        }
                        else if (dx < 0 && dy < 0){ //quadrant 2
                            theta = (Math.PI - Math.atan(Math.abs(dy/dx)));
                        }
                        else if (dx <0 && dy > 0){ // quadrant 3
                            theta = (Math.PI + Math.atan(Math.abs(dy/dx)));
                        }
                        else{ // quadrant 4
                            theta = (2*Math.PI - Math.atan(Math.abs(dy/dx)));
                        }

                        Log.d(TAG, "theta: "+Math.toDegrees(theta)+"   mag: "+mag);

                        ivJoyStick.animate().x(pos_x)
                                            .y(pos_y)
                                            .setDuration(0)
                                            .start();

                        dpad_handler(theta, mag);

                        break;
                    default:
                        return false;
                }
                return true;

            }
        });

        ivA.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_UP:
                        A = false;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        A = true;
                        mVib.vibrate(25);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });

        ivX.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_UP:
                        X = false;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        X = true;
                        mVib.vibrate(25);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });

        ivY.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_UP:
                        Y = false;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        Y = true;
                        mVib.vibrate(25);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });

        ivB.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_UP:
                        B = false;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        B = true;
                        mVib.vibrate(25);
                        break;
                    default:
                        return false;
                }
                return true;
            }

        });

//        btnBlue.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//
//                if (mBluetoothAdapter == null) {
//                    blueText = "no bt adapter";
//                    return false;
//                }
//
//                switch(motionEvent.getAction()){
//                    case MotionEvent.ACTION_UP:
//                        blue = false;
//                        blueText = "no press";
//                        break;
//                    case MotionEvent.ACTION_DOWN:
//                        blue = true;
//                        if (!mBluetoothAdapter.isEnabled()) {
//                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//                            blueText = "press";
//                            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//                            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//                            startActivity(discoverableIntent);
//                        } else {
//                            mBluetoothAdapter.disable();
//                        }
//                        break;
//                    default:
//                        return false;
//                }
//                debug();
//                return true;
//            }
//
//        });

//        btnBlue.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//
//                switch(motionEvent.getAction()){
//                    case MotionEvent.ACTION_UP:
//                        blue = false;
//                        break;
//                    case MotionEvent.ACTION_DOWN:
//                        blue = true;
//                        if(!mBluetooth.isEnabled()) {
//                            mBluetooth.enable();
//                            btnBlue.setText("Turn off");
//                        } else {
//                            mBluetooth.disable();
//                            btnBlue.setText("Turn on");
//                        }
//                        break;
//                    default:
//                        return false;
//                }
////                debug();
//                return true;
//            }
//
//        });
        btnStart.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_UP:
                        start = false;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        start = true;
                        mVib.vibrate(25);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });

        btnSelect.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_UP:
                        select = false;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        select = true;
                        mVib.vibrate(25);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });

        btnBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mBluetooth.isEnabled()) {
                    mBluetooth.enable();
                    btnBlue.setText("Turn off");
                } else {
                    mBluetooth.disable();
                    btnBlue.setText("Turn on");
                }
            }
        });

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnected) {
                    btnConnect.setText("Disconnect");
                    mBluetooth.findRetroPie();
                }else{
                    btnConnect.setText("Connect");
                    mBluetooth.closeConnection();
                }
            }
        });

    }

    /**
     * specifies the direction based on the angle of the joystick
     * @param theta
     */
    private void dpad_handler(double theta, double mag){

        right = false;
        up = false;
        left = false;
        down = false;

        theta = Math.toDegrees(theta);

        // don't want any input if joystick in center
        if( mag >= 50) {
            if (theta >= 22.5 && theta < 67.5) {
                right = true;
                up = true;
            } else if (theta >= 67.5 && theta < 112.5) {
                up = true;
            } else if (theta >= 112.5 && theta < 157.5) {
                up = true;
                left = true;
            } else if (theta >= 157.5 && theta < 202.5) {
                left = true;
            } else if (theta >= 202.5 && theta < 247.5) {
                left = true;
                down = true;
            } else if (theta >= 247.5 && theta < 292.5) {
                down = true;
            } else if (theta >= 292.5 && theta < 337.5) {
                down = true;
                right = true;
            } else {
                right = true;
            }
        }
    }

    /**
     * packs the button and joysticks input into two byte long array
     */
    private void encode_input() {
        encoded_input = 0;
        encoded_input = (short) (encoded_input | ((right ? 1 : 0)   << RIGHT_BIT_POS));
        encoded_input = (short) (encoded_input | ((left ? 1 : 0)    << LEFT_BIT_POS));
        encoded_input = (short) (encoded_input | ((down ? 1 : 0)    << DOWN_BIT_POS));
        encoded_input = (short) (encoded_input | ((up ? 1 : 0)      << UP_BIT_POS));
        encoded_input = (short) (encoded_input | ((A ? 1 : 0)       << A_BIT_POS));
        encoded_input = (short) (encoded_input | ((B ? 1 : 0)       << B_BIT_POS));
        encoded_input = (short) (encoded_input | ((Y ? 1 : 0)       << Y_BIT_POS));
        encoded_input = (short) (encoded_input | ((X ? 1 : 0)       << X_BIT_POS));
        encoded_input = (short) (encoded_input | ((start ? 1 : 0)   << START_BIT_POS));
        encoded_input = (short) (encoded_input | ((select ? 1 : 0)  << SELECT_BIT_POS));
    }

    private void debug(){

        encode_input();

//        Log.d(TAG,"X: "+pos_x+"  Y: "+pos_y);
        tvJoyStickDebug.setText("  X: "+(int)pos_x+
                                "  Y: "+(int)pos_y+
                                "\n  R: "+right+"  L: "+left+"  U: "+up+"  D: "+down+
                                "\n  A: "+A+" B:"+B+" X:"+X+" Y:"+Y+
                                "\n bt:"+blue+ " bin: "+String.format("%016d", Integer.parseInt(Integer.toBinaryString(encoded_input)))+" hex:"+Integer.toHexString(encoded_input));

    }


//    //needed for bluetooth connection
//    public void startConnection(){
//        Log.d(TAG, "The things.");
//        startBTConnection(mBTDevice,MY_UUID_INSECURE);
//    }
//
//    /**
//     * starting chat service method
//     */
//    public void startBTConnection(BluetoothDevice device, UUID uuid){
//        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");
//        mBluetoothConnection  = new MyBluetoothService(context);
//        mBluetoothConnection.startClient(device,uuid);
//    }

    private void startSendingInputThread(){
        if(mSendInputThread != null){
            mSendInputThread.cancel();
        }
        mSendInputThread = new SendInputThread();
        mSendInputThread.start();
    }

    private class SendInputThread extends Thread {

        private boolean isCancelled = false;

        public SendInputThread() {
            Log.d(TAG, "SendInputThread starting.");
        }

        public void run(){

            // write input_array every 50 ms to retropie
            while (!isCancelled) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        debug();
                        if(isConnected){
//                            mBluetooth.write(String.format("%016d", Integer.parseInt(Integer.toBinaryString(encoded_input))));
                            // write as hex string to reduce size of message
                            // add "_" to end of string to tell retropie that we are finished transmitting input
                            mBluetooth.write(Integer.toHexString(encoded_input)+"_");
                        }
                    }
                });
                try {
                    Thread.sleep(50);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }

        public void cancel(){
            isCancelled = true;
        }

    }



    @Override
    protected void onDestroy(){
//        unregisterReceiver(mBroadcastReceiver);
        if(mSendInputThread != null){
            mSendInputThread.cancel();
            mSendInputThread = null;
        }
        mBluetooth.release();
        super.onDestroy();

    }



}
