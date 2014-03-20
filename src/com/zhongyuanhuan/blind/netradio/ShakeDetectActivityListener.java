package com.zhongyuanhuan.blind.netradio;

/**
 * 
 * Part of a shake detection library. 
 * 
 * @author James
 * @copyright 2013 JMB Technology Limited
 * @license Open Source; 3-clause BSD
 * @see uk.co.jarofgreen.lib.ShakeDetectActivity
 */
public abstract class ShakeDetectActivityListener {
	
	public abstract void shakeDetected(int x_pos, int x_neg, int y_pos, int y_neg, int z_pos, int z_neg);
	
}
