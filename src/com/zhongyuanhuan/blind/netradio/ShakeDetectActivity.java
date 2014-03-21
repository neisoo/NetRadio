package com.zhongyuanhuan.blind.netradio;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;


/**
 * 
 * A shake detection library. 
 * To use, have your Activity create an instance of the class and set a listener using addListener();
 * Make sure you call onResume() and onPause() on from the activity
 * 
 * @author James
 * @copyright 2013 JMB Technology Limited
 * @license Open Source; 3-clause BSD 
 */
public class ShakeDetectActivity implements SensorEventListener {
	
	SensorManager sensorMgr;

	public ShakeDetectActivity(Context context) {
		sensorMgr = (SensorManager) context.getSystemService(Activity.SENSOR_SERVICE);
		sensorMgr.registerListener(this,sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_GAME);
	}	

	public void onResume() {
		sensorMgr.registerListener(this,sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_GAME);
	}

	public void onPause() {
		sensorMgr.unregisterListener(this);
	}

	private class DataPoint {
		public float x, y, z;
		public long atTimeMilliseconds;
		public DataPoint(float x, float y, float z, long atTimeMilliseconds) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.atTimeMilliseconds = atTimeMilliseconds;
		}
	}

	private List<DataPoint> dataPoints = new ArrayList<DataPoint>();

	private static final int SHAKE_CHECK_THRESHOLD = 200; // 多长时间检查一次晃动

	/**
	 * After we detect a shake, we ignore any events for a bit of time. We don't want two shakes to close together.
	 */
	private static final int IGNORE_EVENTS_AFTER_SHAKE = 500; // 两次晃动的最小时间间隔
	private long lastUpdate;
	private long lastShake = 0;

	private float last_x = 0, last_y=0, last_z=0; 

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

			long curTime = System.currentTimeMillis();
			// if a shake in last X seconds ignore.
			if (lastShake != 0 && (curTime - lastShake) < IGNORE_EVENTS_AFTER_SHAKE) return;

			float x = event.values[SensorManager.DATA_X];
			float y = event.values[SensorManager.DATA_Y];
			float z = event.values[SensorManager.DATA_Z];
			
			if (last_x != 0 && last_y != 0 && last_z != 0 && (last_x != x || last_y != y || last_z != z)) {
				DataPoint dp = new DataPoint(last_x-x, last_y-y, last_z-z, curTime);
				//Log.i("XYZ",Float.toString(dp.x)+"   "+Float.toString(dp.y)+"   "+Float.toString(dp.z)+"   ");
				dataPoints.add(dp);

				if ((curTime - lastUpdate) > SHAKE_CHECK_THRESHOLD) {
					lastUpdate = curTime;
					checkForShake();
				}
			}
			last_x = x;
			last_y = y;
			last_z = z;
		}		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private static final long KEEP_DATA_POINTS_FOR = 1500; // 保存最近多长时间的采样，超过时间的采样忽略，决定动作需在多长时间内完成。
	private static final long MINIMUM_EACH_DIRECTION = 1; // 每个轴加速度方向改变次数, 一次往复为1，决定往复次数。
	private static final float POSITIVE_COUNTER_THRESHHOLD = (float) 5.0;  // 正方向的最小有效变化值，决定动作剧烈程度。 
	private static final float NEGATIVE_COUNTER_THRESHHOLD = (float) -5.0; // 负方向的最小有效变化值，决定动作剧烈程度。

	public void checkForShake() {
		long curTime = System.currentTimeMillis();
		long cutOffTime = curTime - KEEP_DATA_POINTS_FOR;
		while(dataPoints.size() > 0 && dataPoints.get(0).atTimeMilliseconds < cutOffTime) dataPoints.remove(0);

		int x_pos =0, x_neg=0, x_dir = 0, y_pos=0, y_neg=0, y_dir=0, z_pos=0, z_neg = 0, z_dir = 0;
		for(DataPoint dp: dataPoints){
			if (dp.x > POSITIVE_COUNTER_THRESHHOLD && x_dir < 1) {
				++x_pos;
				x_dir = 1;
			}
			if (dp.x < NEGATIVE_COUNTER_THRESHHOLD && x_dir > -1) {
				++x_neg;
				x_dir = -1;
			}
			if (dp.y > POSITIVE_COUNTER_THRESHHOLD && y_dir < 1) {
				++y_pos;
				y_dir = 1;
			}
			if (dp.y < NEGATIVE_COUNTER_THRESHHOLD && y_dir > -1) {
				++y_neg;
				y_dir = -1;
			}
			if (dp.z > POSITIVE_COUNTER_THRESHHOLD && z_dir < 1) {
				++z_pos;
				z_dir = 1;
			}
			if (dp.z < NEGATIVE_COUNTER_THRESHHOLD && z_dir > -1) {
				++z_neg;			
				z_dir = -1;
			}
		}
		//Log.i("CHANGE",Integer.toString(x_pos)+" - "+Integer.toString(x_neg)+"  "+Integer.toString(y_pos)+" - "+Integer.toString(y_neg)+"  "+Integer.toString(z_pos)+" - "+Integer.toString(z_neg));

		if ((x_pos >= MINIMUM_EACH_DIRECTION && x_neg >= MINIMUM_EACH_DIRECTION) || 
				(y_pos >= MINIMUM_EACH_DIRECTION && y_neg >= MINIMUM_EACH_DIRECTION) || 
				(z_pos >= MINIMUM_EACH_DIRECTION && z_neg >= MINIMUM_EACH_DIRECTION) ) {						
			lastShake = System.currentTimeMillis();
			last_x = 0; last_y = 0; last_z = 0;
			dataPoints.clear();
			triggerShakeDetected(x_pos, x_neg, y_pos, y_neg, z_pos, z_neg);
			return;
		}

	}
	
	ArrayList<ShakeDetectActivityListener> listeners = new ArrayList<ShakeDetectActivityListener>();
	
	public void addListener(ShakeDetectActivityListener listener) {
		listeners.add(listener);
	}
	
	protected void triggerShakeDetected(int x_pos, int x_neg, int y_pos, int y_neg, int z_pos, int z_neg) {
		for(ShakeDetectActivityListener listener: listeners) {
			listener.shakeDetected(x_pos, x_neg, y_pos, y_neg, z_pos, z_neg);
		}
	}


}

