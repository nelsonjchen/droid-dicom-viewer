package be.ac.ulb.lisa.idot.android.dicomviewer;

import android.app.ListActivity;
import android.os.Bundle;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class DICOMFileInfo extends ListActivity {

    private static LinkedHashMap<String,String> map ;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dicom_info_viewer);
        map = new LinkedHashMap<String, String>();
        map.put("lol", "lol");
        map.put("2nd amendment","AMENDMENTED");


    }
}