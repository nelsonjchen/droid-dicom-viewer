package be.ac.ulb.lisa.idot.android.dicomviewer;

import android.app.Activity;
import android.os.Bundle;
import be.ac.ulb.lisa.idot.dicom.DICOMException;
import be.ac.ulb.lisa.idot.dicom.data.DICOMMetaInformation;
import be.ac.ulb.lisa.idot.dicom.file.DICOMReader;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DICOMFileInfo extends Activity {


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String file_loc = "/sdcard/dropbox/school/adiview_stuff/working_images/CT-MONO2-8-abdo.dcm";
        try {
            DICOMReader dicomReader = new DICOMReader(file_loc);

            DICOMMetaInformation metaInformation = dicomReader.parseMetaInformation();

            dicomReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (DICOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (EOFException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        finish();

    }

}