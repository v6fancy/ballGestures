package com.meizu.ballgestures;


import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;


public class BallCross extends Activity {

    private DrawView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = new DrawView(this);
        setContentView(view);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //为避免在程序退出后线程仍在进行,造成不必要的系统资源浪费,在Activity退出是时候,主动将线程停止
    @Override
    public void onDestroy()
    {
//        mScreen.stopDrawing();
        MainActivity.isOpen = false;
        super.onDestroy();
    }

//    @Override
//    public void onResume()
//    {
//        mScreen = new TheScreenView(this);
//        setContentView(mScreen);
//        super.onResume();
//    }


}
