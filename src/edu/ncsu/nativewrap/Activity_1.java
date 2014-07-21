package edu.ncsu.nativewrap;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.io.FileUtils;



import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import android.os.StrictMode;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_1 extends Activity {
	static final String logTag="NativeWrap";
	static Context context;
	static final String hash_link="https://dl.dropboxusercontent.com/s/fbzxjhip9mx36kg/ruleset_hash.txt";
	static final String ruleset_link="https://dl.dropboxusercontent.com/s/etpfkfkt5b7360f/ruleset.xml";
	static String ruleset_hash_path;
	static String ruleset_xml_path;
	static String new_ruleset_hash_path;
	static boolean downloadUpdate=false;
	boolean checkComplete=false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_activity_1);
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);       
		return;
	}

	protected void onStart()
    {	
    	super.onStart();	
        context=this;
    	ruleset_hash_path= getFilesDir()+"/ruleset_hash.txt";
    	ruleset_xml_path= getFilesDir()+"/ruleset.xml";
    	new_ruleset_hash_path=getFilesDir()+"/ruleset_hash_new.txt";	
		new CheckUpdate().execute();
		return;
	}
	
	void downloadUpdateThread(){
		new DownloadUpdate().execute();
	}
	//Download a file using HTTPSURLConnection
	static boolean downloadFile(String url_string, String destination){
		try {
			URL url = new URL(url_string);
		    HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
		    urlConnection.setRequestMethod("GET");
		    urlConnection.setDoOutput(false);
		    urlConnection.setRequestProperty("Accept","*/*");
		    urlConnection.connect();
		    File file = new File(destination);

		    FileOutputStream fileOutput = new FileOutputStream(file);
		    
		    InputStream inputStream = urlConnection.getInputStream();

		    byte[] buffer = new byte[1024];
		    int bufferLength = 0;

		    while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
		        fileOutput.write(buffer, 0, bufferLength);
		    }
		    fileOutput.close();

		} catch (MalformedURLException e) {
		        e.printStackTrace();
		        return false;
		} catch (IOException e) {
		        e.printStackTrace();
		        return false;
		}
		
		return true;
	}
	
	static String getRulesetHash(String filename){
		Scanner scan_hash;
		try {
			scan_hash = new Scanner(new File(filename));
			return scan_hash.nextLine();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	void forwardIntent(Intent intent){
    	if (Intent.ACTION_MAIN.equals(intent.getAction())) {
    		intent.setAction(Intent.ACTION_SEND);
    		intent.setType("text/plain");
    		intent.putExtra(Intent.EXTRA_TEXT,"https://www.google.com");
    		intent.removeCategory(Intent.CATEGORY_LAUNCHER);
    		//intent.setClass(getApplicationContext(), WrapMain.class);
    		
		}
    	intent.setClass(context, WrapMain.class);
		context.startActivity(intent);
	}
	
	private class CheckUpdate extends AsyncTask<Void, Void, Void> {

	    private ProgressDialog updatedialog;
	    private AlertDialog.Builder alertDialogBuilder;
	    private AlertDialog alert;
	    private boolean abort=false;
	    @Override
	    protected void onPreExecute() {
			updatedialog = new ProgressDialog(Activity_1.this);
			updatedialog.setTitle("Checking for HTTPSEverywhere Ruleset update.");
			updatedialog.setMessage("Please wait...");
			updatedialog.setCancelable(false);
			//dialog.setIndeterminate(true);
			updatedialog.show();
	    }

	    @Override
	    protected Void doInBackground(Void... arg0) {
	    	//Does hash file exist?
	    	//If not, get from assets, then check for updates
			File ruleset_hash=new File(ruleset_hash_path);
			if(!ruleset_hash.isFile()){
				AssetManager assetManager = getAssets();
				try {
				    InputStream fis_1 = assetManager.open("ruleset_hash.txt");
				    FileUtils.copyInputStreamToFile(fis_1, new File(ruleset_hash_path));
				    InputStream fis_2 = assetManager.open("ruleset.xml");
				    FileUtils.copyInputStreamToFile(fis_2, new File(ruleset_xml_path));
				}
				catch(Exception e){
					abort=true;
					//forwardIntent(getIntent());
					return null;
				}
			}		
			Log.d(logTag, "Ruleset resources in place");		
			//Download ruleset hash first
			if(!downloadFile(hash_link, new_ruleset_hash_path)){
				abort=true;
				//forwardIntent(getIntent());
				return null;
			}
			String old_hash = getRulesetHash(ruleset_hash_path);
			String new_hash = getRulesetHash(new_ruleset_hash_path);
			if(old_hash==null || new_hash==null){
				abort=true;
				//forwardIntent(getIntent());
				return null;
			}
			Log.d(logTag, "New hash: "+new_hash);
			
			if(!old_hash.equals(new_hash)){
		        downloadUpdate=true;
				return null;
		    }
			else {
				Log.d(logTag, "Hash match, no need to download");
				//Proceed to the next activity, show a toast and then the next activity.
				//Toast toast = Toast.makeText(Activity_1.context, "NativeWrap is up to date! Thanks for waiting.", Toast.LENGTH_SHORT);
				//toast.setGravity(Gravity.CENTER|Gravity.CENTER, 0, 0);
				//toast.show();	
				//forwardIntent(getIntent());
				abort=true;
				return null;
			}
			
	    	//return null;
	    }	
	    @SuppressLint("NewApi")
		protected void onPostExecute(Void result) {
	        if (updatedialog.isShowing()) {
	        	updatedialog.dismiss();
	        }
	        if(abort){
	        	forwardIntent(getIntent());
	        }
	        checkComplete=true;
			if(downloadUpdate){
			    AlertDialog.Builder alertDialogBuilder;
			    AlertDialog alert;
				alertDialogBuilder = new AlertDialog.Builder(context);
				alertDialogBuilder.setTitle("Update NativeWrap?");
				alertDialogBuilder.setMessage("Will update the HTTPSEverywhere Ruleset. Crucial for forcing HTTPS.");
				alertDialogBuilder.setCancelable(false);
				alertDialogBuilder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						downloadUpdateThread();
					}
				});
				alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						Toast toast = Toast.makeText(context, "NativeWrap's HTTPS rules need to be updated.", Toast.LENGTH_SHORT);
		    			toast.setGravity(Gravity.CENTER|Gravity.CENTER, 0, 0);
		    			toast.show();
						dialog.cancel();
						forwardIntent(getIntent());
					}
				});
				alert = alertDialogBuilder.create();
				alert.show();
			}
	    }
	}
	
	private class DownloadUpdate extends AsyncTask<Void, Void, Void> {
    	private ProgressDialog progress;
	    @Override
	    protected void onPreExecute() {
	    	progress = new ProgressDialog(Activity_1.this);
			progress.setTitle("Updating NativeWrap.");
			progress.setMessage("Please wait...");
			progress.setCancelable(false);
			//dialog.setIndeterminate(true);
			progress.show();
	    }
	    @Override
	    protected Void doInBackground(Void... arg0) {		
			final String new_ruleset_xml_path=getFilesDir()+"new_ruleset.xml";
	  	    if(!downloadFile(ruleset_link, new_ruleset_xml_path)){
    	    	//forwardIntent(getIntent());
	  	    	Toast toast = Toast.makeText(context, "Download Failed! Aborting.", Toast.LENGTH_SHORT);
	  	    	return null;
    	    }
    	    //replace old files with new
    	    File oldfiles[]={new File(ruleset_hash_path), new File(ruleset_xml_path)};
    	    oldfiles[0].delete();
    	    oldfiles[1].delete();
    	    File newfiles[]={new File(new_ruleset_hash_path), new File(new_ruleset_xml_path)};
    	    newfiles[0].renameTo(new File(ruleset_hash_path));
    	    newfiles[1].renameTo(new File(ruleset_xml_path));
    	    //Inform the user
    	    //Toast toast = Toast.makeText(context, "Download Complete. NativeWrap is now up to date.", Toast.LENGTH_SHORT);
    		//toast.setGravity(Gravity.CENTER|Gravity.CENTER, 0, 0);
    		//toast.show();	
			return null;
		}
	    @SuppressLint("NewApi")
		protected void onPostExecute(Void result) {
	        if (progress.isShowing()) {
	        	progress.dismiss();
	        }
	        forwardIntent(getIntent());
	    }
	}
	
}
