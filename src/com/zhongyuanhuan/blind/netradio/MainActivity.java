package com.zhongyuanhuan.blind.netradio;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.app.Activity;
import android.app.Service;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;

public class MainActivity extends Activity {

	private static final String TAG = "BlindNetRadio";
	private MediaPlayer mMediaPlayer;
	private ShakeDetectActivity mShakeDetect;
	private int mShakeCount = 0;
	private TextView mTextView;
	private String mMenuDescPath = "";
	private String mMenuSelectWaitPath = "";
	private String mMenuSelectStartPath = "";
	private String mMenuSelectDonePath = "";
	private String mDescription = "";
	private String mMediaPath = Environment.getExternalStorageDirectory().getPath() + "/netradio";
	private Thread mMenuThread = null;
	private Boolean mMenuRun = true;
	Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Check media library.
		if (!LibsChecker.checkVitamioLibs(this))
			return;

		// setContentView(R.layout.activity_main);
		mTextView = new TextView(this);
		setContentView(mTextView);
		
		// Description.
		mMenuDescPath = mMediaPath + "/menu_descript.ogg";
		mDescription = "Description the menu.";
		mMenuSelectWaitPath = mMediaPath + "/menu_select_wait.ogg";
		mMenuSelectStartPath = mMediaPath + "/menu_select_start.ogg";
		mMenuSelectDonePath = mMediaPath + "/menu_select_done.ogg";
		
		// Build menu.
		mMenu.clear();
		MenuItem itemNetRadio = new MenuItem(mMediaPath + "/menu_1_1_netradio.ogg", "Net Radio", "NetRadioActivity");		
		mMenu.add(itemNetRadio);
		MenuItem itemMusic = new MenuItem(mMediaPath + "/menu_1_2_music.ogg", "Playback Muisc", "PlaybackMusicActivity");		
		mMenu.add(itemMusic);
		MenuItem itemNews = new MenuItem(mMediaPath + "/menu_1_3_news.ogg", "Playback News", "PlaybackNewsActivity");		
		mMenu.add(itemNews);
		MenuItem itemPhone = new MenuItem(mMediaPath + "/menu_1_4_phone.ogg", "Phone Call", "PhoneCallActivity");		
		mMenu.add(itemPhone);
		MenuItem itemSMS = new MenuItem(mMediaPath + "/menu_1_5_sms.ogg", "Playback SMS", "PlaybackSMSActivity");		
		mMenu.add(itemSMS);
		MenuItem itemTime = new MenuItem(mMediaPath + "/menu_1_6_time.ogg", "Time and wether", "TimeWetherActivity");		
		mMenu.add(itemTime);
		MenuItem itemVolume = new MenuItem(mMediaPath + "/menu_1_7_volume.ogg", "Adjust Volume", "AdjustVolumeActivity");		
		mMenu.add(itemVolume);
		MenuItem itemBack = new MenuItem(mMediaPath + "/menu_1_8_return.ogg", "Go back", "Back");		
		mMenu.add(itemBack);
		MenuItem itemStop = new MenuItem(mMediaPath + "/menu_1_9_off.ogg", "Stop", "Stop");		
		mMenu.add(itemStop);

		// Handler
		mHandler = new MainAcitvityHandler();

		// Shake detect.
		mShakeDetect = new ShakeDetectActivity(this);
		mShakeDetect.addListener(new ShakeDetectActivityListener() {
			@Override
			public void shakeDetected(int x_pos, int x_neg, int y_pos,
					int y_neg, int z_pos, int z_neg) {
				MainActivity.this.triggerShakeDetected(x_pos, x_neg, y_pos,
						y_neg, z_pos, z_neg);
			}
		});

		mMediaPlayer = new MediaPlayer(this);
		//mHandler.postDelayed(mMenuRunable, 1000);
		
		mMenuThread = new Thread(new MenuRunnable());
		mMenuThread.start();
		//playAudio(mMediaPath + "/menu_1_1_netradio.ogg");
		//playAudio(mMediaPath + "mMenuDescPath");
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
		
		mMenuRun = false;
		if (mMenuThread != null) {
			try {
				mMenuThread.join(); // wait for menu thread to finish.
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			
			mMenuThread = null;
		}
		
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

	public void triggerShakeDetected(int x_pos, int x_neg, int y_pos,
			int y_neg, int z_pos, int z_neg) {
		// do something!
		mShakeCount++;
		String msg = "Shake:" + mShakeCount;
		int dx = x_pos + x_neg;
		int dy = y_pos + y_neg;
		int dz = z_pos + z_neg;
		if (dx > dy && dx > dz) {
			msg += " X";
		} else if (dy > dx && dy > dz) {
			msg += " Y";
		} else if (dz > dx && dz > dy) {
			msg += " Z";
		}

		msg += Integer.toString(x_pos) + " - " + Integer.toString(x_neg) + "  "
				+ Integer.toString(y_pos) + " - " + Integer.toString(y_neg)
				+ "  " + Integer.toString(z_pos) + " - "
				+ Integer.toString(z_neg);

		mTextView.setText(msg);
		Vibrator vib = (Vibrator) MainActivity.this
				.getSystemService(Service.VIBRATOR_SERVICE);
		vib.vibrate(500);

		mHandler.removeCallbacks(mShakeCountRunnable);
		mHandler.postDelayed(mShakeCountRunnable, 1000);
	}

	private void playAudio(String path) {
		try {
			//mMediaPlayer = new MediaPlayer(this);
			mMediaPlayer.setDataSource(path);
			mMediaPlayer.prepare();
			mMediaPlayer.start();
		} catch (Exception e) {
			Log.e(TAG, "error: " + e.getMessage(), e);
		}
	}

	private static final int MAIN_MENU_SHAKE_COUNT = 3;

	private static final int MSG_MAIN_MENU = 1;
	private static final int MSG_SHAKE_ONE = 2;
	private static final int MSG_SHAKE_TWO = 3;
	private static final int MSG_UPDATE_DESCRIPTION = 4;
	private static final int MSG_MENU_SELECT = 5;

	private class MainAcitvityHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_MAIN_MENU:
				mTextView.setText("popup main menu.");
				mMenuRun = false;
				if (mMenuThread != null) {
					try {
						mMenuThread.join(); // wait for menu thread to finish.
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
				mMenuThread = new Thread(new MenuRunnable());
				mMenuThread.start();
				break;
			case MSG_MENU_SELECT:
				mTextView.setText("menu selected = " + msg.arg1);
				break;
				
			case MSG_UPDATE_DESCRIPTION:
				mTextView.setText("popup main menu.");
				break;
			default:
				break;
			}
		}
	}

	private final Runnable mShakeCountRunnable = new Runnable() {
		public void run() {
			if (mShakeCount == 1) {
				mHandler.sendEmptyMessage(MSG_SHAKE_ONE);
			}

			if (mShakeCount >= MAIN_MENU_SHAKE_COUNT) {
				mHandler.sendEmptyMessage(MSG_MAIN_MENU);
			}
			mShakeCount = 0;
		}
	};

	//////////////////////////////////////////////////////////////////
	// Menu runable

	private class MenuItem {
		public String audioPath;
		public String action;
		public String description;

		public MenuItem(String audioPath, String action, String description) {
			this.audioPath = audioPath;
			this.action = action;
			this.description = description;
		}
	}

	private List<MenuItem> mMenu = new ArrayList<MenuItem>();
	protected boolean mIsInMenuSelect = false;
	protected int mMenuSelect = 0;

	// Show menu
	private class MenuRunnable implements Runnable {
		private final int WAIT_TIME_IN_Millis = 500; 

		public void run() {
			mMenuRun = true;
			mIsInMenuSelect = true;

			// Say: You can shake 3 times to show or hide menu . 
			try {
				// Show description
				mMediaPlayer.setDataSource(mMenuDescPath);
				mMediaPlayer.prepare();
				mMediaPlayer.start();
				while (mMediaPlayer.isPlaying()) {
					if (!mMenuRun) break;
					Thread.sleep(WAIT_TIME_IN_Millis);
				}
				mMediaPlayer.reset();
			} catch (Exception e) {
				mMediaPlayer.reset();
				Log.e(TAG, "error: " + e.getMessage(), e);
				if (!mMenuRun) return;
			}

			while (mIsInMenuSelect && mMenuRun) {
				
				try {
					// Say: Please let screen turn up. 
					do {
						mMediaPlayer.setDataSource(mMenuSelectWaitPath);
						mMediaPlayer.prepare();
						mMediaPlayer.start();
						while (mMediaPlayer.isPlaying() && !mShakeDetect.isScreenUp()) {
							Log.i(TAG, "Menuitem_a: wait up");
							if (!mMenuRun) break;
							Thread.sleep(WAIT_TIME_IN_Millis);
						}
						mMediaPlayer.reset();
						
						if (!mMenuRun) break;
					} while (!mShakeDetect.isScreenUp());

					if (!mMenuRun) break;

					// Say: Start select. 
					mMediaPlayer.setDataSource(mMenuSelectStartPath);
					mMediaPlayer.prepare();
					mMediaPlayer.start();
					while (mMediaPlayer.isPlaying()) {
						Log.i(TAG, "Menuitem_a: wait up");
						if (!mMenuRun) break;
						Thread.sleep(WAIT_TIME_IN_Millis);
					}
					mMediaPlayer.reset();
					
				} catch (Exception e) {
					Log.e(TAG, "error: " + e.getMessage(), e);
					if (!mMenuRun) break;
				}
				
				if (!mMenuRun) break;

				// Show menu and check menu selected.
				mMenuSelect = 0;				
				for (MenuItem menu : mMenu) {
					try {
	                    //Message msg = new Message();   
	                    //msg.what = MSG_UPDATE_DESCRIPTION;
	                    //msg.arg1 = mMenuSelect;					
						//mHandler.sendMessage(msg);
						
						Log.i(TAG, "Menuitem1: " + menu.audioPath);
						mMediaPlayer.setDataSource(menu.audioPath);
						Log.i(TAG, "Menuitem2: ");
						mMediaPlayer.prepare();
						Log.i(TAG, "Menuitem3: ");
						mMediaPlayer.start();
						Log.i(TAG, "Menuitem4: ");
						
						// Check user select.
						while (mMediaPlayer.isPlaying()) {
							Log.i(TAG, "Menuitem5: ");
							if (!mMenuRun) break;
							Thread.sleep(WAIT_TIME_IN_Millis);
							Log.i(TAG, "Menuitem6: " + mShakeDetect.isScreenDown());
							
							if (mShakeDetect.isScreenDown()) {
								mMediaPlayer.reset();

								// Say: function is selected.
								mMediaPlayer.setDataSource(mMenuSelectDonePath);
								mMediaPlayer.prepare();
								mMediaPlayer.start();
								while (mMediaPlayer.isPlaying()) {
									if (!mMenuRun) break;
									Thread.sleep(WAIT_TIME_IN_Millis);
								}
								mMediaPlayer.reset();

								// Say: what function is select.
								mMediaPlayer.setDataSource(menu.audioPath);
								mMediaPlayer.prepare();
								mMediaPlayer.start();
								while (mMediaPlayer.isPlaying()) {
									if (!mMenuRun) break;
									Thread.sleep(WAIT_TIME_IN_Millis);
								}
								mMediaPlayer.reset();
								
								// Send message to do something.
			                    Message menuMsg = new Message();   
			                    menuMsg.what = MSG_MENU_SELECT;
			                    menuMsg.arg1 = mMenuSelect;
								mHandler.sendMessage(menuMsg);
								
								// Stop menu.
								mMenuRun = false;
								break;
							}
						}
						mMediaPlayer.reset();
						
						if (!mMenuRun) break;
					} catch (Exception e) {
						Log.i(TAG, "Menuitem7: ");
						Log.e(TAG, "error: " + e.getMessage(), e);
						if (!mMenuRun) return;
					}
					mMenuSelect++;
					Log.i(TAG, "Menuitem8: ");
				}
				Log.i(TAG, "Menuitem9: ");
				
				if (!mMenuRun) break;
				
				// Wait sometime.
				try {
					Thread.sleep(WAIT_TIME_IN_Millis);
					if (!mMenuRun) return;
					Thread.sleep(WAIT_TIME_IN_Millis);
					if (!mMenuRun) return;
					Thread.sleep(WAIT_TIME_IN_Millis);
					if (!mMenuRun) return;
				} catch (Exception e) {
					Log.e(TAG, "error: " + e.getMessage(), e);
					if (!mMenuRun) return;
				}
			}
			
			mIsInMenuSelect = false;
		}		
	}
	
}
