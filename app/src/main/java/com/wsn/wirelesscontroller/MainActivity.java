package com.wsn.wirelesscontroller;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
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

    private String blueText = "null";

    private ImageView ivA,ivB,ivX,ivY,ivBlue;

    private float js_origin_X, js_origin_Y = 0;


    //Bluetooth variables
    BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 10;
    Context context;
    MyBluetoothService mBluetoothConnection;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    BluetoothDevice mBTDevice;


    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                    startConnection();
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };


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
        ivBlue = (ImageView)findViewById(R.id.bluetooth);

        setHandlers();
        debug();


        //bluetooth stuff
        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    private void setHandlers(){

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

                        Log.d(TAG, "theta: "+Math.toDegrees(theta));

                        ivJoyStick.animate().x(pos_x)
                                            .y(pos_y)
                                            .setDuration(0)
                                            .start();

                        input_handler(theta);

                        break;
                    default:
                        return false;
                }
                debug();
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
                        break;
                    default:
                        return false;
                }
                debug();
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
                        byte[] bytes = "x".toString().getBytes(Charset.defaultCharset());
                        mBluetoothConnection  = new MyBluetoothService(context);
                        mBluetoothConnection.write(bytes);
                        break;
                    default:
                        return false;
                }
                debug();
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
                        break;
                    default:
                        return false;
                }
                debug();
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
                        break;
                    default:
                        return false;
                }
                debug();
                return true;
            }
        });

        ivBlue.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (mBluetoothAdapter == null) {
                    blueText = "no bluetooth";
                }

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_UP:
                        blue = false;
                        blueText = "no press";
                        break;
                    case MotionEvent.ACTION_DOWN:
                        blue = true;
                        if (!mBluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                            blueText = "press";
                            Intent discoverableIntent =
                                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                            startActivity(discoverableIntent);


                        }
                        break;
                    default:
                        return false;
                }
                debug();
                return true;
            }
        });
    }

    private void input_handler(double theta){
        right = false;
        up = false;
        left = false;
        down = false;

        theta = Math.toDegrees(theta);

        if (theta >= 22.5 && theta < 67.5){
            right = true;
            up = true;
        }
        else if (theta >= 67.5 && theta < 112.5){
            up = true;
        }
        else if (theta >= 112.5 && theta < 157.5){
            up = true;
            left = true;
        }
        else if (theta >= 157.5 && theta < 202.5){
            left = true;
        }
        else if (theta >= 202.5 && theta < 247.5){
            left = true;
            down = true;
        }
        else if (theta >= 247.5 && theta < 292.5){
            down = true;
        }
        else if (theta >= 292.5 && theta < 337.5){
            down = true;
            right = true;
        }
        else {
            right = true;
        }

    }

    private void debug(){

//        Log.d(TAG,"X: "+pos_x+"  Y: "+pos_y);
        tvJoyStickDebug.setText("  X: "+(int)pos_x+
                                "  Y: "+(int)pos_y+
                                "\n  R: "+right+"  L: "+left+"  U: "+up+"  D: "+down+
                                "\n  A: "+A+" B:"+B+" X:"+X+" Y:"+Y+
                                "\n bluetooth:"+blue+" text:"+blueText);

    }


    //needed for bluetooth connection
    public void startConnection(){
        Log.d(TAG, "The things.");
        startBTConnection(mBTDevice,MY_UUID_INSECURE);
    }

    /**
     * starting chat service method
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");
        mBluetoothConnection  = new MyBluetoothService(context);
        mBluetoothConnection.startClient(device,uuid);
    }



    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }



}
