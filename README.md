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

* A nice File Metadata Viewing Activity from the Chooser.

* The chooser now sorts without capitalization.

* The ability to rotate an Image in the Viewer Activity.

* A Git repository that does not have R.java and other riffraff files
  under version control thanks to .gitignore . It's also now on GitHub!