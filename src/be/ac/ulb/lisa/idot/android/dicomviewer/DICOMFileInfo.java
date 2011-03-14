package be.ac.ulb.lisa.idot.android.dicomviewer;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import be.ac.ulb.lisa.idot.dicom.DICOMException;
import be.ac.ulb.lisa.idot.dicom.data.DICOMMetaInformation;
import be.ac.ulb.lisa.idot.dicom.file.DICOMReader;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputHandler;
import org.dcm4che2.io.DicomInputStream;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class DICOMFileInfo extends Activity implements DicomInputHandler {
    ArrayList<RowModel> information;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String file_loc = "/sdcard/dropbox/school/adiview_stuff/working_images/CT-MONO2-8-abdo.dcm";
        try {
            File file = new File(file_loc);

            DicomInputStream dis = new DicomInputStream(file);

            BasicDicomObject bdo = new BasicDicomObject();
            dis.setHandler(this);
            dis.readDicomObject(bdo, -1);
            information = new ArrayList<RowModel>();



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

    @Override
    public boolean readValue(DicomInputStream in) throws IOException {
        switch (in.tag()){
            case Tag.Item:
//                if (in.sq().vr() != VR.SQ && in.valueLength() != -1) {
//                    outFragment(in);
//                } else {
//                    outItem(in);
//                }
                break;
            case Tag.ItemDelimitationItem:
            case Tag.SequenceDelimitationItem:
//                if (in.level() > 0)
//                    outItem(in);
                break;
            default:
                outElement(in);
        }

        return true;

    }

    public void outElement(DicomInputStream in) {

        return;
    }

    class RowModel {
        String description;
        String value;
    }


}