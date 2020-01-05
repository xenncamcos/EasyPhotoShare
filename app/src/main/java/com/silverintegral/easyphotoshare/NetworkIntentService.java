package com.silverintegral.easyphotoshare;

import android.app.IntentService;
import android.content.Intent;

public class NetworkIntentService extends IntentService {
	final static String TAG = "NetworkIntentService";

	public NetworkIntentService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			Thread.sleep(10000);

			Intent broadcastIntent = new Intent();
			broadcastIntent.putExtra(
					"message", "Hello, BroadCast!");
			broadcastIntent.setAction("MY_ACTION");
			getBaseContext().sendBroadcast(broadcastIntent);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
