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
 * This file <DICOMViewer.java> is part of Droid Dicom Viewer.
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
 * Version: 1.1
 *
 */

package be.ac.ulb.lisa.idot.android.dicomviewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import be.ac.ulb.lisa.idot.android.commons.ExternalStorage;
import be.ac.ulb.lisa.idot.android.dicomviewer.data.DICOMViewerData;
import be.ac.ulb.lisa.idot.android.dicomviewer.mode.CLUTMode;
import be.ac.ulb.lisa.idot.android.dicomviewer.mode.ScaleMode;
import be.ac.ulb.lisa.idot.android.dicomviewer.mode.ToolMode;
import be.ac.ulb.lisa.idot.android.dicomviewer.thread.DICOMImageCacher;
import be.ac.ulb.lisa.idot.android.dicomviewer.thread.ThreadState;
import be.ac.ulb.lisa.idot.android.dicomviewer.view.DICOMImageView;
import be.ac.ulb.lisa.idot.android.dicomviewer.view.GrayscaleWindowView;
import be.ac.ulb.lisa.idot.dicom.data.DICOMImage;
import be.ac.ulb.lisa.idot.dicom.file.DICOMFileFilter;
import be.ac.ulb.lisa.idot.dicom.file.DICOMImageReader;
import be.ac.ulb.lisa.idot.image.data.LISAImageGray16Bit;
import be.ac.ulb.lisa.idot.image.file.LISAImageGray16BitReader;
import be.ac.ulb.lisa.idot.image.file.LISAImageGray16BitWriter;

/**
 * DicomViewer activity that shows an image.
 * 
 * @author Pierre Malarme
 * @version 1.1
 *
 */
public class DICOMViewer extends Activity implements SeekBar.OnSeekBarChangeListener {
	
	// ---------------------------------------------------------------
	// - <static> VARIABLES
	// ---------------------------------------------------------------
	
	// DIALOG
	/**
	 * Define the progress dialog id for the loading of a DICOM image.
	 */
	private static final short PROGRESS_DIALOG_LOAD = 0;
	
	/**
	 * Define the progress dialog id for the caching of
	 * DICOM image.
	 */
	private static final short PROGRESS_DIALOG_CACHE = 1;
	
	// SAVED INSTANCE STATE KEY
	/**
	 * Define the key for savedInstanceState
	 */
	private static final String FILE_NAME = "file_name";
	
	// ---------------------------------------------------------------
	// - VARIABLES
	// ---------------------------------------------------------------
	
	// UI VARIABLES
	
	/**
	 * The image view.
	 */
	private DICOMImageView mImageView;
	
	/**
	 * The tool bar linear layout.
	 */
	private LinearLayout mToolBar;
	
	/**
	 * Set if the tool bar is locked or not.
	 */
	private boolean mLockToolBar = false;
	
	/**
	 * The button to set the mToolMode to
	 * ToolMode.DIMENSION.
	 */
	private Button mDimensionToolButton;

	/**
	 * The button to set the mToolMode to
	 * ToolMode.GRAYSCALE.
	 */
	private Button mGrayscaleToolButton;
	
	/**
	 * Normal CLUT button.
	 */
	private Button mCLUTNormalButton;
	
	/**
	 * Inverse CLUT button. 
	 */
	private Button mCLUTInverseButton;
	
	/**
	 * Rainbow CLUT button.
	 */
	private Button mCLUTRainbowButton;
	
	/**
	 * Lock/unlock tool bar button.
	 */
	private Button mLockUnlockToolBar;
	
	/**
	 * Current tool button. 
	 */
	private Button mCurrentToolButton;
	
	/**
	 * The grayscale window (ImageView).
	 */
	private GrayscaleWindowView mGrayscaleWindow;
	
	/**
	 * Previous button.
	 */
	private Button mPreviousButton;
	
	/**
	 * Next button.
	 */
	private Button mNextButton;
	
	/**
	 * Image index TextView.
	 */
	private TextView mIndexTextView;
	
	/**
	 * Series seek bar.
	 */
	private SeekBar mIndexSeekBar;
	
	/**
	 * Layout representing the series
	 * navigation bar.
	 */
	private LinearLayout mNavigationBar;
	
	/**
	 * Row orientation TextView.
	 */
	private TextView mRowOrientation;
	
	/**
	 * Column orientation TextView.
	 */
	private TextView mColumnOrientation;
	
	// MENU VARIABLE
	/**
	 * DICOM Viewer menu.
	 */
	private Menu mMenu;
	
	// SINGULAR IMAGE VARIABLE
	
	/**
	 * Is this file meant to be viewed as just one?
	 */
	private boolean mSingle_image_view = false;
	
	// DICOM FILE LOADER THREAD
	/**
	 * File loader thread.
	 */
	private DICOMFileLoader mDICOMFileLoader = null;
	
	// WAIT VARIABLE
	/**
	 * Progress dialog for file loading.
	 */
	private ProgressDialog loadingDialog;
	
	/**
	 * Progress dialog for file caching.
	 */
	private ProgressDialog cachingDialog;
	
	/**
	 * Set if the activity is busy (true) or not (false).
	 * When a DICOM image is parsed or a LISA 16-Bit grayscale
	 * image is loaded, the activity is in waiting mode
	 * => busy = true.
	 */
	private boolean mBusy = false;
	// TODO needed ? or wait for the end of the loading thread
	// is enough ?
	
	// FILE VARIABLE
	/**
	 * The array of DICOM image in the
	 * folder.
	 */
	private File[] mFileArray = null;
	
	/**
	 * The LISA 16-Bit image.
	 */
	private LISAImageGray16Bit mImage = null;
	
	/**
	 * The index of the current file.
	 */
	private int mCurrentFileIndex;
	
	// INITIALIZATION VARIABLE
	/**
	 * Set if the DICOM Viewer is initialized or not.
	 */
	private boolean mIsInitialized = false;
	
	// DATA VARIABLE
	/**
	 * DICOM Viewer data.
	 */
	private DICOMViewerData mDICOMViewerData = null;
	
	
	// ---------------------------------------------------------------
	// # <override> FUNCTIONS
	// ---------------------------------------------------------------
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		// Set the content view
		setContentView(R.layout.dicom_viewer);
		
		// Declare the UI variables
		mImageView = (DICOMImageView) findViewById(R.id.imageView);
		mToolBar = (LinearLayout) findViewById(R.id.toolBar);
		mDimensionToolButton = (Button) findViewById(R.id.dimensionMode);
		mGrayscaleToolButton = (Button) findViewById(R.id.grayscaleMode);
		mCLUTNormalButton = (Button) findViewById(R.id.clutNormal);
		mCLUTInverseButton = (Button) findViewById(R.id.clutInverse);
		mCLUTRainbowButton = (Button) findViewById(R.id.clutRainbow); 
		mLockUnlockToolBar = (Button) findViewById(R.id.lockUnlockToolbar);
		mCurrentToolButton = (Button) findViewById(R.id.currentToolButton);
		mGrayscaleWindow = (GrayscaleWindowView) findViewById(R.id.grayscaleImageView);
		mPreviousButton = (Button) findViewById(R.id.previousImageButton);
		mNextButton = (Button) findViewById(R.id.nextImageButton);
		mIndexTextView = (TextView) findViewById(R.id.imageIndexView);
		mIndexSeekBar = (SeekBar) findViewById(R.id.serieSeekBar);
		mNavigationBar = (LinearLayout) findViewById(R.id.navigationToolbar);
		mRowOrientation = (TextView) findViewById(R.id.rowTextView);
		mColumnOrientation = (TextView) findViewById(R.id.columnTextView);
		
		// Get the file name from the savedInstanceState or from the intent
		String fileName = null;
		
		// If the saved instance state is not null get the file name
		if (savedInstanceState != null) {
			
			fileName = savedInstanceState.getString(FILE_NAME);
			
		// Get the intent
		} else {
			
			Intent intent = getIntent();
			
			if (intent != null) {
				
				Bundle extras = intent.getExtras();
				
				fileName = extras == null ? null : extras.getString("DICOMFileName");
				
				// For Intents from DropBox or OI FileManager
				if (!(intent.hasExtra("DICOMFileName"))){
					try {
						fileName = (new URI(intent.getDataString())).getPath();
						mSingle_image_view = true;
					} catch (URISyntaxException e) {
					}
				}
			}
			
		}
		
		// If the file name is null, alert the user and close
		// the activity
		if (fileName == null) {
			
			showExitAlertDialog("[ERROR] Loading file",
					"The file cannot be loaded.\n\n" +
					"Cannot retrieve its name.");
			
		// Load the file
		} else {
			
			// Get the File object for the current file
			File currentFile = new File(fileName);
			
			// Start the loading thread to load the DICOM image
			mDICOMFileLoader = new DICOMFileLoader(loadingHandler, currentFile);
			mDICOMFileLoader.start();
			mBusy = true;
			
			// Get the files array = get the files contained
			// in the parent of the current file
			// If Single Image View is on we need a minimal files array
			if (!mSingle_image_view){
				mFileArray = currentFile.getParentFile().listFiles(new DICOMFileFilter());
			} else {
				mFileArray = new File[1];
				mFileArray[0] = currentFile;
			}
			
			// Sort the files array
			Arrays.sort(mFileArray);
			
			// If the files array is null or its length is less than 1,
			// there is an error because it must at least contain 1 file:
			// the current file
			if (mFileArray == null || mFileArray.length < 1) {
				
				showExitAlertDialog("[ERROR] Loading file",
						"The file cannot be loaded.\n\n" +
						"The directory containing normally the file contains" +
						" no DICOM files.");
				
			} else {
				
				// Get the file index in the array
				mCurrentFileIndex = getIndex(currentFile);
				
				// If the current file index is negative
				// or greater or equal to the files array
				// length there is an error
				if (mCurrentFileIndex < 0
						|| mCurrentFileIndex >= mFileArray.length) {
					
					showExitAlertDialog("[ERROR] Loading file",
							"The file cannot be loaded.\n\n" +
							"The file is not in the directory.");
				
				// Else initialize views and navigation bar
				} else {
					
					// Check if the seek bar must be shown or not
					if (mFileArray.length == 1) {
						
						mNavigationBar.setVisibility(View.INVISIBLE);
						
					} else {
						
						// Display the current file index
						mIndexTextView.setText(String.valueOf(mCurrentFileIndex + 1));
					
						// Display the files count and set the seek bar maximum
						TextView countTextView = (TextView) findViewById(R.id.imageCountView);
						countTextView.setText(String.valueOf(mFileArray.length));
						mIndexSeekBar.setMax(mFileArray.length - 1);
						mIndexSeekBar.setProgress(mCurrentFileIndex);
						
						// Set the visibility of the previous button
						if (mCurrentFileIndex == 0) {
							
							mPreviousButton.setVisibility(View.INVISIBLE);
							
						} else if (mCurrentFileIndex == (mFileArray.length - 1)) {
							
							mNextButton.setVisibility(View.INVISIBLE);
							
						}
						
					}
					
				}
				
			}
			
		}
		
		// Set the seek bar change index listener
		mIndexSeekBar.setOnSeekBarChangeListener(this);
		
		// Set onLongClickListener
		mIndexSeekBar.setOnLongClickListener(onLongClickListener);
		mNavigationBar.setOnLongClickListener(onLongClickListener);
		mPreviousButton.setOnLongClickListener(onLongClickListener);
		mNextButton.setOnLongClickListener(onLongClickListener);
		mToolBar.setOnLongClickListener(onLongClickListener);
		mGrayscaleWindow.setOnLongClickListener(onLongClickListener);
		
		// Create the DICOMViewerData and set
		// the tool mode to dimension
		mDICOMViewerData = new DICOMViewerData();
		mDICOMViewerData.setToolMode(ToolMode.DIMENSION);
		mDimensionToolButton.setBackgroundResource(R.drawable.ruler_select);
		mCurrentToolButton.setBackgroundResource(R.drawable.ruler_select);
		mCLUTNormalButton.setVisibility(View.GONE);
		mCLUTNormalButton.setBackgroundResource(R.drawable.clut_normal_select);
		mCLUTInverseButton.setVisibility(View.GONE);
		mCLUTRainbowButton.setVisibility(View.GONE);
		mToolBar.setVisibility(View.GONE);
		
		// Set the tool mode too the DICOMImageView and
		// the GrayscaleWindow
		mImageView.setDICOMViewerData(mDICOMViewerData);
		mGrayscaleWindow.setDICOMViewerData(mDICOMViewerData);
		
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
			                DICOMViewer.this.finish();
			           }
			       });
			
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
			
		}
		
		super.onResume();
		
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		
		// We wait until the end of the loading thread
		// before putting the activity in pause mode
		if (mDICOMFileLoader != null) {
			
			// Wait until the loading thread die
			while (mDICOMFileLoader.isAlive()) {
				try {
					synchronized(this) {
						wait(10);
					}
				} catch (InterruptedException e) {
					// Do nothing
				}
			}
			
		}
		
		super.onPause();
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mImage = null;
		mDICOMViewerData = null;
		mFileArray = null;
		mDICOMFileLoader = null;
		
		// Free the drawable callback
		if (mImageView != null) {
			Drawable drawable = mImageView.getDrawable();
			
			if (drawable != null)
				drawable.setCallback(null);
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// Save the current file name
		String currentFileName = mFileArray[mCurrentFileIndex].getAbsolutePath();
		outState.putString(FILE_NAME, currentFileName);
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
            
        // Create image load dialog
        case PROGRESS_DIALOG_LOAD:
        	loadingDialog = new ProgressDialog(this);
        	loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        	loadingDialog.setMessage("Loading image...");
        	loadingDialog.setCancelable(false);
        	return loadingDialog;
            
        default:
            return null;
            
        }
		
    }
	
	
	// ---------------------------------------------------------------
	// + <override> FUNCTIONS
	// ---------------------------------------------------------------
	

	@Override
	public void onLowMemory() {
		
		// Hint the garbage collector
		System.gc();
		
		// Show the exit alert dialog
		showExitAlertDialog("[ERROR] LowMemory", "[ERROR] LowMemory");

		super.onLowMemory();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
        // It's safer to hold the menu (cf. Android
		// documentation)
        mMenu = menu;
        
        // Inflate the currently selected menu XML resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dicom_viewer_menu, menu);
        
        // Set the show/hide grayscale window state
        MenuItem grayscaleShowHide = menu.findItem(R.id.showHide_grayscaleWindow);
        
        if (mGrayscaleWindow.getVisibility() == View.INVISIBLE)
        	grayscaleShowHide.setChecked(false);
        else
        	grayscaleShowHide.setChecked(true);
        
        // Set the show/hide series navigation bar
        MenuItem serieNavigationBar = menu.findItem(R.id.showHide_serieSeekBar);
        
        if (mNavigationBar.getVisibility() == View.INVISIBLE)
        	serieNavigationBar.setChecked(false);
        else
        	serieNavigationBar.setChecked(true);
        
        // If there is at most one file
        if (mFileArray.length <= 1)
        	serieNavigationBar.setEnabled(false);
        else
        	serieNavigationBar.setEnabled(true);
        
        // Set the show/hide tool bar
        MenuItem toolBar = menu.findItem(R.id.showHide_toolbar);
        
        if (mToolBar.getVisibility() == View.GONE
        		&& mCurrentToolButton.getVisibility() == View.INVISIBLE)
        	toolBar.setChecked(false);
        else
        	toolBar.setChecked(true);
        
        // Set the CLUT mode
        switch (mDICOMViewerData.getCLUTMode()) {
        
        case CLUTMode.NORMAL:
        	MenuItem clutMode = menu.findItem(R.id.show_normalLUT);
        	clutMode.setChecked(true);
        	break;
        	
        case CLUTMode.INVERSE:
        	clutMode = menu.findItem(R.id.show_inverseLUT);
        	clutMode.setChecked(true);
        	break;
        	
        case CLUTMode.RAINBOW:
        	clutMode = menu.findItem(R.id.show_rainbowCLUT);
        	clutMode.setChecked(true);
        	break;
        
        }
        
        // Set the lock/unlock tool bar menu time
        MenuItem lockUnlockToolBar = menu.findItem(R.id.lock_toolbar);
        
        if (mLockToolBar)
        	lockUnlockToolBar.setChecked(true);
        else
        	lockUnlockToolBar.setChecked(false);
        
        return true;
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            
        	// SHOW/HIDE
            case R.id.showHide_grayscaleWindow:
            	if (mGrayscaleWindow.getVisibility() == View.INVISIBLE) {
            		item.setChecked(true);
            		mGrayscaleWindow.setVisibility(View.VISIBLE);
            	} else {
            		item.setChecked(false);
            		mGrayscaleWindow.setVisibility(View.INVISIBLE);
            	}
            	return true;
            	
            case R.id.showHide_serieSeekBar:
            	if(mNavigationBar.getVisibility() == View.INVISIBLE) {
            		item.setChecked(true);
            		mNavigationBar.setVisibility(View.VISIBLE);
            	} else {
            		item.setChecked(false);
            		mNavigationBar.setVisibility(View.INVISIBLE);
            	}
            	return true;
            	
            case R.id.showHide_toolbar:
            	if(mToolBar.getVisibility() == View.VISIBLE
            			|| mCurrentToolButton.getVisibility() == View.VISIBLE) {
            		
            		item.setChecked(false);
            		
            		hideToolBar(null);
            		mCurrentToolButton.setVisibility(View.INVISIBLE);
            		
            	} else {
            		item.setChecked(false);
            		
            		if (mLockToolBar)
            			showToolBar(null);
            		else
            			mCurrentToolButton.setVisibility(View.VISIBLE);
            		
            	}
            	return true;
            	
            case R.id.lock_toolbar:
            	if (mLockToolBar) {
            		item.setChecked(false);
            		mLockToolBar = false;
            		mLockUnlockToolBar.setBackgroundResource(R.drawable.unlock);
            		hideToolBar(null);
            	} else {
            		item.setChecked(true);
                	mLockToolBar = true;
                	mLockUnlockToolBar.setBackgroundResource(R.drawable.lock);
                	showToolBar(null);
            	}
            	return true;
            	
            // LUT/CLUT
            case R.id.show_normalLUT:
            	item.setChecked(true);
            	
            	mCLUTNormalButton.setBackgroundResource(
    					R.drawable.clut_normal_select);
    			mCLUTInverseButton.setBackgroundResource(
    					R.drawable.clut_inverse);
    			mCLUTRainbowButton.setBackgroundResource(
    					R.drawable.clut_rainbow);
    			
            	mDICOMViewerData.setCLUTMode(CLUTMode.NORMAL);
            	
            	mGrayscaleWindow.updateCLUTMode();
            	
            	mImageView.draw();
            	
            	return true;
            	
            case R.id.show_inverseLUT:
            	item.setChecked(true);
            	
            	mCLUTNormalButton.setBackgroundResource(
    					R.drawable.clut_normal);
    			mCLUTInverseButton.setBackgroundResource(
    					R.drawable.clut_inverse_select);
    			mCLUTRainbowButton.setBackgroundResource(
    					R.drawable.clut_rainbow);
    			
            	mGrayscaleWindow.updateCLUTMode();
            	
            	mImageView.draw();
            	
            	return true;
            	
            case R.id.show_rainbowCLUT:
            	item.setChecked(true);
            	
            	mCLUTNormalButton.setBackgroundResource(
    					R.drawable.clut_normal);
    			mCLUTInverseButton.setBackgroundResource(
    					R.drawable.clut_inverse);
    			mCLUTRainbowButton.setBackgroundResource(
    					R.drawable.clut_rainbow_select);
            	
            	mDICOMViewerData.setCLUTMode(CLUTMode.RAINBOW);
            	
            	mGrayscaleWindow.updateCLUTMode();
            	
            	mImageView.draw();
            	
            	return true;
            	
            // GRAYSCALE WINDOW
            case R.id.grayscaleWindow_CTBone:
            	mDICOMViewerData.setWindowWidth(1500);
            	mDICOMViewerData.setWindowCenter(300 + 1024);
            	mImageView.draw();
            	return true;
            	
            case R.id.grayscaleWindow_CTCrane:
            	mDICOMViewerData.setWindowWidth(100);
            	mDICOMViewerData.setWindowCenter(50 + 1024);
            	mImageView.draw();
            	return true;
            	
            case R.id.grayscaleWindow_CTLung:
            	mDICOMViewerData.setWindowWidth(1400);
            	mDICOMViewerData.setWindowCenter(1024 - 400);
            	mImageView.draw();
            	return true;
            
            case R.id.grayscaleWindow_CTAbdomen:
            	mDICOMViewerData.setWindowWidth(350);
            	mDICOMViewerData.setWindowCenter(40 + 1024);
            	mImageView.draw();
            	return true;
            	
            // IMAGE SCALE MODE
            case R.id.fit_to_screen:
            	item.setChecked(true);
            	mDICOMViewerData.setScaleMode(ScaleMode.FITIN);
            	mImageView.resetSize();
            	return true;
            	
            case R.id.real_size:
            	item.setChecked(true);
            	mDICOMViewerData.setScaleMode(ScaleMode.REALSIZE);
            	mImageView.resetSize();
            	return true;
            	
            // CACHE ALL IMAGES
            case R.id.ddv_CacheAllImage:
    			cacheImages();
            	return true;
            	
            case R.id.ddv_RotateImage:
            	mImageView.toggleRotate();
            	return true;
            	
            // ABOUT DIALOG	
            case R.id.ddv_DialogAbout:
            	Dialog dialog = new Dialog(this);
            	dialog.setContentView(R.layout.dialog_about);
            	dialog.setTitle("Droid Dicom Viewer: About");
            	dialog.show();
            	return true;
            	
        }
        
        return super.onOptionsItemSelected(item);
	 }
	
	
	// ---------------------------------------------------------------
	// + <implements> FUNCTION
	// ---------------------------------------------------------------
	
	/* (non-Javadoc)
	 * @see android.widget.SeekBar.OnSeekBarChangeListener#onProgressChanged(android.widget.SeekBar, int, boolean)
	 */
	public synchronized void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		
		try {
			
			// If it is busy, do nothing
			if (mBusy)
				return;
			
			// It is busy now
			mBusy = true;
			
			// Wait until the loading thread die
			while (mDICOMFileLoader.isAlive()) {
				try {
					synchronized(this) {
						wait(10);
					}
				} catch (InterruptedException e) {
					// Do nothing
				}
			}
			
			// Set the current file index
			mCurrentFileIndex = progress;
			
			// Start the loading thread to load the DICOM image
			mDICOMFileLoader = new DICOMFileLoader(loadingHandler, 
					mFileArray[mCurrentFileIndex]);
			
			mDICOMFileLoader.start();
			
			// Update the UI
			mIndexTextView.setText(String.valueOf(mCurrentFileIndex + 1));
			
			// Set the visibility of the previous button
			if (mCurrentFileIndex == 0) {
				
				mPreviousButton.setVisibility(View.INVISIBLE);
				mNextButton.setVisibility(View.VISIBLE);
				
			} else if (mCurrentFileIndex == (mFileArray.length - 1)) {
				
				mNextButton.setVisibility(View.INVISIBLE);
				mPreviousButton.setVisibility(View.VISIBLE);
				
			} else {
				
				mPreviousButton.setVisibility(View.VISIBLE);
				mNextButton.setVisibility(View.VISIBLE);
				
			}
			
		} catch (OutOfMemoryError ex) {
			
			System.gc();
			
			showExitAlertDialog("[ERROR] Out Of Memory",
					"This series contains images that are too big" +
					" and that cause out of memory error. The best is to don't" +
					" use the series seek bar. If the error occurs again" +
					" it is because this series is not adapted to your" +
					" Android(TM) device.");
			
		}
		
	}

	// Needed to implement the SeekBar.OnSeekBarChangeListener
	public void onStartTrackingTouch(SeekBar seekBar) {
		// Do nothing.
	}

	// Needed to implement the SeekBar.OnSeekBarChangeListener
	public void onStopTrackingTouch(SeekBar seekBar) {
		
		System.gc(); // TODO needed ?
		// Do nothing.		
	}
	
	
	// ---------------------------------------------------------------
	// + FUNCTIONS
	// ---------------------------------------------------------------
	
	/**
	 * Set mToolMode to ToolMode.DIMENSION.
	 * 
	 * @param view The view that call the handler. Can be null
	 */
	public void dimensionMode(View view) {

		if (mDICOMViewerData.getToolMode() != ToolMode.DIMENSION) {

			mDICOMViewerData.setToolMode(ToolMode.DIMENSION);

			mDimensionToolButton.setBackgroundResource(R.drawable.ruler_select);
			mCurrentToolButton.setBackgroundResource(R.drawable.ruler_select);
			mGrayscaleToolButton.setBackgroundResource(R.drawable.grayscale);
			
			mCLUTNormalButton.setVisibility(View.GONE);
			mCLUTInverseButton.setVisibility(View.GONE);
			mCLUTRainbowButton.setVisibility(View.GONE);
			

		}
		
		if (!mLockToolBar)
			hideToolBar(view);

	}

	/**
	 * Set mToolMode to ToolMode.GRAYSCALE.
	 * 
	 * @param view The view that call the handler. Can be null
	 */
	public void grayscaleMode(View view) {

		if (mDICOMViewerData.getToolMode() != ToolMode.GRAYSCALE) {

			mDICOMViewerData.setToolMode(ToolMode.GRAYSCALE);

			mGrayscaleToolButton.setBackgroundResource(R.drawable.grayscale_select);
			mCurrentToolButton.setBackgroundResource(R.drawable.grayscale_select);
			mDimensionToolButton.setBackgroundResource(R.drawable.ruler);
			
			mCLUTNormalButton.setVisibility(View.VISIBLE);
			mCLUTInverseButton.setVisibility(View.VISIBLE);
			mCLUTRainbowButton.setVisibility(View.VISIBLE);

		}
		
		if (!mLockToolBar)
			hideToolBar(view);

	}
	
	
	// ---------------------------------------------------------------
	// + <synchronized> FUNCTIONS
	// ---------------------------------------------------------------
	
	/**
	 * Handle touch on the previousButton.
	 * @param view
	 */
	public synchronized void previousImage(View view) {
		
		// If it is busy, do nothing
		if (mBusy)
			return;
		
		// It is busy now
		mBusy = true;
		
		// Wait until the loading thread die
		while (mDICOMFileLoader.isAlive()) {
			try {
				synchronized(this) {
					wait(10);
				}
			} catch (InterruptedException e) {
				// Do nothing
			}
		}
		
		// If the current file index is 0, there is
		// no previous file in the files array
		// We add the less or equal to zero because it is
		// safer
		if (mCurrentFileIndex <= 0) {
			
			// Not necessary but safer, because we don't know
			// how the code will be used in the future
			mCurrentFileIndex = 0;
			
			// If for a unknown reason the previous button is
			// visible => hide it
			if (mPreviousButton.getVisibility() == View.VISIBLE)
				mPreviousButton.setVisibility(View.INVISIBLE);
			
			mBusy = false;
			return;
			
		}
		
		//  Decrease the file index
		mCurrentFileIndex--;
		
		// Start the loading thread to load the DICOM image
		mDICOMFileLoader = new DICOMFileLoader(loadingHandler, 
				mFileArray[mCurrentFileIndex]);
		
		mDICOMFileLoader.start();
		
		// Update the UI
		mIndexTextView.setText(String.valueOf(mCurrentFileIndex + 1));
		mIndexSeekBar.setProgress(mCurrentFileIndex);
		
		if (mCurrentFileIndex == 0)
			mPreviousButton.setVisibility(View.INVISIBLE);
		
		// The next button is automatically set to visible
		// because if there is a previous image, there is
		// a next image
		mNextButton.setVisibility(View.VISIBLE);
		
	}
	
	/**
	 * Handle touch on next button.
	 * @param view
	 */
	public synchronized void nextImage(View view) {
		
		// If it is busy, do nothing
		if (mBusy)
			return;
		
		// It is busy now
		mBusy = true;
		
		// Wait until the loading thread die
		while (mDICOMFileLoader.isAlive()) {
			try {
				synchronized(this) {
					wait(10);
				}
			} catch (InterruptedException e) {
				// Do nothing
			}
		}
		
		// If the current file index is the last file index,
		// there is no next file in the files array
		// We add the greater or equal to (mFileArray.length - 1)
		// because it is safer
		if (mCurrentFileIndex >= (mFileArray.length - 1)) {
			
			// Not necessary but safer, because we don't know
			// how the code will be used in the future
			mCurrentFileIndex = (mFileArray.length - 1);
			
			// If for a unknown reason the previous button is
			// visible => hide it
			if (mNextButton.getVisibility() == View.VISIBLE)
				mNextButton.setVisibility(View.INVISIBLE);
			
			mBusy = false;
			return;
			
		}
		
		//  Increase the file index
		mCurrentFileIndex++;
		
		// Start the loading thread to load the DICOM image
		mDICOMFileLoader = new DICOMFileLoader(loadingHandler, 
				mFileArray[mCurrentFileIndex]);
		
		mDICOMFileLoader.start();
		
		// Update the UI
		mIndexTextView.setText(String.valueOf(mCurrentFileIndex + 1));
		mIndexSeekBar.setProgress(mCurrentFileIndex);
		
		if (mCurrentFileIndex == (mFileArray.length - 1))
			mNextButton.setVisibility(View.INVISIBLE);
		
		// The previous button is automatically set to visible
		// because if there is a next image, there is
		// a previous image
		mPreviousButton.setVisibility(View.VISIBLE);
	
	}
	
	/**
	 * Hide navigation tool bar if it is visible.
	 * @param view
	 */
	public void hideNavigationBar(View view) {
		
		if (mNavigationBar.getVisibility() == View.VISIBLE) {
			
			mNavigationBar.setVisibility(View.INVISIBLE);
			
			if (mMenu != null) {
				
				MenuItem showHideNavigationBar = 
					mMenu.findItem(R.id.showHide_serieSeekBar);
				
				if (showHideNavigationBar != null) {
					showHideNavigationBar.setChecked(false);
				}
				
			}
			
		}
		
	}
	
	/**
	 * Hide navigation tool bar if it is visible.
	 * @param view
	 */
	public void hideGrayscaleWindow(View view) {
		
		if (mGrayscaleWindow.getVisibility() == View.VISIBLE) {
			
			mGrayscaleWindow.setVisibility(View.INVISIBLE);
			
			if (mMenu != null) {
				
				MenuItem showHideGrayscaleWindow = 
					mMenu.findItem(R.id.showHide_grayscaleWindow);
				
				if (showHideGrayscaleWindow != null) {
					showHideGrayscaleWindow.setChecked(false);
				}
				
			}
			
		}
		
	}
	
	/**
	 * Hide tool bar if it is visible.
	 * @param view
	 */
	public void hideToolBar(View view) {
		
		if (mToolBar.getVisibility() == View.VISIBLE) {
			
			mToolBar.setVisibility(View.GONE);
			
			// If the tool bar is locked then the current icon
			// is invisible too
			if (!mLockToolBar) {
				
				mCurrentToolButton.setVisibility(View.VISIBLE);
				
			} else {
				
				if (mMenu != null) {
					
					MenuItem showHideToolBar = 
						mMenu.findItem(R.id.showHide_toolbar);
					
					if (showHideToolBar != null) {
						showHideToolBar.setChecked(false);
					}
					
				}
				
			}
			
		}
		
	}
	
	/**
	 * Hide tool bar if it is visible.
	 * @param view
	 */
	public void showToolBar(View view) {
		
		if (mToolBar.getVisibility() == View.GONE) {
			
			mCurrentToolButton.setVisibility(View.INVISIBLE);
			mToolBar.setVisibility(View.VISIBLE);
			
			if (mMenu != null) {
				
				MenuItem showHideToolBar = 
					mMenu.findItem(R.id.showHide_toolbar);
				
				if (showHideToolBar != null) {
					showHideToolBar.setChecked(true);
				}
				
			}
			
		}
		
	}
	
	/**
	 * Set the CLUT mode.
	 * @param view
	 */
	public synchronized void setCLUTMode(View view) {
		
		if (view == null)
			return;
		
		switch (view.getId())
		{
		
		case R.id.clutNormal:
			mCLUTNormalButton.setBackgroundResource(
					R.drawable.clut_normal_select);
			mCLUTInverseButton.setBackgroundResource(
					R.drawable.clut_inverse);
			mCLUTRainbowButton.setBackgroundResource(
					R.drawable.clut_rainbow);
			
			mDICOMViewerData.setCLUTMode(CLUTMode.NORMAL);
			
			// Update the menu
			if (mMenu != null) {
				
				MenuItem showHideToolBar = 
					mMenu.findItem(R.id.show_normalLUT);
				
				if (showHideToolBar != null) {
					showHideToolBar.setChecked(true);
				}
				
			}
			
			break;
			
		case R.id.clutInverse:
			mCLUTNormalButton.setBackgroundResource(
					R.drawable.clut_normal);
			mCLUTInverseButton.setBackgroundResource(
					R.drawable.clut_inverse_select);
			mCLUTRainbowButton.setBackgroundResource(
					R.drawable.clut_rainbow);
			
			mDICOMViewerData.setCLUTMode(CLUTMode.INVERSE);
			
			if (mMenu != null) {
				
				MenuItem showHideToolBar = 
					mMenu.findItem(R.id.show_inverseLUT);
				
				if (showHideToolBar != null) {
					showHideToolBar.setChecked(true);
				}
				
			}
			
			break;
			
		case R.id.clutRainbow:
			mCLUTNormalButton.setBackgroundResource(
					R.drawable.clut_normal);
			mCLUTInverseButton.setBackgroundResource(
					R.drawable.clut_inverse);
			mCLUTRainbowButton.setBackgroundResource(
					R.drawable.clut_rainbow_select);
			
			mDICOMViewerData.setCLUTMode(CLUTMode.RAINBOW);
			
			if (mMenu != null) {
				
				MenuItem showHideToolBar = 
					mMenu.findItem(R.id.show_rainbowCLUT);
				
				if (showHideToolBar != null) {
					showHideToolBar.setChecked(true);
				}
				
			}
			
			break;
		
		}
		
		mGrayscaleWindow.updateCLUTMode();
		
		mImageView.draw();
		
		if (!mLockToolBar)
			hideToolBar(null);
		
	}
	
	/**
	 * Lock/unlock the tool bar.
	 * @param view
	 */
	public void lockUnlockToolBar(View view) {
		
		if (mLockToolBar) {
			
			mLockToolBar = false;
			mLockUnlockToolBar.setBackgroundResource(R.drawable.unlock);
			
			if (mMenu != null) {
				
				MenuItem showHideToolBar = 
					mMenu.findItem(R.id.lock_toolbar);
				
				if (showHideToolBar != null) {
					showHideToolBar.setChecked(false);
				}
				
			}
			
			hideToolBar(view);
			
		} else {
			
			mLockToolBar = true;
			mLockUnlockToolBar.setBackgroundResource(R.drawable.lock);
			
			if (mMenu != null) {
				
				MenuItem showHideToolBar = 
					mMenu.findItem(R.id.lock_toolbar);
				
				if (showHideToolBar != null) {
					showHideToolBar.setChecked(true);
				}
				
			}
			
		}
		
	}
	
	// ---------------------------------------------------------------
	// - FUNCTIONS
	// ---------------------------------------------------------------
	
	/**
	 * Get the index of the file in the files array.
	 * @param file
	 * @return Index of the file in the files array
	 * or -1 if the files is not in the list.
	 */
	private int getIndex(File file) {
		
		if (mFileArray == null)
			throw new NullPointerException("The files array is null.");
		
		for (int i = 0; i < mFileArray.length; i++) {
			
			if (mFileArray[i].getName().equals(file.getName()))
				return i;
			
		}
		
		return -1;
		
	}
	
	/**
	 * Set the currentImage
	 * 
	 * @param image
	 */
	private void setImage(LISAImageGray16Bit image) {
		
		if (image == null)
			throw new NullPointerException("The LISA 16-Bit grayscale image " +
					"is null");
		
		try {
		
			// Set the image
			mImage = null;
			mImage = image;
			mImageView.setImage(mImage);
			mGrayscaleWindow.setImage(mImage);
			
			setImageOrientation();
			
			// If it is not initialized, set the window width and center
			// as the value set in the LISA 16-Bit grayscale image
			// that comes from the DICOM image file.
			if (!mIsInitialized) {
				
				mIsInitialized = true;
				mDICOMViewerData.setWindowWidth(mImage.getWindowWidth());
				mDICOMViewerData.setWindowCenter(mImage.getWindowCenter());
				
				mImageView.draw();
				mImageView.fitIn();
				
			} else {
				
				mImageView.draw();
			}
			
			mBusy = false;
			
		} catch (OutOfMemoryError ex) {
			
			System.gc();
			
			showExitAlertDialog("[ERROR] Out Of Memory",
					"This series contains images that are too big" +
					" and that cause out of memory error. The best is to don't" +
					" use the series seek bar. If the error occurs again" +
					" it is because this series is not adapted to your" +
					" Android(TM) device.");
			
		} catch (ArrayIndexOutOfBoundsException ex) {
			
			showExitAlertDialog("[ERROR] Image drawing",
					"An uncatchable error occurs while " +
					"drawing the DICOM image.");
			
		}
		
	}
	
	/**
	 * Set the image orientation TextViews.
	 */
	private void setImageOrientation() {
		
		float[] imageOrientation = mImage.getImageOrientation();
		
		if (imageOrientation == null
				|| imageOrientation.length != 6
				|| imageOrientation.equals(new float[6])) // equal to a float with 6 zeros
			return;
		
		// Displaying the row orientation
		mRowOrientation.setText(getImageOrientationString(imageOrientation, 0));
		
		// Displaying the column orientation
		mColumnOrientation.setText(getImageOrientationString(imageOrientation, 3));
		
	}
	
	/**
	 * Get the image orientation String for a 3D vector of float
	 * that is related to a direction cosine.
	 * 
	 * @param imageOrientation
	 * @param offset
	 * @return
	 */
	private String getImageOrientationString(float[] imageOrientation, int offset) {
		
		String orientation = "";
		
		// The threshold is 0.25
		
		// If abs(ImageOrientation.X) < threshold
		// and ImageOrientation.X > 0 => orientation: RL
		if (imageOrientation[0 + offset] >= 0.25) {
			
			orientation += "R";
			
		// If abs(ImageOrientation.X) < threshold
		// and ImageOrientation.X < 0 => orientation: LR	
		} else if (imageOrientation[0 + offset] <= -0.25) {
			
			orientation += "L";
			
		}
		
		// If abs(ImageOrientation.Y) < threshold
		// and ImageOrientation.Y > 0 => orientation: AP
		if (imageOrientation[1 + offset] >= 0.25) {
			
			orientation += "A";
			
		// If abs(ImageOrientation.Y) < threshold
		// and ImageOrientation.Y < 0 => orientation: PA	
		} else if (imageOrientation[1 + offset] <= -0.25) {
			
			orientation += "P";
			
		}
		
		// If abs(ImageOrientation.Z) < threshold
		// and ImageOrientation.Z > 0 => orientation: FH
		if (imageOrientation[2 + offset] >= 0.25) {
			
			orientation += "F";
			
		// If abs(ImageOrientation.Z) < threshold
		// and ImageOrientation.Z < 0 => orientation: HF	
		} else if (imageOrientation[2 + offset] <= -0.25) {
			
			orientation += "H";
			
		}
		
		return orientation;
		
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
		            	break;
		            	
		            case ThreadState.CATCHABLE_ERROR_OCCURRED:
		            	cachingDialog.setProgress(message.arg1);
		            	Toast.makeText(DICOMViewer.this, "[Error]: file ("
		            			+ (String) message.obj + ") cannot be cached.", Toast.LENGTH_SHORT).show();
		            	break;
		            	
		            case ThreadState.UNCATCHABLE_ERROR_OCCURRED:
		            	try {
		            		
		            		dismissDialog(PROGRESS_DIALOG_CACHE);
		            		
		            	} catch (IllegalArgumentException ex) {
		            		// Do nothing
		            	}
		            	AlertDialog.Builder builder = new AlertDialog.Builder(DICOMViewer.this);
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
		            	builder = new AlertDialog.Builder(DICOMViewer.this);
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
				new DICOMImageCacher(cacheHandler,
						mFileArray[mCurrentFileIndex].getParent());
			
			dicomImageCacher.start();
            
			
		} catch (FileNotFoundException e) {
			
			// TODO display an error ?
			
		}
		
	}
	
	/**
	 * Show an alert dialog (AlertDialog) to inform
	 * the user that the activity must finish.
	 * @param title Title of the AlertDialog.
	 * @param message Message of the AlertDialog.
	 */
	private void showExitAlertDialog(String title, String message) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message)
			   .setIcon(android.R.drawable.ic_dialog_alert)
			   .setTitle(title)
		       .setCancelable(false)
		       .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               DICOMViewer.this.finish();
		           }
		       });
		
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
		
	}
	
	
	// ---------------------------------------------------------------
	// - <final> Class
	// ---------------------------------------------------------------
	
	/**
	 * The OnLongClickListener.
	 */
	private final View.OnLongClickListener onLongClickListener
								= new View.OnLongClickListener() {
		
		public boolean onLongClick(View view) {
			
			if (view == null)
				return false;
			
			switch(view.getId()) {
			
			case R.id.toolBar:
				hideToolBar(view);
				return true;
				
			case R.id.grayscaleImageView:
				hideGrayscaleWindow(view);
				return true;
			
			case R.id.navigationToolbar:
			case R.id.previousImageButton:
			case R.id.nextImageButton:
			case R.id.serieSeekBar:
				hideNavigationBar(view);
				return true;
			
			default:
				return false;

			}
		}
	};
	
	private final Handler loadingHandler = new Handler() {
		
		public void handleMessage(Message message) {
        	
            switch (message.what) {
            
            case ThreadState.STARTED:
            	showDialog(PROGRESS_DIALOG_LOAD);
            	break;
            	
            case ThreadState.FINISHED:
            	try {
            		
            		dismissDialog(PROGRESS_DIALOG_LOAD);
            		
            	} catch (IllegalArgumentException ex) {	
            		// Do nothing		
            	}
            	
            	// Set the loaded image
            	if (message.obj instanceof LISAImageGray16Bit) {
            		setImage((LISAImageGray16Bit) message.obj);
            	}
            	
            	break;
            	
            case ThreadState.UNCATCHABLE_ERROR_OCCURRED:
            	try {
            		
            		dismissDialog(PROGRESS_DIALOG_LOAD);
            		
            	} catch (IllegalArgumentException ex) {
            		// Do nothing
            	}
            	
            	// Get the error message
            	String errorMessage;
            	
            	if (message.obj instanceof String)
            		errorMessage = (String) message.obj;
            	else
            		errorMessage = "Unknown error";
            	
            	// Show an alert dialog
            	showExitAlertDialog("[ERROR] Loading file",
            			"An error occured during the file loading.\n\n"
    					+ errorMessage);
            	
            	break;
            	
            case ThreadState.OUT_OF_MEMORY:
            	try {
            		
            		dismissDialog(PROGRESS_DIALOG_LOAD);
            		
            	} catch (IllegalArgumentException ex) {
            		// Do nothing
            	}
            	
            	// Show an alert dialog
            	showExitAlertDialog("[ERROR] Loading file",
            			"OutOfMemoryError: During the loading of image ("
    					+ mFileArray[mCurrentFileIndex].getName()
    					+ "), an out of memory error occurred.\n\n"
    					+ "Your file is too large for your Android system. You can"
    					+ " try to cache the image in the file chooser."
    					+ " If the error occured again, then the image cannot be displayed"
    					+ " on your device.\n"
    					+ "Try to use the Droid Dicom Viewer desktop file cacher software"
    					+ " (not available yet).");
            	
            	break;
            
            }
            
		}
		
	};
	
	
	// ---------------------------------------------------------------
	// - <static> CLASS
	// ---------------------------------------------------------------
	
	private static final class DICOMFileLoader extends Thread {
		
		// The handler to send message to the parent thread
		private final Handler mHandler;
		
		// The file to load
		private final File mFile;
		
		public DICOMFileLoader(Handler handler, File file) {
			
			if (handler == null)
				throw new NullPointerException("The handler is null while" +
						" calling the loading thread.");
			
			mHandler = handler;
			
			if (file == null)
				throw new NullPointerException("The file is null while" +
						" calling the loading thread.");
			
			mFile = file;
			
			
		}
		
		public void run() {
			
			// If the image data is null, do nothing.
			if (!mFile.exists()) {
				
				Message message = mHandler.obtainMessage();
				message.what = ThreadState.UNCATCHABLE_ERROR_OCCURRED;
				message.obj = "The file doesn't exist.";
				mHandler.sendMessage(message);
				
				return;
			}
			
			// If image exists show image
			try {
				
				LISAImageGray16BitReader reader =
					new LISAImageGray16BitReader(mFile + ".lisa");
				
				LISAImageGray16Bit image = reader.parseImage();
				reader.close();
				
				// Send the LISA 16-Bit grayscale image
				Message message = mHandler.obtainMessage();
				message.what = ThreadState.FINISHED;
				message.obj = image;
				mHandler.sendMessage(message);
				
				return;
				
			} catch (Exception ex) {
				// Do nothing and create a LISA image
			}
			
			// Create a LISA image and ask to show the
			// progress dialog in spinner mode
			mHandler.sendEmptyMessage(ThreadState.STARTED);
			
			try {
				
				DICOMImageReader dicomFileReader = new DICOMImageReader(mFile);
				
				DICOMImage dicomImage = dicomFileReader.parse();
				dicomFileReader.close();
				
				// If the image is uncompressed, show it and cached it.
				if (dicomImage.isUncompressed()) {
					
					LISAImageGray16BitWriter out =
						new LISAImageGray16BitWriter(mFile + ".lisa");
					
					out.write(dicomImage.getImage());
					out.flush();
					out.close();
					
					Message message = mHandler.obtainMessage();
					message.what = ThreadState.FINISHED;
					message.obj = dicomImage.getImage();
					mHandler.sendMessage(message);
					
					// Hint the garbage collector
					System.gc();
					
				} else {
					
					Message message = mHandler.obtainMessage();
					message.what = ThreadState.UNCATCHABLE_ERROR_OCCURRED;
					message.obj = "The file is compressed. Compressed format are not"
						+ " supported yet.";
					mHandler.sendMessage(message);
					
				}
				
			} catch (OutOfMemoryError ex) {
				
				Message message = mHandler.obtainMessage();
				message.what = ThreadState.OUT_OF_MEMORY;
				message.obj = ex.getMessage();
				mHandler.sendMessage(message);
				
			} catch (Exception ex) {
				
				Message message = mHandler.obtainMessage();
				message.what = ThreadState.UNCATCHABLE_ERROR_OCCURRED;
				message.obj = ex.getMessage();
				mHandler.sendMessage(message);
				
			}
			
		}
		
	}
	
}
