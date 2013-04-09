package com.example.hiparsely;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import com.example.parselyandroid.*;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ParselyTracker.sharedInstance("arstechnica.com", 2);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void trackURL(View view) {
		Log.i("MainActivity", "track url called");
	}
	
	public void trackPID(View view) {
		Log.i("MainActivity", "track pid called");
	}
	
	public void toggleConnection(View view) {
		Log.i("MainActivity", "toggle connection called");
	}
}
