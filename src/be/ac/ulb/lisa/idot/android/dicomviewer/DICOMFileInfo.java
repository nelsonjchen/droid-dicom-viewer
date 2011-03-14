package be.ac.ulb.lisa.idot.android.dicomviewer;

import android.app.Activity;
import android.os.Bundle;
import be.ac.ulb.lisa.idot.dicom.DICOMException;
import be.ac.ulb.lisa.idot.dicom.data.DICOMMetaInformation;
import be.ac.ulb.lisa.idot.dicom.file.DICOMReader;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.io.DicomInputStream;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DICOMFileInfo extends Activity {


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String file_loc = "/sdcard/dropbox/school/adiview_stuff/working_images/CT-MONO2-8-abdo.dcm";
        try {
            File file = new File(file_loc);

            DicomInputStream dis = new DicomInputStream(file);

            BasicDicomObject bdo = new BasicDicomObject();
            dis.readDicomObject(bdo,-1);

            dis.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (EOFException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        finish();

    }

}