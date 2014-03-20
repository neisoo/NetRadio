package com.zhongyuanhuan.blind.netradio;

import android.os.Bundle;
import android.os.Vibrator;
import android.app.Activity;
import android.app.Service;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;


public class MainActivity extends Activity {

	private static final String TAG = "BlindNetRadio";
	private MediaPlayer mMediaPlayer;
	private ShakeDetectActivity mShakeDetect;
	private int mShakeCount = 0;
	private TextView mTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!LibsChecker.checkVitamioLibs(this))
			return;
		
		//setContentView(R.layout.activity_main);
		mTextView = new TextView(this);
		setContentView(mTextView);
		
		mShakeDetect = new ShakeDetectActivity(this);
		mShakeDetect.addListener(
        new ShakeDetectActivityListener() {
            @Override
            public void shakeDetected(int x_pos, int x_neg, int y_pos, int y_neg, int z_pos, int z_neg) {
                MainActivity.this.triggerShakeDetected(x_pos, x_neg, y_pos, y_neg, z_pos, z_neg);
            }
        });
		
		//playAudio("mms://ting.mop.com/mopradio");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}

	}

	@Override
    protected void onResume() {
        super.onResume();
        mShakeDetect.onResume();
    }

    @Override
    protected void onPause() {
    	mShakeDetect.onPause();
        super.onPause();
    }

    public void triggerShakeDetected(int x_pos, int x_neg, int y_pos, int y_neg, int z_pos, int z_neg) {
        // do something!
    	mShakeCount++;
    	String msg = "Shake:" + mShakeCount;
    	int dx = x_pos + x_neg;
    	int dy = y_pos + y_neg;
    	int dz = z_pos + z_neg;
    	if (dx > dy && dx > dz) {
    		msg += " X";
    	}
    	else if (dy > dx && dy > dz) { 
    		msg += " Y";
    	}
    	else if (dz > dx && dz > dy) { 
    		msg += " Z";
    	}
    	
    	mTextView.setText(msg);
    	Vibrator vib = (Vibrator) MainActivity.this.getSystemService(Service.VIBRATOR_SERVICE);
        vib.vibrate(500);
    }

	private void playAudio(String path) {
		try {
			mMediaPlayer = new MediaPlayer(this);
			mMediaPlayer.setDataSource(path);
			mMediaPlayer.prepare();
			mMediaPlayer.start();
		} catch (Exception e) {
			Log.e(TAG, "error: " + e.getMessage(), e);
		}
	}

}
