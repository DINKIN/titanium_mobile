/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2016 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.filesystem;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollInvocation;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;

import ti.modules.titanium.stream.FileStreamProxy;

@Kroll.module
public class FilesystemModule extends KrollModule
{
	private static final String TAG = "TiFilesystem";

	@Kroll.constant public static final int MODE_READ = 0;
	@Kroll.constant public static final int MODE_WRITE = 1;
	@Kroll.constant public static final int MODE_APPEND = 2;

	private static String[] RESOURCES_DIR = { "app://" };

	// Methods
	public FilesystemModule()
	{
		super();
	}

	@Kroll.method
	public FileProxy createTempFile(KrollInvocation invocation)
	{
		try {
			File f = File.createTempFile("tifile", "tmp");
			String[] parts = { f.getAbsolutePath() };
			return new FileProxy(invocation.getSourceUrl(), parts, false);
		} catch (IOException e) {
			Log.e(TAG, "Unable to create tmp file: " + e.getMessage(), e);
			return null;
		}
	}

	@Kroll.method
	public FileProxy createTempDirectory(KrollInvocation invocation)
	{
		String dir = String.valueOf(System.currentTimeMillis());
		File tmpdir = new File(System.getProperty("java.io.tmpdir"));
		File f = new File(tmpdir,dir);
		f.mkdirs();
		String[] parts = { f.getAbsolutePath() };
		return new FileProxy(invocation.getSourceUrl(), parts);
	}

	@Kroll.getProperty @Kroll.method
	public boolean isExternalStoragePresent()
	{
		return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}

	@Kroll.method
	public FileProxy getFile(KrollInvocation invocation, Object[] parts)
	{
		//If directory doesn't exist, return
		if (parts[0] == null) {
		    Log.w(TAG, "A null directory was passed. Returning null.");
		    return null;
		}
		String[] sparts = TiConvert.toStringArray(parts);
		return new FileProxy(invocation.getSourceUrl(), sparts);
	}

	@Kroll.method
	private boolean hasStoragePermissions() {
		if (Build.VERSION.SDK_INT < 23) {
			return true;
		}
		Activity currentActivity = TiApplication.getInstance().getCurrentActivity();
		if (currentActivity.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
			return true;
		}
		return false;
	}

	@Kroll.method
	public void requestStoragePermissions(@Kroll.argument(optional=true)KrollFunction permissionCallback)
	{
		if (hasStoragePermissions()) {
			return;
		}

		String[] permissions = new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE};
		Activity currentActivity = TiApplication.getInstance().getCurrentActivity();
		TiBaseActivity.registerPermissionRequestCallback(TiC.PERMISSION_CODE_EXTERNAL_STORAGE, permissionCallback, getKrollObject());
		currentActivity.requestPermissions(permissions, TiC.PERMISSION_CODE_EXTERNAL_STORAGE);

	}

	@Kroll.getProperty @Kroll.method
	public FileProxy getApplicationDirectory()
	{
		return null;
	}

	@Kroll.getProperty @Kroll.method
	public String getApplicationDataDirectory()
	{
		return "appdata-private://";
	}

	@Kroll.getProperty @Kroll.method
	public String getResRawDirectory()
	{
		return "android.resource://" + TiApplication.getInstance().getPackageName() + "/raw/";
	}

	@SuppressWarnings("deprecation")
	@Kroll.getProperty @Kroll.method
	public String getApplicationCacheDirectory()
	{
		TiApplication app = TiApplication.getInstance();
		if (app == null) {
			return null;
		}

		File cacheDir = app.getCacheDir();

		try {
			return cacheDir.toURL().toString();

		} catch (MalformedURLException e) {
			Log.e(TAG, "Exception converting cache directory to URL", e);
			return null;
		}
	}

	@Kroll.getProperty @Kroll.method
	public String getResourcesDirectory()
	{
		return "app://";
	}

	@Kroll.getProperty @Kroll.method
	public String getExternalStorageDirectory()
	{
		return "appdata://";
	}

	@Kroll.getProperty @Kroll.method
	public String getTempDirectory()
	{
		TiApplication tiApplication = TiApplication.getInstance();
		return "file://" + tiApplication.getTempFileHelper().getTempDirectory().getAbsolutePath();
	}

	@Kroll.getProperty @Kroll.method
	public String getSeparator()
	{
		return File.separator;
	}

	@Kroll.getProperty @Kroll.method
	public String getLineEnding()
	{
		return System.getProperty("line.separator");
	}

	@Kroll.method
	public FileStreamProxy openStream(KrollInvocation invocation, int mode, Object[] parts) throws IOException
	{
		String[] sparts = TiConvert.toStringArray(parts);
		FileProxy fileProxy = new FileProxy(invocation.getSourceUrl(), sparts);
		fileProxy.getBaseFile().open(mode, true);

		return new FileStreamProxy(fileProxy);
	}

	@Override
	public String getApiName()
	{
		return "Ti.Filesystem";
	}
}
