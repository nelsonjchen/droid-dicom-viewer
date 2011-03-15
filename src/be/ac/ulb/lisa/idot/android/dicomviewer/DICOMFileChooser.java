/*
 *
 * Copyright (C) 2011 Pierre Malarme
 *
 * Authors: Pierre Malarme <pmalarme at ulb.ac.be>
 *
 * Institution: Laboratory of Image Synthesis and Analysis (LISA)
 *              Faculty of Applied Science
 *              Universite Libre de Bruxelles (U.L.B.)
 *
 * Website: http://lisa.ulb.ac.be
 *
 * This file <DICOMFileChooser.java> is part of Droid Dicom Viewer.
 *
 * Droid Dicom Viewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Droid Dicom Viewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Droid Dicom Viewer. If not, see <http://www.gnu.org/licenses/>.
 *
 * Released date: 17-02-2011
 *
 * Version: 1.0
 *
 */

package be.ac.ulb.lisa.idot.android.dicomviewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import be.ac.ulb.lisa.idot.android.commons.ExternalStorage;
import be.ac.ulb.lisa.idot.android.dicomviewer.thread.DICOMImageCacher;
import be.ac.ulb.lisa.idot.android.dicomviewer.thread.ThreadState;
import be.ac.ulb.lisa.idot.dicom.data.DICOMMetaInformation;
import be.ac.ulb.lisa.idot.dicom.file.DICOMReader;

/**
 * File chooser.
 * 
 * @author Pierre Malarme
 * @version 1.0
 *
 */
public class DICOMFileChooser extends ListActivity {
	
	// ---------------------------------------------------------------
	// - <static> VARIABLES
	// ---------------------------------------------------------------
	
	/**
	 * Id for the onSaveInstanceState.
	 */
	private static final String TOP_DIR_ID = "top_directory";
	
	/**
	 * Menu id for DICOM images caching.
	 */
	private static final short MENU_CACHE_IMAGE = 0;
	
	/**
	 * Menu for the displaying of the about dialog.
	 */
	private static final short MENU_ABOUT = 1;

    /**
     * Menu for Preferences Activity
     */
    private static final short MENU_PREFERENCES = 2;
	
	/**
	 * Define the progress dialog id for the caching of
	 * DICOM image.
	 */
	private static final short PROGRESS_DIALOG_CACHE = 1;
	
	
	// ---------------------------------------------------------------
	// - VARIABLES
	// ---------------------------------------------------------------
	
	/**
	 * Current directory.
	 */
	private File mTopDirectory;
	
	/**
	 * Array adapter to display the
	 * directory and the files.
	 */
	ArrayAdapter<String> mAdapter;
	
	/**
	 * DICOM file in mTopDirectory count.
	 */
	private int mTotal = 0;
	
	/**
	 * TextView to display the cached files count.
	 */
	private TextView mCachedFileTextView;
	
	/**
	 * File chooser main layout.
	 */
	private LinearLayout mMainLayout;
	
	/**
	 * Progress dialog for file caching.
	 */
	private ProgressDialog cachingDialog;
	
	
	// ---------------------------------------------------------------
	// # <override> FUNCTIONS
	// ---------------------------------------------------------------

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		// Set the content view
		setContentView(R.layout.file_chooser_list);
		
		// Define the main layout
		mMainLayout = (LinearLayout) findViewById(R.id.file_chooser_mainLayout);
		
		// Defined the cached file TextView (even if it is not
		// shown) and define the on click listener
		mCachedFileTextView = new TextView(this);
		mCachedFileTextView.setPadding(10, 20, 10, 20);
		mCachedFileTextView.setBackgroundColor(0xffffffff);
		mCachedFileTextView.setTextColor(0xff000000);
		mCachedFileTextView.setTextSize(14.0f);
		mCachedFileTextView.setClickable(true);
		mCachedFileTextView.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				
				AlertDialog.Builder builder = new AlertDialog.Builder(DICOMFileChooser.this);
				builder.setMessage("Do you want to delete cached files ?")
					   .setTitle("Cached file")
				       .setCancelable(true)
				       .setPositiveButton("Cancel", null)
				       .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                DICOMFileChooser.this.deleteCachedFile();
				           }
				       });
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
				
			}
		});		
		
		// Check if the external storage is available
		if (ExternalStorage.checkAvailable()) {
			
			if (savedInstanceState != null) {
				String topDirectoryString = savedInstanceState.getString(TOP_DIR_ID);
				
				mTopDirectory = (topDirectoryString == null) ? Environment.getExternalStorageDirectory()
						: new File(savedInstanceState.getString("top_directory"));
			} else {
				// Set the top directory
				mTopDirectory = Environment.getExternalStorageDirectory();
				
				// Display the disclaimer
                SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(this);
                if(spref.getBoolean("hide_disclaimer",false) != true) {
                    displayDisclaimer();
                }
			}
			
		}
		
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		
		// If there is no external storage available, quit the application
		if (!ExternalStorage.checkAvailable()) {
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("There is no external storage.\n"
						+ "1) There is no external storage : add one.\n"
						+ "2) Your external storage is used by a computer:"
						+ " Disconnect the it from the computer.")
				   .setTitle("[ERROR] No External Storage")
			       .setCancelable(false)
			       .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                DICOMFileChooser.this.finish();
			           }
			       });
			
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		
		// Else display data
		} else {
			
			fill();
			
		}
		
		super.onResume();
		
	}

	/* (non-Javadoc)
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		super.onListItemClick(l, v, position, id);
		
		String itemName = mAdapter.getItem(position);
		
		// If it is a directory, display its content
		if (itemName.charAt(0) == '/') {
			
			 mTopDirectory = new File(mTopDirectory.getPath() + itemName);
			 
			 fill();
			 
		// If itemNam = ".." go to parent directory
		} else if (itemName.equals("..")) {
			
			mTopDirectory = mTopDirectory.getParentFile();
			
			fill();
			
		// If it is a file.
		} else {
			
			try {
				
				// Create a DICOMReader to parse meta info
				DICOMReader dicomReader = new DICOMReader(mTopDirectory.getPath() + "/" + itemName);
				
				DICOMMetaInformation metaInformation = dicomReader.parseMetaInformation();
				
				dicomReader.close();
				
				if (metaInformation.getSOPClassUID().equals("1.2.840.10008.1.3.10")) {
					
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setMessage("Media Storage Directory (DICOMDIR) are not supported yet.")
						   .setTitle("[ERROR] Opening file " + itemName)
					       .setCancelable(false)
					       .setPositiveButton("Close", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					                // Do nothing
					           }
					       });
					AlertDialog alertDialog = builder.create();
					alertDialog.show();
					
				} else {
					
					// Open the DICOM Viewer
					Intent intent = new Intent(this, DICOMFileInfo.class);
					intent.putExtra("DICOMFileName", mTopDirectory.getPath() + "/" + itemName);
					intent.putExtra("FileCount", mTotal);
					startActivity(intent);
					
				}
				
			} catch (Exception ex) {
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Error while opening the file " + itemName
						+ ". \n" + ex.getMessage())
					   .setTitle("[ERROR] Opening file " + itemName)
				       .setCancelable(false)
				       .setPositiveButton("Close", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                // Do nothing
				           }
				       });
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
				
			}
			
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// Save the top directory absolute path
		outState.putString(TOP_DIR_ID, mTopDirectory.getAbsolutePath());
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		// TODO : cancelable dialog ?
       
		switch(id) {
		
		// Create image cache dialog
        case PROGRESS_DIALOG_CACHE:
            cachingDialog = new ProgressDialog(this);
            cachingDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            cachingDialog.setMessage("Caching image...");
            cachingDialog.setCancelable(false);
            return cachingDialog;
        	
        default:
            return null;
            
        }
		
    }
	
	
	// ---------------------------------------------------------------
	// + <override> FUNCTIONS
	// ---------------------------------------------------------------

	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		
		// If the directory is the external storage directory or there is no parent,
		// super.onBackPressed(). Else go to parent directory.
		if (mTopDirectory.getParent() == null
				|| mTopDirectory.equals(Environment.getExternalStorageDirectory())) {
			
			super.onBackPressed();
			
		} else {
		
			mTopDirectory = mTopDirectory.getParentFile();
			
			fill();
			
		}
		
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_CACHE_IMAGE, 0, "Cache all images");
        menu.add(0, MENU_PREFERENCES,0, "Preferences");
		menu.add(0, MENU_ABOUT, 1, "About");
		
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		switch (item.getItemId()) {
		
		case MENU_CACHE_IMAGE:
			cacheImages();
			return true;
			
		case MENU_ABOUT:
			Dialog dialog = new Dialog(this);
        	dialog.setContentView(R.layout.dialog_about);
        	dialog.setTitle("Droid Dicom Viewer: About");
        	dialog.show();
			return true;
        case MENU_PREFERENCES:
            startActivity(new Intent(this,DICOMFileChooserPreferences.class));
            return true;
		default:
			return super.onMenuItemSelected(featureId, item);
			
		}
		
	}
	
		
	// ---------------------------------------------------------------
	// - FUNCTIONS
	// ---------------------------------------------------------------

	/**
	 * Update the content of the view.
	 */
	private void fill() {
		
		// If the external storage is not available, we cannot
		// fill the view
		if (!ExternalStorage.checkAvailable())
			return;
		
		// Remove the cached file text view form the layout
		if (mMainLayout.indexOfChild(mCachedFileTextView) != -1)
			mMainLayout.removeView(mCachedFileTextView);
		
		
		// Cached file counter
		int cachedImageCount = 0;
		
		// Get the children directories and the files of top directories 
		File[] childrenFiles = mTopDirectory.listFiles();
		
		// Declare the directories and the files array
		List<String> directoryList = new ArrayList<String>();
		List<String> fileList = new ArrayList<String>();
		
		// Loop on all children
		for (File child: childrenFiles) {
			
			// If it is a directory
			if (child.isDirectory()) {
				
				String directoryName = child.getName();
				if (directoryName.charAt(0) != '.')
					directoryList.add("/" + child.getName());
			
			// If it is a file.
			} else {
				
				String[] fileName = child.getName().split("\\.");
				
				if (!child.isHidden()) {
					
					if (fileName.length > 1) {
						
						// DICOM files have no extension or dcm extension
						if (fileName[fileName.length-1].equals("dcm")) {
							
							fileList.add(child.getName());
							
						// Else if it is a LISA image, count the cached image.
						} else if (fileName[fileName.length-1].equals("lisa")) {
							
							cachedImageCount++;
							
						}
						
					} else {
						
						fileList.add(child.getName());
						
					}
					
				}
				
			}
			
		}
		
		// Sort both list
		Collections.sort(directoryList,String.CASE_INSENSITIVE_ORDER);
		Collections.sort(fileList,String.CASE_INSENSITIVE_ORDER);

		// Set the number of dicom file
		mTotal = fileList.size();
		
		// Output list will be files before directories
		// then we add the directoryList to the fileList
		fileList.addAll(directoryList);
		
		if (!mTopDirectory.equals(Environment.getExternalStorageDirectory()))
			fileList.add(0, "..");
		
		// If there is cached filed, display it
		if (cachedImageCount > 0) {
			
			mCachedFileTextView.setText("Cached files: " + cachedImageCount);
			mMainLayout.addView(mCachedFileTextView, 0);
			
		}
		
		mAdapter = new ArrayAdapter<String>(this, R.layout.file_chooser_item, R.id.fileName, fileList);
		
		setListAdapter(mAdapter);
		
	}
	
	/**
	 * Delete cached files present in the mTopDirectory.
	 */
	private void deleteCachedFile() {
		
		if (!ExternalStorage.checkWritable()) {
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Cannot delete cached files because " +
					"the external storage is not writable.")
				   .setTitle("[ERROR] Delete cached file.")
			       .setCancelable(false)
			       .setPositiveButton("Close", null);
			
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
			
			return;
			
		}
		
		// Get the children directories and the files of top directories 
		File[] childrenFiles = mTopDirectory.listFiles();
		
		// Loop on the file of the directory
		for (File child: childrenFiles) {
			
			// If it is not a directory or a hidden file
			if (!child.isDirectory()
					&& child.getName().charAt(0) != '.') {
				
				// Check that the extension is "lisa"
				String[] fileName = child.getName().split("\\.");
				
				// If it is the case, there is a dot in the file name
				if (fileName.length > 1) {
					
					if (fileName[fileName.length-1].equals("lisa")) {
						
						child.delete();
						
					}
					
				}
			}
			
		}
		
		// Update the view
		fill();
		
	}
	
	/**
	 * Cache of the image in the files array
	 */
	private void cacheImages() {
		
		try {
			
			// The handler is inside the function because
			// normally this function is called once.
			final Handler cacheHandler = new Handler() {
				
		        public void handleMessage(Message message) {
		        	
		            switch (message.what) {
		            
		            case ThreadState.STARTED:
		            	cachingDialog.setMax(message.arg1);
		            	break;
		            	
		            case ThreadState.PROGRESSION_UPDATE:
		            	cachingDialog.setProgress(message.arg1);
		            	break;
		            	
		            case ThreadState.FINISHED:
		            	try {
		            		
		            		dismissDialog(PROGRESS_DIALOG_CACHE);
		            		
		            	} catch (IllegalArgumentException ex) {
		            		// Do nothing
		            	}
		            	fill();
		            	break;
		            	
		            case ThreadState.CATCHABLE_ERROR_OCCURRED:
		            	cachingDialog.setProgress(message.arg1);
		            	Toast.makeText(DICOMFileChooser.this, "[Error]: file ("
		            			+ (String) message.obj + ") cannot be cached.", Toast.LENGTH_SHORT).show();
		            	break;
		            	
		            case ThreadState.UNCATCHABLE_ERROR_OCCURRED:
		            	try {
		            		
		            		dismissDialog(PROGRESS_DIALOG_CACHE);
		            		
		            	} catch (IllegalArgumentException ex) {
		            		// Do nothing
		            	}
		            	AlertDialog.Builder builder = new AlertDialog.Builder(DICOMFileChooser.this);
		    			builder.setMessage("Unknown error: An unknown error occurred during"
		    					+ " images caching.")
		    				   .setTitle("[ERROR] Caching file")
		    			       .setCancelable(false)
		    			       .setPositiveButton("Close", null);
		    			AlertDialog alertDialog = builder.create();
		    			alertDialog.show();
		    			break;
		            	
		            case ThreadState.OUT_OF_MEMORY:
		            	try {
		            		
		            		dismissDialog(PROGRESS_DIALOG_CACHE);
		            		
		            	} catch (IllegalArgumentException ex) {
		            		// Do nothing
		            	}
		            	builder = new AlertDialog.Builder(DICOMFileChooser.this);
		    			builder.setMessage("OutOfMemoryError: During the caching of series files,"
		    					+ " an out of memory error occurred.\n\n"
		    					+ "Your file(s) is (are) too large for your Android system. You can"
		    					+ " try again in the file chooser. If the error occured again,"
		    					+ " then the image cannot be displayed on your device.\n"
		    					+ "Try to use the Droid Dicom Viewer desktop file cacher software"
		    					+ " (not available yet).")
		    				   .setTitle("[ERROR] Caching file")
		    			       .setCancelable(false)
		    			       .setPositiveButton("Close", null);
		    			alertDialog = builder.create();
		    			alertDialog.show();
		    			break;
		            	
		            
		            };
		            
		        }
		        
		    };
		    
		    // Show the progress dialog for caching image
			showDialog(PROGRESS_DIALOG_CACHE);
			
		    // Start the caching thread
			DICOMImageCacher dicomImageCacher =
				new DICOMImageCacher(cacheHandler, mTopDirectory);
			
			dicomImageCacher.start();
            
			
		} catch (FileNotFoundException e) {
			
			// TODO display an error ?
			
		}
		
	}
	
	/**
	 * Display the disclaimer.
	 */
	private void displayDisclaimer() {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.about_dialog)
			   .setTitle("Disclaimer")
		       .setCancelable(false)
		       .setPositiveButton("Decline", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   DICOMFileChooser.this.finish();
		           }
		       })
		       .setNegativeButton("Accept", null);
		
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
		
	}

}