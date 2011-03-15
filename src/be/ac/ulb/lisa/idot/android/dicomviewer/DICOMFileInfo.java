package be.ac.ulb.lisa.idot.android.dicomviewer;

import android.*;
import android.R;
import android.app.Activity;
import android.app.ListActivity;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import be.ac.ulb.lisa.idot.android.dicomviewer.data.DCM4CheTagNameHack;
import be.ac.ulb.lisa.idot.dicom.DICOMTag;
import com.sun.xml.internal.ws.model.FieldSignature;
import org.dcm4che2.data.*;
import org.dcm4che2.io.DicomInputHandler;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.util.TagUtils;
import org.xml.sax.SAXException;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;


public class DICOMFileInfo extends ListActivity implements DicomInputHandler {
    ArrayList<RowModel> info;
    char[] cbuf = new char[64];
    int maxValLen = 20;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String file_loc = "/sdcard/dropbox/school/adiview_stuff/working_images/CT-MONO2-8-abdo.dcm";

        try {
            File file = new File(file_loc);

            DicomInputStream dis = new DicomInputStream(file);


            BasicDicomObject bdo = new BasicDicomObject();
            ElementDictionary dict = ElementDictionary.getDictionary();
            AssetManager asm = getAssets();


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
//        finish();

    }

    private void extractXMLtoSDCard(){

    }

    @Override
    public boolean readValue(DicomInputStream in) throws IOException {
        switch (in.tag()){
            case Tag.Item:
                if (in.sq().vr() != VR.SQ && in.valueLength() != -1) {
                    outFragment(in);
                } else {
                    outItem(in);
                }
                break;
            case Tag.ItemDelimitationItem:
            case Tag.SequenceDelimitationItem:
                if (in.level() > 0)
                    outItem(in);
                break;
            default:
                outElement(in);
        }

        return true;

    }

    private void outItem(DicomInputStream in) throws IOException{
        in.readValue(in);
    }

    public void outFragment(DicomInputStream in) throws IOException{
        in.readValue(in);
        in.readValue(in);
        DicomElement sq = in.sq();
        byte[] data = sq.removeFragment(0);
        boolean bigEndian = in.getTransferSyntax().bigEndian();
        StringBuffer line = new StringBuffer();
        sq.vr().promptValue(data, bigEndian, null, cbuf, maxValLen, line);
        RowModel row = new RowModel(in.tag(),line.toString());
        info.add(row);

    }

    public void outElement(DicomInputStream in) throws IOException{

        if (hasItems(in)) {
//            outLine(in);
            readItems(in);
        } else {
            outValue(in);
//            outLine(in);
        }


    }

     private void outValue(DicomInputStream in) throws IOException {
        int tag = in.tag();
        VR vr = in.vr();
        byte[] val = in.readBytes(in.valueLength());
        DicomObject dcmobj = in.getDicomObject();
        boolean bigEndian = in.getTransferSyntax().bigEndian();
        StringBuffer line = new StringBuffer();
        vr.promptValue(val, bigEndian, dcmobj.getSpecificCharacterSet(),
                cbuf, maxValLen, line);
        RowModel row = new RowModel(tag,line.toString());
        info.add(row);

        if (tag == Tag.SpecificCharacterSet
                || tag == Tag.TransferSyntaxUID
                || TagUtils.isPrivateCreatorDataElement(tag)) {
            dcmobj.putBytes(tag, vr, val, bigEndian);
        }
        if (tag == 0x00020000) {
            in.setEndOfFileMetaInfoPosition(
                    in.getStreamPosition() + vr.toInt(val, bigEndian));
        }
    }

     private void readItems(DicomInputStream in) throws IOException {
        in.readValue(in);
        in.getDicomObject().remove(in.tag());
    }

    private boolean hasItems(DicomInputStream in) {
        return in.valueLength() == -1 || in.vr() == VR.SQ;
    }

    private String outLine(DicomInputStream in) {
        return in.getDicomObject().nameOf(in.tag());
    }


    class RowModel {
        String description;
        String value;
        int tag;

        public RowModel(int tag, String value){
            this.tag = tag;
            this.value = value;
        }

        public String toString() {
            return "desc:" + getDescription() + " val:" + getValue();
        }

        public String getDescription() {
            description = Integer.toHexString(tag);

            switch(tag){
                case Tag.PatientAge:
                    break;
                default:
                    description = DCM4CheTagNameHack.getTagName(tag);
            }

            if (description == null){
                description = "N/A";
            };

            return description;
        }

        public String getValue() {
            return value;
        }

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