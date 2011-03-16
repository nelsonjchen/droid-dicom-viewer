Droid DICOM Viewer
==================

http://code.google.com/p/droid-dicom-viewer/

2011 © Pierre Malarme


## Fork


This fork was done as an exercise in enhancing a previously available Android
application such as an Android DICOM viewer.

Sadly, not much attention was paid to respecting the original code style.
Additionally, a third party library (dcm4che2) was tacked on to add features that
the original Android specific code did not have. Even then, we were not able to fully
utilize the original features of the library that exist on SunOracle's original VM but
do not on Android's Dalvik psuedo-Java platform.

We wish that more proper implementations of the following features will eventually
find their way into the original project.

We've added:

* Intents from OI File Manager, DropBox, and similar apps are handled.

* A nice File Metadata Viewing Activity from the Chooser and the Viewer.

* The chooser now sorts without capitalization.

* The ability to rotate an Image in the Viewer Activity.

* There is now a Preference screen for hiding the disclaimer on first
  startup.

* A Git repository that does not have R.java and other riffraff files
  under version control thanks to .gitignore. It's also now on GitHub under
  crazysim/droid-dicom-viewer . Our branch with our enhancements are on the
  *enhance* branch of that repository. Without enhancement, our properly 
  versioned controlled version of the original repository is on the *git* 
  branch. Finally, *master* is a commit for commit port of the original 
  mercurial repository. 

## Building

Add the Jar files to your classpath. Run in your IDE.
