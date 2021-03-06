package com.ionic.keyboard;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.graphics.Rect;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;
import android.util.DisplayMetrics;

public class IonicKeyboard extends CordovaPlugin{
    public static String TAG = "Safetybank";
    public static String FullScreenPreferenceName = "Fullscreen";
    public static String FSModeImmersive = "Immersive";
    public static String FSModeNonImmersive = "NonImmersive";
    public static String FSModeFullScreen = "FullScreen";
    public static String FSModeNonFullScreen = "NonFullScreen";
	private float density;

    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        //calculate density-independent pixels (dp)
        //http://developer.android.com/guide/practices/screens_support.html
        DisplayMetrics dm = new DisplayMetrics();
        cordova.getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        density = dm.density;

        final CordovaWebView appView = webView;

        //http://stackoverflow.com/a/4737265/1091751 detect if keyboard is showing
        final View rootView = cordova.getActivity().getWindow().getDecorView().findViewById(android.R.id.content).getRootView();
        OnGlobalLayoutListener list = new OnGlobalLayoutListener() {
            int previousHeightDiff = 0;
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                //r will be populated with the coordinates of your view that area still visible.
                rootView.getWindowVisibleDisplayFrame(r);

				View v = rootView.getRootView();
				String mode = getUserFullScreenPreference();
				
				if (Build.VERSION.SDK_INT > 16) {
					appView.sendJavascript("cordova.fireWindowEvent('native.viewPortChanged', " + 
					"{" +
						"density: '" + Float.toString(density) + "', " +
						"api: '"+ Integer.toString(Build.VERSION.SDK_INT) + "', " +
						"mode: '"+ mode + "', " +
						"viewPort: {" + 
							"top: " + Integer.toString(r.top) + ", " +
							"bottom: " + Integer.toString(r.bottom) + ", " +
							"left: " + Integer.toString(r.left) + ", " +
							"right: " + Integer.toString(r.right) + ", " +
							"width: " + Integer.toString(r.width()) + ", " +
							"height: " + Integer.toString(r.height()) + //", " +
						"}, " +
						"device: {" +
							"top: " + Integer.toString(v.getTop()) + ", " +
							"bottom: " + Integer.toString(v.getBottom()) + ", " +
							"left: " + Integer.toString(v.getLeft()) + ", " +
							"top: " + Integer.toString(v.getTop()) + ", " +
							"width: " + Integer.toString(v.getWidth()) + ", " +
							"height: " + Integer.toString(v.getHeight()) + //", " +
						"}"+ //", " +
					"});");
				}
				
				appView.sendJavascript("cordova.fireWindowEvent('native.buildVersionInfo', " + 
					"{" +
						"codename: '"+ Build.VERSION.CODENAME + "', " +
						"incremental: '"+ Build.VERSION.INCREMENTAL + "', " +
						"release: '"+ Build.VERSION.RELEASE + "', " +
						"sdk_int: '"+ Integer.toString(Build.VERSION.SDK_INT) + "', " +
					"});");
			}
        };

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(list);
    }
	private String fullScreenSetMessage;
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if ("close".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    //http://stackoverflow.com/a/7696791/1091751
                    InputMethodManager inputManager = (InputMethodManager) cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    View v = cordova.getActivity().getCurrentFocus();

                    if (v == null) {
                        callbackContext.error("No current focus");
                    } else {
                        inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        callbackContext.success(); // Thread-safe.
                    }
                }
            });
            return true;
        }
        if ("show".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    ((InputMethodManager) cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
                    callbackContext.success(); // Thread-safe.
                }
            });
            return true;
        }

		if ("setFullScreenPreference".equals(action)) {
			final String setOption = args.getString(0);
			Log.d(TAG, "setFullScreenPreference: " + setOption);

			fullScreenSetMessage = "Error setting your preference";
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
					final Window window = cordova.getActivity().getWindow();
					String option = setOption;
					if (!FSModeImmersive.equals(option) 
						&& !FSModeFullScreen.equals(option)
						&& !FSModeNonImmersive.equals(option) 
						&& !FSModeNonFullScreen.equals(option)) {
						option = getUserFullScreenPreference();
						Log.d(TAG, "setFullScreenPreference.enforced " + setOption);
					}

					if (FSModeImmersive.equals(option)) {
						fullScreenSetMessage = goImmersive(window);
					}
					else if (FSModeFullScreen.equals(option)) {
						fullScreenSetMessage = goFullScreen(window);
					}
					else if (FSModeNonImmersive.equals(option)) {
						fullScreenSetMessage = goNonImmersive(window);
					}
					else if (FSModeNonFullScreen.equals(option)) {
						fullScreenSetMessage = goNonFullScreen(window);
					}
					
					JSONObject response = new JSONObject();
					injectFullScreenResult(response);
					try {
						response.put("message", fullScreenSetMessage);
					}
					catch (JSONException e) {
						  Log.e(TAG, "Problem creating JSON object from setFullScreenPreference:" + e.getMessage());
					}
					PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, response);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                    //callbackContext.success(); // Thread-safe.
                }
            });
            return true;
        }
		if ("getFullScreenPreference".equals(action)) {
			fullScreenSetMessage = "Error getting your FullScreen preference";
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() { 
					String mode = getUserFullScreenPreference();
					Log.d(TAG, "getFullScreenPreference:"+mode);
					JSONObject response = new JSONObject();
					injectFullScreenResult(response);
					PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, response);
                    pluginResult.setKeepCallback(true);
					callbackContext.sendPluginResult(pluginResult);
                    //callbackContext.success(); // Thread-safe.
                }
            });
            return true;
        }
        return false;  // Returning false results in a "MethodNotFound" error.
    }
	private void injectFullScreenResult(JSONObject response) {
		//http://stackoverflow.com/a/4737265/1091751 detect if keyboard is showing
        final View rootView = cordova.getActivity().getWindow().getDecorView().findViewById(android.R.id.content).getRootView();
        
		Rect r = new Rect();
		//r will be populated with the coordinates of your view that area still visible.
		rootView.getWindowVisibleDisplayFrame(r);

		View v = rootView.getRootView();
		String mode = getUserFullScreenPreference();
		try {
			response.put("density", Float.toString(density));
			response.put("mode", mode);
			response.put("api", Build.VERSION.SDK_INT);
			JSONObject viewPort = new JSONObject();
			viewPort.put("top", r.top);
			viewPort.put("bottom", r.bottom);
			viewPort.put("left", r.left);
			viewPort.put("right", r.right);
			viewPort.put("width", r.width());
			viewPort.put("height", r.height());
			response.put("viewPort", viewPort); 
			JSONObject device = new JSONObject();
			device.put("top", v.getTop());
			device.put("bottom", v.getBottom());
			device.put("left", v.getLeft());
			device.put("top", v.getTop());
			device.put("width", v.getWidth());
			device.put("height", v.getHeight());
			response.put("device", device);
		}
		catch (JSONException e) {
				Log.e(TAG, "Problem creating JSON object from getFullScreenResult:" + e.getMessage());
		}
	}
	private String goImmersive(Window window) {
		saveUserPreference(FullScreenPreferenceName, FSModeImmersive);
		String message;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			saveUserPreference(FullScreenPreferenceName, FSModeNonFullScreen);
			message = "Immersive mode not supported on this ANCIENT Android version";
			window.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, 
								 WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			saveUserPreference(FullScreenPreferenceName, FSModeNonImmersive);
			message = "Immersive mode not supported on this Android version";
			window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	    else {
			message = "Full screen immersive mode activated";
			window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
		Log.d(TAG, message);
		return message;
    }
	private String goNonImmersive(Window window) {
		saveUserPreference(FullScreenPreferenceName, FSModeNonImmersive);
		String message;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			saveUserPreference(FullScreenPreferenceName, FSModeNonFullScreen);
			message = "Full screen non imersive mode is not supported by this ANCIENT Android version";
			window.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, 
								 WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			message = "Full screen non-immersive mode activated";
			window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	    else {
			message = "Full screen non-immersive mode activated";
			window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
		Log.d(TAG, message);
		return message;
    }
	private String goFullScreen(Window window) {
		saveUserPreference(FullScreenPreferenceName, FSModeFullScreen);
		String message;

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			saveUserPreference(FullScreenPreferenceName, FSModeNonFullScreen);
			message = "Full screen mode is not supported by this ANCIENT Android version";
			window.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, 
								 WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			message = "Full screen mode activated";
			window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	    else {
			message = "Full screen mode activated";
			window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
		}
		Log.d(TAG, message);
		return message;
    }
	private String goNonFullScreen(Window window) {
		saveUserPreference(FullScreenPreferenceName, FSModeNonFullScreen);
		String message = "Non full screen mode activated";
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			window.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, 
								 WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			window.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	    else {
			window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}
		return message;
    }
	private String getUserFullScreenPreference() 
	{
		SharedPreferences sharedPref = cordova.getActivity().getPreferences(Context.MODE_PRIVATE);
		String mode = sharedPref.getString(FullScreenPreferenceName, FSModeImmersive);
		if (mode == null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
				return FSModeNonFullScreen;
			} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				return FSModeNonImmersive;
			}
			else {
				return FSModeImmersive;
			}
		}
		return mode;
	}
	private void saveUserPreference(String preference, String value) {
		SharedPreferences sharedPref = cordova.getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(preference, value);
		editor.commit();
	}
}

