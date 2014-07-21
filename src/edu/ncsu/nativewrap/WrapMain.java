package edu.ncsu.nativewrap;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;



public class WrapMain extends Activity {
	static String logTag = "NativeWrap";
	static String rulesetPath;
	TextView fromRule;
	TextView toRule;
	CheckBox forceHTTPS;
	EditText URLEdit;
	@Override
	public void onBackPressed() {
	    moveTaskToBack(true);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wrap_main);		
	}
    protected void onStart()
    {
    	super.onStart();
    	rulesetPath="ruleset.xml";
    	//Strings to store the rule, in case its available.
		String countFilePath = getFilesDir()+"/countFile.txt";
		//Getting the count of wrapped packages installed.
		File countFile = new File(countFilePath);
		int appNumber = 0;
		if(!countFile.isFile())
		{	
			//Creating the count file
			try{
				countFile.createNewFile();
				//Iterate till package does not exist, in case NativeWrap is uninstalled but the 
				//apps it has created still exist.
				while(packageExists("com.demoapps.adwait"+appNumber))
					appNumber++;
				Writer wr = new FileWriter(countFile);
				wr.write((appNumber)+"");
				wr.close();
			}
			catch(Exception e)
			{
				Log.d(logTag,"Exception while creating file:"+countFile);
			}
		}
		else{
			try {
				//Reading the appNumber from the file
				FileInputStream fis = new FileInputStream(countFile);
				// Get the object of DataInputStream
				DataInputStream in = new DataInputStream(fis);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				appNumber = Integer.parseInt(br.readLine());
				//Iterate till package does not exist, in case WrapMain is uninstalled but the 
				//apps it has created still exist.
				//Then incrementing it by 1 and writing it to the file again.
				while(packageExists("com.demoapps.adwait"+appNumber))
					appNumber++;
				countFile.delete();
				countFile.createNewFile();
				Writer wr = new FileWriter(countFile);
				wr.write((appNumber)+"");				
				if(wr!=null) wr.close();
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d(logTag,"AppNumber = "+appNumber);
		}
		
		final String packagename = "com.demoapps.adwait"+appNumber;
		//When the make app button is clicked
		final Button button = (Button) findViewById(R.id.make);
		final EditText mEdit   = (EditText)findViewById(R.id.editText1);
		//final EditText URLEdit = (EditText)findViewById(R.id.editText2);
		final CheckBox SameOriginCheck = (CheckBox)findViewById(R.id.sameOrigin);
		//final CheckBox forceHTTPS=(CheckBox)findViewById(R.id.forceHTTPS);
		final CheckBox readExternalCheck=(CheckBox)findViewById(R.id.readExternal);
		final CheckBox writeExternalCheck=(CheckBox)findViewById(R.id.writeExternal);
		//final TextView fromRule=(TextView)findViewById(R.id.fromRule);
		//final TextView toRule=(TextView)findViewById(R.id.toRule);
		final Context context=getApplicationContext();
		fromRule=(TextView)findViewById(R.id.fromRule);
		toRule=(TextView)findViewById(R.id.toRule);
		forceHTTPS=(CheckBox)findViewById(R.id.forceHTTPS);
		URLEdit = (EditText)findViewById(R.id.editText2);
		forceHTTPS.setVisibility(View.INVISIBLE);
		
		Intent intent=getIntent();
		
		if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
	        if ("text/plain".equals(intent.getType())) {
	            URLEdit.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
	        }
		}
		
		/*If there is a rule for the URL, enable the forceHTTPS option.
		 * 1. Check if there is a rule for the URL.
		 * 2. Display the enabled forceHTTPS option.
		*/
		//Call the asynctask that checks for httpseverywhere url match
		new HTTPSEverywhereMatch().execute();
		
		//When ForceHTTPS checkbox is checked/unchecked
		forceHTTPS.setOnClickListener(new OnClickListener() {
		      @Override
		      public void onClick(View v) {
		        if(forceHTTPS.isChecked()) {
    				new HTTPSEverywhereMatch().execute();
		        }
		        else if(!forceHTTPS.isChecked()){
		        	//If unchecked, do not package the rule
		        	fromRule.setText("from");
		        	toRule.setText("to");
		        } 
		      }
		    });
		
		//When Make APK is clicked
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String appname = mEdit.getText().toString();
                String url = URLEdit.getText().toString();
                boolean sameOrigin=false;
                boolean readExternal=false;
                boolean writeExternal=false;
                if (SameOriginCheck.isChecked())
                	sameOrigin=true;
                if(readExternalCheck.isChecked())
                	readExternal=true;
                if(writeExternalCheck.isChecked())
                	writeExternal=true;
                if(appname==null || appname.equals(""))
                {
                	appname = packagename;
                } 
                if(url==null || url.equals(""))
                {
                	url = "https://www.google.com";
                }          
                //Sending an intent to the AppMakerActivity
                Intent explicitIntent = new Intent();
                explicitIntent.setClass(getApplicationContext(), AppMakerActivity.class);
                explicitIntent.putExtra("packagename", packagename);
                explicitIntent.putExtra("appname", appname);
                explicitIntent.putExtra("url", url);
                explicitIntent.putExtra("sameorigin", sameOrigin); 
                explicitIntent.putExtra("readExternal", readExternal);
                explicitIntent.putExtra("writeExternal", writeExternal);
                String fromRuleText = ""+ fromRule.getText();
                String toRuleText = "" + toRule.getText();
                if(!fromRuleText.equals("from"))
                	explicitIntent.putExtra("fromRule", fromRuleText);
                if(!toRuleText.equals("to"))
                	explicitIntent.putExtra("to", toRuleText);
                Log.d(logTag,"Appname ="+appname+" Packagename="+packagename+" URL="+url+" readExt="+readExternal +" writeExt="+writeExternal);
                startActivity(explicitIntent);   
            }
        });
    }
	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_wrap_main, menu);
		return true;
	}*/
	
	//Checking if a package exists on the phone
	public boolean packageExists(String targetPackage){
        List<ApplicationInfo> packages;
        PackageManager pm = getPackageManager();        
        packages = pm.getInstalledApplications(0);    
        for (ApplicationInfo packageInfo : packages) {
        	if(packageInfo.packageName.equals(targetPackage)) 
        		return true;
        }        
        return false;
    }
	
	public InputStream getInputStreamForAsset(String assetName)
	{
        AssetManager assetManager= getAssets();
        InputStream is=null;
		try {
			is = assetManager.open(assetName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return is;
	}
	
	//Checking the HTTPSEverywhere ruleset and updating the view.
	public void setRuleText(String urlEdit, String fromRule, String toRule){
		this.URLEdit.setText(urlEdit);	
		this.fromRule.setText(fromRule);
		this.toRule.setText(toRule);
		this.forceHTTPS.setVisibility(View.VISIBLE);
		//Log.d(logTag, "Rule from:"+fromRule+"| to:"+toRule+"| url:"+urlEdit);
	}
	
	private class HTTPSEverywhereMatch extends AsyncTask<Void, Void, Void> {
    	private ProgressDialog progress;
    	private boolean match;
    	private String urlEdit;
    	private String fromRule;
    	private String toRule;
    	
	    @Override
	    protected void onPreExecute() {
	    	match=false;
	    	urlEdit=fromRule=toRule=null;
	    	progress = new ProgressDialog(WrapMain.this);
			progress.setTitle("Setting up the environment.");
			progress.setMessage("Please wait...");
			progress.setCancelable(false);
			//dialog.setIndeterminate(true);
			progress.show();
	    }
	    @Override
	    protected Void doInBackground(Void... arg0) {		
	    	try{
	    		String urlbeforeclick = URLEdit.getText().toString();
	    		InputStream in=WrapMain.this.openFileInput(rulesetPath);
	    		String[] array = URLRuleMatcher.getForceHTTPSUrl(urlbeforeclick, in);
	    		if((array[0]) != null)
	    		{
	    			match=true;
	    			urlEdit=array[0];
	    			fromRule=array[1];
	    			toRule=array[2];			
	    		}
	    		if(in!=null) in.close();
	    	}
	    	catch(Exception e)
	    	{
	    		Log.w(logTag,"Exception while checking for forceHTTPSURL"+e);
	    	}
			return null;
		}
	    @SuppressLint("NewApi")
		protected void onPostExecute(Void result) {
	        if (progress.isShowing()) {
	        	progress.dismiss();
	        }
	        if(match)
	        	WrapMain.this.setRuleText(urlEdit, fromRule, toRule);
	    }
	}
}