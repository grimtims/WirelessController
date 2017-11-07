package com.wsn.wirelesscontroller;

import android.content.pm.ActivityInfo;
import android.media.Image;
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
import android.widget.Toast;

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
    private boolean A,B,X,Y = false;

    private ImageView ivA,ivB,ivX,ivY;

    private float js_origin_X, js_origin_Y = 0;

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

        setHandlers();
        debug();

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
                                "\n  A: "+A+" B:"+B+" X:"+X+" Y:"+Y);

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
