/*
 * Copyright (c) 2014, North Carolina State University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of North Carolina State University nor the names of
 * its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */

package edu.ncsu.nativewrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import kellinwood.security.zipsigner.ZipSigner;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;

import org.apache.commons.io.FileUtils;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.AxmlWriter;
import pxb.android.axml.AxmlReader;

public class AppMakerActivity extends Activity {
	final static String packagetoReplace = "com.example.containerapp";
	final static String appnametoReplace = "ContainerApp";
	static boolean readExternal=false;
	static boolean writeExternal=false;
	static String fromRule=null;
	static String toRule=null;
	static String logTag="NativeWrap";
	static boolean setFavicon=true;
	Context context;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context=this;
		//Getting data from the intent
		Intent received_intent = getIntent();
		String packagename = received_intent.getStringExtra("packagename");
	    String appname = received_intent.getStringExtra("appname");	
	    readExternal=received_intent.getBooleanExtra("readExternal", false);
	    writeExternal=received_intent.getBooleanExtra("writeExternal", false);
	    setFavicon=received_intent.getBooleanExtra("setFavicon", true);
	    fromRule=received_intent.getStringExtra("fromRule");
	    toRule=received_intent.getStringExtra("toRule");
	    Log.i(logTag, "readExternal="+readExternal+" writeExternal="+writeExternal);
	    Log.d(logTag,"PackageName:Appname = "+packagename+":"+appname);
	    
	    //Common objects
		File path2 = getFilesDir();
		  
		//Getting the default apk from assets		
		AssetManager assetManager = getAssets();
		try { 
		    InputStream fis = assetManager.open("default-app.apk");
		    FileUtils.copyInputStreamToFile(fis, new File(getFilesDir()+"/default-app.apk"));
		    if(fis!=null) fis.close();
		    
		    //*****************
		    //Extracting the AndroidManifest.xml file from the APK
		    extractAPK(new File(getFilesDir()+"/default-app.apk"), "AndroidManifest.xml", getFilesDir()+"/");
		    //*****************
		    //Modifying AndroidManifest.xml	
		    //Converting .xml to byte[], and then deleting the AndroidManifest.xml
		    File manifest = new File(getFilesDir()+"/AndroidManifest.xml");
		    byte[] orgData = FileUtils.readFileToByteArray(manifest);
		    manifest.delete();
		    //Calling library function to Modify the byte[] version of the manifest, and 
		    //writing the modified byte[] to AndroidManifest.xml again.
		    byte[] fixedData =modifyAxml(orgData, packagename, appname);
		    File manifest2 = new File(getFilesDir()+"/AndroidManifest.xml");
		    FileUtils.writeByteArrayToFile(manifest2, fixedData);
		    //******************
		    //Creating a file to hold the url and adding it to the apk		  
			File urlFile = new File(path2+"/default_url.xml");
			//Writing the file to hold the url
			urlFile.delete();
	        urlFile.createNewFile();
			Writer wr = new FileWriter(urlFile);
			wr.write(received_intent.getStringExtra("url"));
			wr.write("\n"+received_intent.getBooleanExtra("sameorigin",false));
			if(fromRule!=null&&toRule!=null){
				wr.write("\n"+fromRule+"\n"+toRule);
			}
			wr.close();
			
			//*****************
			//Extracting favicons if possible
			File iconFiles[]=null;
			if(setFavicon)
			{
				try{
					URL receivedURL = new URL(received_intent.getStringExtra("url"));
					URL url = new URL(receivedURL.getProtocol()+"://"+receivedURL.getHost()+"/favicon.ico");
					System.out.println("GETTING BITMAP");
					Bitmap favicon = getBitmapFromURL(url);	
					iconFiles = new File[4];
					Bitmap lowDensity = convertBitmap(context, favicon, DisplayMetrics.DENSITY_LOW);
					writeBitmapToFile(getFilesDir()+"/ic_launcher_low.png",lowDensity);
					iconFiles[0]=new File(getFilesDir()+"/ic_launcher_low.png");
					Bitmap mediumDensity = convertBitmap(context, favicon, DisplayMetrics.DENSITY_MEDIUM);
					writeBitmapToFile(getFilesDir()+"/ic_launcher_medium.png",mediumDensity);
					iconFiles[1]=new File(getFilesDir()+"/ic_launcher_medium.png");
					Bitmap highDensity = convertBitmap(context, favicon, DisplayMetrics.DENSITY_HIGH);
					writeBitmapToFile(getFilesDir()+"/ic_launcher_high.png",highDensity);
					iconFiles[2]=new File(getFilesDir()+"/ic_launcher_high.png");
					Bitmap xhighDensity = convertBitmap(context, favicon, DisplayMetrics.DENSITY_XHIGH);
					writeBitmapToFile(getFilesDir()+"/ic_launcher_xhigh.png",xhighDensity);
					iconFiles[3]=new File(getFilesDir()+"/ic_launcher_xhigh.png");
				
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		
			
		    //*****************
		    //Adding the modified AndroidManifest.xml back to the apk file
		    File zipFile = new File(getFilesDir()+"/default-app.apk");
		    File filearray[]=new File[2];
		    filearray[0]=manifest2;
		    filearray[1]=urlFile;
		    addFilesToExistingZip(zipFile,filearray, iconFiles);
		    manifest2.delete();
		    //******************
		    //Signing the apk using zipsigner lib
		    try {
		        // Sign with the built-in default test key/certificate.
		        ZipSigner zipSigner = new ZipSigner();
		        zipSigner.setKeymode("testkey");
		        zipSigner.signZip( getFilesDir()+"/default-app.apk", getFilesDir()+"/final.apk"); 		    
		        //Copying to sdcard
		        FileInputStream fis3 = new FileInputStream(getFilesDir()+"/final.apk");
		        //FileUtils.copyInputStreamToFile(fis3, new File(path+"/"+appname+".apk"));	
		        FileOutputStream fos3 = openFileOutput(appname+".apk", Context.MODE_WORLD_READABLE);
		        byte[] buf = new byte[1024];
		        int len;
		        while ((len = fis3.read(buf)) > 0) {
		        	fos3.write(buf, 0, len);
		        }
		        //System.out.println("Copied to STORAGE");
		        if(fis3!=null) fis3.close();
		        if(fos3!=null) fos3.close();
		    }
		    catch (Exception e) {
		        Log.d(logTag,"Exception while signing or copying:"+e);
		        
		    }
		    
		    //Installing the signed final.apk from the storage
		    Intent intent = new Intent(Intent.ACTION_VIEW);
		    intent.setDataAndType(Uri.fromFile(new File(getFilesDir()+"/"+appname+".apk")), "application/vnd.android.package-archive");
		    startActivity(intent);
		    File todelete1 = new File(getFilesDir()+"/default-app.apk");
		    File todelete2 = new File(getFilesDir()+"/final.apk");
		    todelete1.delete();
		    todelete2.delete();
		    this.finish();
		    //******************
	    }catch (IOException e1) {
		    e1.printStackTrace();		 
		}

	}
		
	/*Add a certain File(s) to the archive, exactly what I want.*/
	public static void addFilesToExistingZip(File zipFile,
		 File[] files, File[] iconFiles) throws IOException {
               // get a temp file
		File tempFile = File.createTempFile(zipFile.getName(), null);
               // delete it, otherwise you cannot rename your existing zip to it.
		tempFile.delete();
		boolean renameOk=zipFile.renameTo(tempFile);
		if (!renameOk)
		{
			throw new RuntimeException("could not rename the file "+zipFile.getAbsolutePath()+" to "+tempFile.getAbsolutePath());
		}
		byte[] buf = new byte[1024];
	 
		ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
	
		ZipEntry entry = zin.getNextEntry();
	
		int xmlcount=0;
		boolean ldpi=false, mdpi=false, hdpi=false, xhdpi=false;
		while (entry != null) {
			String name = entry.getName();
			boolean notInFiles = true;
			//Adding default_url.xml
			if(name.equals("assets/default_url.xml"))
			{
				if(xmlcount==0)
				{	xmlcount=1;
					//System.out.println("___________________Modifying content for "+name);
					InputStream inxml = new FileInputStream(files[files.length-1]);
					out.putNextEntry(new ZipEntry(name));
					// Transfer bytes from the ZIP file to the output file
					int len;
					while ((len = inxml.read(buf)) > 0) {
						out.write(buf, 0, len);
					} 
					out.closeEntry();
				}
			}
			else if(name.equals("res/drawable-ldpi/ic_launcher.png") && iconFiles!=null && iconFiles[0]!=null){
				if(!ldpi){
					ldpi=true;
					//System.out.println("___________________Modifying content for "+name);
					InputStream inxml = new FileInputStream(iconFiles[0]);
					out.putNextEntry(new ZipEntry(name));
					// Transfer bytes from the ZIP file to the output file
					int len;
					while ((len = inxml.read(buf)) > 0) {
						out.write(buf, 0, len);
					} 
					out.closeEntry();
				}
			}
			else if(name.equals("res/drawable-mdpi/ic_launcher.png") && iconFiles!=null && iconFiles[1]!=null){
				if(!mdpi){
					mdpi=true;
					//System.out.println("___________________Modifying content for "+name);
					InputStream inxml = new FileInputStream(iconFiles[1]);
					out.putNextEntry(new ZipEntry(name));
					// Transfer bytes from the ZIP file to the output file
					int len;
					while ((len = inxml.read(buf)) > 0) {
						out.write(buf, 0, len);
					} 
					out.closeEntry();
				}
			}
			else if(name.equals("res/drawable-hdpi/ic_launcher.png") && iconFiles!=null && iconFiles[2]!=null){
				if(!hdpi){
					hdpi=true;
					//System.out.println("___________________Modifying content for "+name);
					InputStream inxml = new FileInputStream(iconFiles[2]);
					out.putNextEntry(new ZipEntry(name));
					// Transfer bytes from the ZIP file to the output file
					int len;
					while ((len = inxml.read(buf)) > 0) {
						out.write(buf, 0, len);
					} 
					out.closeEntry();
				}
			}
			else if(name.equals("res/drawable-xhdpi/ic_launcher.png") && iconFiles!=null && iconFiles[3]!=null){
				if(!xhdpi){
					xhdpi=true;
					//System.out.println("___________________Modifying content for "+name);
					InputStream inxml = new FileInputStream(iconFiles[3]);
					out.putNextEntry(new ZipEntry(name));
					// Transfer bytes from the ZIP file to the output file
					int len;
					while ((len = inxml.read(buf)) > 0) {
						out.write(buf, 0, len);
					} 
					out.closeEntry();
				}
			}
			//If this file is not among the ones to overwrite, write it to the zip as it is.
			for (File f : files) {
				if (f.getName().equals(name) || name.equals("assets/default_url.xml")) {
					//System.out.println("******************************************Found File "+name);				
					notInFiles = false;
					break;
				}
				
				if (notInFiles) {
					if(name.equals("assets/default_url.xml"))
					{}
					else if(iconFiles!=null && (name.equals("res/drawable-ldpi/ic_launcher.png") || name.equals("res/drawable-mdpi/ic_launcher.png") ||name.equals("res/drawable-hdpi/ic_launcher.png") || name.equals("res/drawable-xhdpi/ic_launcher.png")))
					{}
					else
					{	
						try{
							// Add ZIP entry to output stream.
							out.putNextEntry(new ZipEntry(name));
							// Transfer bytes from the ZIP file to the output file
							int len;
							while ((len = zin.read(buf)) > 0) 
							{
								out.write(buf, 0, len);
							}
							out.closeEntry();
						}
						catch(Exception e)
						{}
					}
				}
			}
			entry = zin.getNextEntry();	
		}	
	
		// Close the streams		
		zin.close();
		// Writing the AndroidManifest.xml
		//I added -1 to the condition so that the default_url file is not written
		for (int i = 0; i < files.length-1; i++) {
			InputStream in = new FileInputStream(files[i]);
			// Add ZIP entry to output stream.
			out.putNextEntry(new ZipEntry(files[i].getName()));
			// Transfer bytes from the file to the ZIP file
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			// Complete the entry
			out.closeEntry();
			in.close();
		}
		
		out.close(); 
		tempFile.delete();
	}

	//******************

	  //Utility Function for modifying a binary xml
	public byte[] modifyAxml(byte[] orgData, final String packagename, final String appname) throws IOException {
	    AxmlReader ar = new AxmlReader(orgData);
		AxmlWriter aw = new AxmlWriter();
		ar.accept(new AxmlVisitor(aw) {	
			@Override
		    public NodeVisitor first(String ns, String name) {// manifest	  
				NodeVisitor nv = super.first(ns, name);
		        return new NodeVisitor(nv) {
		        	//Adding a permission
		            @Override
		            public void end() {
		            	//add permissions here; INTERNET is already present by default
		            	if(readExternal) {
		            		NodeVisitor read_storage = super.child(null, "uses-permission");
		                    read_storage.attr("http://schemas.android.com/apk/res/android", "name", 0x1010003,
		                              TYPE_STRING, "android.permission.READ_EXTERNAL_STORAGE");
		                }
		            	if(writeExternal) {
		                    NodeVisitor write_storage = super.child(null, "uses-permission");
		                    write_storage.attr("http://schemas.android.com/apk/res/android", "name", 0x1010003,
		                              TYPE_STRING, "android.permission.WRITE_EXTERNAL_STORAGE");
		                }
		                super.end();
		            }
		        	public void attr(String ns, String name, int resourceId, int type, Object obj) {
	                    if ((ns==null)&& name.equals("package")) {
	                        String pname = (String) obj;
	                        obj = pname.replace(packagetoReplace, packagename);// change packagename
	                        //System.out.println("Package: Replacing "+packagetoReplace+" with "+packagename);
	                        super.attr(ns, name, resourceId, type, obj);
	                    } 
	                    else {
	                        super.attr(ns, name, resourceId, type, obj);
	                    }
	                }
		            @Override
		            public NodeVisitor child(String ns, String name) {
		        	// application
		            	NodeVisitor nv = super.child(ns,name);
		            	return new NodeVisitor(nv) {
		            		
		            	@Override
	                    	public void attr(String ns, String name, int resourceId, int type, Object obj) {
		            			if ("http://schemas.android.com/apk/res/android".equals(ns)
		            					&& name.equals("label")) {
		            			try{ 
		            				String applicationClass = (String) obj;
		            				obj = applicationClass.replace(appnametoReplace, appname);// change application name
		            				//System.out.println("Application: Replacing "+appnametoReplace+" with "+appname);
		            				super.attr(ns, name, resourceId, type, obj);
		            			}
		            			catch(Exception e) {
		            				Log.d(logTag,"Exception while modifying application label:"+e);
		            			}
		            			} else {
		            				super.attr(ns, name, resourceId, type, obj);
		            			}
	                    	}
		            	@Override
		            	public NodeVisitor child(String ns, String name) {// activity,receiver
		            		if (name.equals("activity")) {		            		
		            			return new NodeVisitor(super.child(ns, name)) {
		            				@Override
		            				public void attr(String ns, String name, int resourceId, int type, Object obj) {
		            					if ("http://schemas.android.com/apk/res/android".equals(ns)
		            							&& name.equals("label")) {
		            					try{ 
		            						String activityClass = (String) obj;
		            						obj = activityClass.replace(appnametoReplace, appname);// change activity class name
		            						//System.out.println("Activity: Replacing "+appnametoReplace+" with "+appname);
		            						super.attr(ns, name, resourceId, type, obj);
		            					}
		            					catch(Exception e) {
		            						Log.d(logTag,"Exception while modifying activity label:"+e);
		            					}
		            					} else {
		            						super.attr(ns, name, resourceId, type, obj);
		            					}
		            				}
		            			};
		            		}
		                
		            		if (name.equals("service")) {
		            			return new NodeVisitor(super.child(ns, name)) {
		            				@Override
		            				public void attr(String ns, String name, int resourceId, int type, Object obj) {
		            					if ("http://schemas.android.com/apk/res/android".equals(ns)
		            							&& name.equals("label")){
		            					super.attr(ns, name, resourceId, type, obj);
		            					}
		            					else 
		            						super.attr(ns, name, resourceId, type, obj);			                      
		            				}
		            			};
		            		}
		            		if (name.equals("receiver")) {
		            			return new NodeVisitor(super.child(ns, name)) {
			                  };
			                }
		            		if (name.equals("provider")) {
		            		}
		            		return super.child(ns, name);
		            		}
		            	}; 
		            }	          
		        };		        
			}      
		});
		byte[] data = aw.toByteArray();
		// save data
        return data;
	}

	//Utility Function for extracting an APK 
	public static void extractAPK(File source, String toExtract, String destDir) {
		try{
			//System.out.println("Extracting Manifest from APK now");
			java.util.jar.JarFile jarfile = new java.util.jar.JarFile(source); //jar file path(here sqljdbc4.jar)
			java.util.Enumeration<java.util.jar.JarEntry> enu= jarfile.entries();
			while(enu.hasMoreElements()) {
				String destdir = destDir;     //abc is my destination directory
		        java.util.jar.JarEntry je = enu.nextElement();
		        //System.out.println(je.getName());
		        if(!je.getName().equals(toExtract))
		        	continue;
		        java.io.File fl = new java.io.File(destdir + je.getName());
		        if(!fl.exists())
		        {
		            fl.getParentFile().mkdirs();
		            fl = new java.io.File(destdir + je.getName());
		        }
		        if(je.isDirectory())
		        {
		            continue;
		        }
		        java.io.InputStream is = jarfile.getInputStream(je);
		        java.io.FileOutputStream fo = new java.io.FileOutputStream(fl);
		        while(is.available()>0)
		        {
		            fo.write(is.read());
		        }
		        fo.close();
		        is.close();
			  }
		}
		catch(Exception e)
		{
			Log.d(logTag,"Exception while extracting APK"+e);
		}
	}

	//Extracting and formatting the favicons
	public static Bitmap getBitmapFromURL(URL src) {
		try {
			URL url = src;
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setDoInput(true);
	        connection.connect();
	        InputStream input = connection.getInputStream();
	        Bitmap myBitmap = BitmapFactory.decodeStream(input);
			System.out.println("GOT BITMAP:"+myBitmap.getHeight()+"|"+myBitmap.getWidth());
	        return myBitmap;
	    } catch (IOException e) {
	    	e.printStackTrace();
	        return null;
	    }
	}
		
	public static Bitmap convertBitmap(Context context, Bitmap favicon, int density){
		Bitmap bitmap=null;
		BitmapDrawable drawable=new BitmapDrawable(context.getResources(),favicon);
		//drawable.setTargetDensity(density);
		int width=0,height=0;
		switch(density){
		case DisplayMetrics.DENSITY_LOW:
			width=height=32;
			break;
		case DisplayMetrics.DENSITY_MEDIUM:
			width=height=48;
			break;
		case DisplayMetrics.DENSITY_HIGH:
			width=height=72;
			break;
		case DisplayMetrics.DENSITY_XHIGH:
			width=height=96;
			break;
		}
		bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
	    Canvas canvas = new Canvas(bitmap); 
	    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
	    drawable.draw(canvas);
	    System.out.println("Converted to BITMAP:"+bitmap.getHeight()+"|"+bitmap.getWidth());
		return bitmap;
	}
	
	public static void writeBitmapToFile(String path, Bitmap bitmap){
		OutputStream stream;
		try {
			stream = new FileOutputStream(path);
			bitmap.compress(CompressFormat.PNG, 100, stream);
			stream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
