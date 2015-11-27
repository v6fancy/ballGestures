package com.meizu.ballgestures;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class DrawView extends View {

    private String LOG_TAG = "ballgestures-Class";
    private float currentX = 300;
    private float currentY = 400;
    private int ballSize = 200;
    private Paint mPaint, mPaint2;
    public int WIDTH;
    public int HEIGHT;
    public static final double PI = 3.14159265;
    private boolean drawing = false;
    private ArrayList<Circle> circles;
    public boolean direct;  //direct = true,表示球从左边出去(serve)， false时，球从右边出去(client)

    //蓝牙通信相关
    /* 一些常量，代表服务器的名称 */
    public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";
    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";
    public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";
    public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";

    private BluetoothServerSocket mserverSocket = null;
    private ServerThread startServerThread = null;
    private clientThread clientConnectThread = null;
    private BluetoothSocket socket = null;
    private BluetoothDevice device = null;
    private readThread mreadThread = null;
    ;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    //开启客户端
    private class clientThread extends Thread {
        public void run() {
            while (socket == null) {
                try {
                    //创建一个Socket连接：只需要服务器在注册时的UUID号
                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    //连接
                    //启动接受数据
                    socket.connect();
                    Log.i(LOG_TAG, "connect success");

                    mreadThread = new readThread();
                    mreadThread.start();
                } catch (IOException e) {
                    Log.e("connect", "", e);
                    Log.i(LOG_TAG, "连接服务端异常！断开连接重新试一试。");
                }
            }
        }
    }

    ;

    //开启服务器
    private class ServerThread extends Thread {

        public void run() {
            Log.d(LOG_TAG, "server " + " wait cilent connect...");
            try {
				/* 创建一个蓝牙服务器
				 * 参数分别：服务器名称、UUID	 */

                mserverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM,
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                Log.d(LOG_TAG, "server " + " wait cilent connect...");

				/* 接受客户端的连接请求 */
                socket = mserverSocket.accept();
                Log.d(LOG_TAG, "server " + " accept success !");

                //启动接受数据
                mreadThread = new readThread();
                mreadThread.start();
            } catch (IOException e) {
                Log.d(LOG_TAG, "server " + " fail...");
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    /* 停止服务器 */
    private void shutdownServer() {
        new Thread() {
            public void run() {
                if (startServerThread != null) {
                    startServerThread.interrupt();
                    startServerThread = null;
                }
                if (mreadThread != null) {
                    mreadThread.interrupt();
                    mreadThread = null;
                }
                try {
                    if (socket != null) {
                        socket.close();
                        socket = null;
                    }
                    if (mserverSocket != null) {
                        mserverSocket.close();/* 关闭服务器 */
                        mserverSocket = null;
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "server " + " mserverSocket.close()", e);
                }
            }

            ;
        }.start();
    }

    /* 停止客户端连接 */
    private void shutdownClient() {
        new Thread() {
            public void run() {
                if (clientConnectThread != null) {
                    clientConnectThread.interrupt();
                    clientConnectThread = null;
                }
                if (mreadThread != null) {
                    mreadThread.interrupt();
                    mreadThread = null;
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    socket = null;
                }
            }

            ;
        }.start();
    }

    //发送数据
    private void sendMessageHandle(String msg) {
        if (socket == null) {
            Log.i(LOG_TAG, "没有连接");
            return;
        }
        try {
            OutputStream os = socket.getOutputStream();
            os.write(msg.getBytes());
            Log.i(LOG_TAG, "send data = " + msg.toString());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.i(LOG_TAG, "send fail");
        }
    }

    //读取数据
    private class readThread extends Thread {
        public void run() {
            Log.i(LOG_TAG, "readThread");
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream mmInStream = null;

            try {
                mmInStream = socket.getInputStream();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            while (true) {
                try {
                    // Read from the InputStream
                    if ((bytes = mmInStream.read(buffer)) > 0) {
                        byte[] buf_data = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf_data[i] = buffer[i];
                        }
                        String s = new String(buf_data);
                        Log.i(LOG_TAG, "receive data = " + s);
                        String spStr[] = s.split("/");
                        float x = Float.parseFloat(spStr[1]);
                        float y = Float.parseFloat(spStr[0]);
                        //接收数据，x坐标，y坐标，angle，以/分割开
                        circles.clear();
                        if(direct) {
                            Log.i(LOG_TAG, "I am client");
                            circles.add(new Circle(x - ballSize, y, ballSize, direct));
                        }
                        else {
                            Log.i(LOG_TAG, "I am server");
                            circles.add(new Circle(x + WIDTH, y, ballSize, direct));
                        }
                        Log.i(LOG_TAG, "receive data, x = " + x + "y = " + y);
                    }
                } catch (IOException e) {
                    try {
                        mmInStream.close();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    Log.i(LOG_TAG, "RECE FAIL");
                    break;
                }
            }
        }
    }


    public DrawView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub

        circles = new ArrayList<Circle>();
//        circles.add(new Circle(100, 100, 100, direct));
        if(MainActivity.serviceOrCilent== MainActivity.ServerOrCilent.CILENT)
        {
            Log.i(LOG_TAG, "It is client!!");
            direct = false;
            circles.add(new Circle());
            String address = MainActivity.BlueToothAddress;
            if(!address.equals("null"))
            {
                device = mBluetoothAdapter.getRemoteDevice(address);
                clientConnectThread = new clientThread();
                clientConnectThread.start();
                MainActivity.isOpen = true;
            }
            else
            {
                Log.i(LOG_TAG, "address is null !");
            }
        }
        else if(MainActivity.serviceOrCilent== MainActivity.ServerOrCilent.SERVICE)
        {
            Log.i(LOG_TAG, "It is service!!");
            direct = true;
            startServerThread = new ServerThread();
            startServerThread.start();
            MainActivity.isOpen = true;
        }

        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setAntiAlias(true);

        mPaint2 = new Paint();
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setColor(Color.WHITE);
        mPaint2.setAntiAlias(true);

        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        WIDTH = dm.widthPixels;
        HEIGHT = dm.heightPixels;

        //启动界面线程,开始自动更新界面
        drawing = true;
        new Thread(mRunnable).start();

    }

    private Runnable mRunnable = new Runnable() {
        //界面的主线程
        @Override
        public void run() {
            while (drawing) {
                try {
                    //更新球的位置信息
//                    update();
                    //通知系统更新界面,相当于调用了onDraw函数
                    postInvalidate();
                    //界面更新的频率,这里是每30ms更新一次界面
                    Thread.sleep(30);
                    //Log.e(TAG, "drawing");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    public DrawView(Context context, AttributeSet set) {
        // TODO Auto-generated constructor stub  
        super(context, set);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub  
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);

        for( Circle circle : circles)
        {
            canvas.drawCircle(circle.x, circle.y, circle.radius, mPaint);
        }

    }


    class Circle
    {
        float x=250;
        float y=270;
//        double angle= (new Random().nextFloat())*2*PI;;
        int speed=15;
        int radius=ballSize;
        boolean direct = false;
        public Circle() {
        }
        public Circle( float x, float y, int r, boolean direct )
        {
            this.x = x;
            this.y = y;
            radius = r;
//            this.angle = angle;
            this.direct = direct;
        }

        public void ballDisappear()
        {
            circles.clear();
        }

        //利用三角函数计算出球的新位置值,当与边界发生碰撞时,改变球的角度
        public void updateLocate()
        {


        }

    }



    public void spec_sleep(int micsec) {
        try {
            Thread.sleep(micsec);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //当view发生触屏事件时会调用此方法  
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub  

        currentX = event.getX() ;
        currentY = event.getY() ;
        circles.clear();
        circles.add(new Circle(currentX, currentY, ballSize, direct));
        if(direct == false && (currentX + ballSize >= WIDTH)) {
            Log.i(LOG_TAG, "send data : " + currentY + "/" + (currentX + ballSize - WIDTH));
            sendMessageHandle(currentY + "/" + (currentX + ballSize - WIDTH));
            spec_sleep(100);
        }

        if(direct == true && (currentX - ballSize <= 0)) {

            Log.i(LOG_TAG, "send data : " + currentY + "/" + currentX);
            sendMessageHandle(currentY + "/" + currentX);
            spec_sleep(100);
        }


        //通知当前组件重绘自己  
//        invalidate() ;
        return true;
    }



} 