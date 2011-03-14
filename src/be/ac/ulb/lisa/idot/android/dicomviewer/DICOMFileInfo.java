package be.ac.ulb.lisa.idot.android.dicomviewer;

import android.*;
import android.R;
import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputHandler;
import org.dcm4che2.io.DicomInputStream;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;



public class DICOMFileInfo extends ListActivity implements DicomInputHandler {
    ArrayList<RowModel> info;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String file_loc = "/sdcard/dropbox/school/adiview_stuff/working_images/CT-MONO2-8-abdo.dcm";
        try {
            File file = new File(file_loc);

            DicomInputStream dis = new DicomInputStream(file);

            BasicDicomObject bdo = new BasicDicomObject();

            info = new ArrayList<RowModel>();

            dis.setHandler(this);
            dis.readDicomObject(bdo, -1);



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
                if (in.sq().vr() != VR.SQ && in.valueLength() != -1) {
                    outFragment(in);
                } else {
//                    outItem(in);
                }
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

    public void outFragment(DicomInputStream in) throws IOException{
        char[] cbuf = new char[64];
        in.readValue(in);
        DicomElement sq = in.sq();
        byte[] data = sq.removeFragment(0);
        boolean bigEndian = in.getTransferSyntax().bigEndian();
        sq.vr().promptValue(data, bigEndian, null, cbuf, 0, new StringBuffer());
    }

    public void outElement(DicomInputStream in) {

        boolean bigEndian = in.getTransferSyntax().bigEndian();
        int tag = in.tag();
        VR vr = in.vr();
        byte[] val = new byte[0];
        try {
            val = in.readBytes(in.valueLength());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        if (tag == 0x00020000) {
            in.setEndOfFileMetaInfoPosition(
                    in.getStreamPosition() + vr.toInt(val, bigEndian));
        }

        RowModel model = new RowModel();
        model.setDescription(in.getDicomObject().nameOf(tag));
        info.add(model);


    }

    class RowModel {
        String description;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        String value;
    }

    class DICOMMetaAdapter extends ArrayAdapter<RowModel> {
        DICOMMetaAdapter() {
            super(DICOMFileInfo.this,android.R.layout.simple_list_item_1, info);
        }

        public View getView(int position, View convertView, ViewGroup parent){
            View row = convertView;

            if (row == null){
                LayoutInflater inflater = getLayoutInflater();

                row = inflater.inflate(android.R.layout.simple_list_item_1,parent,false);
            }

            TextView label = (TextView) row.findViewById(android.R.id.text1);

            label.setText(info.get(position).getDescription());

            return(row);
        }
    }
}