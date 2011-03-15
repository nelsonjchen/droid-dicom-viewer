package be.ac.ulb.lisa.idot.android.dicomviewer;


import android.app.ListActivity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import be.ac.ulb.lisa.idot.android.dicomviewer.data.DCM4CheTagNameHack;
import org.dcm4che2.data.*;
import org.dcm4che2.io.DicomInputHandler;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.util.TagUtils;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;


public class DICOMFileInfo extends ListActivity implements DicomInputHandler {
    ArrayList<RowModel> info;
    char[] cbuf = new char[64];
    int maxValLen = 20;
    String currentFileName = null;
    private static final String FILE_NAME = "file_name";


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dicom_info_viewer);
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
                    } catch (URISyntaxException e) {

                    } catch (NullPointerException e) {
                        fileName = null;
                    }
                }
            }

        }

        // If the file name is null, alert the user and close
        // the activity
        if (fileName == null) {
            Toast.makeText(this, "Filename is null" ,Toast.LENGTH_LONG).show();
            finish();
            // Load the file
        } else {

            try {
                File file = new File(fileName);

                DicomInputStream dis = new DicomInputStream(file);


                BasicDicomObject bdo = new BasicDicomObject();
                ElementDictionary dict = ElementDictionary.getDictionary();
                AssetManager asm = getAssets();


                info = new ArrayList<RowModel>();

                dis.setHandler(this);
                dis.readDicomObject(bdo, -1);

                dis.close();

                setListAdapter(new DICOMMetaAdapter());
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (EOFException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }


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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current file name
        outState.putString(FILE_NAME, currentFileName);
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

                row = inflater.inflate(R.layout.row_dicom_info,parent,false);
            }

            TextView desc_label = (TextView) row.findViewById(R.id.tagdesc);
            desc_label.setText(info.get(position).getDescription());
            TextView val_label = (TextView) row.findViewById(R.id.tagval);
            val_label.setText(info.get(position).getValue());

            return(row);
        }
    }
}