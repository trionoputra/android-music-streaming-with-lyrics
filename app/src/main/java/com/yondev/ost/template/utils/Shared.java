package com.yondev.ost.template.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Typeface;
import android.view.Display;
import android.view.WindowManager;

public final class Shared
{
	private static ContextWrapper instance;
	public static Typeface OpenSansBold;
	public static Typeface OpenSansRegular;
	public static Typeface openSansLight;
	public static String SERVER_URL = "http://104.131.24.96/api/sound_track/web/";
	public static void initialize(Context base)
	{
		instance = new ContextWrapper(base);
		OpenSansBold = Typeface.createFromAsset(instance.getAssets(),"fonts/OpenSans-Bold.ttf");
		OpenSansRegular = Typeface.createFromAsset(instance.getAssets(),"fonts/OpenSans-Regular.ttf");
		openSansLight = Typeface.createFromAsset(instance.getAssets(),"fonts/OpenSans-Light.ttf");
	}

	public static Context getContext()
	{
		return instance.getBaseContext();
	}

}

