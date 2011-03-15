package be.ac.ulb.lisa.idot.android.dicomviewer.data;

import org.dcm4che2.data.Tag;

import java.lang.reflect.Field;
import java.util.HashMap;

public class DCM4CheTagNameHack {
    static HashMap<Integer, String> map;

    private DCM4CheTagNameHack(){}

    static {
        map = new HashMap<Integer, String>();
        Class tagClass = Tag.class;
        Field[] field = tagClass.getFields();
        for (Field f : field) {
            int tag_val = 0;
            try {
                tag_val = f.getInt(tagClass);
            } catch (IllegalAccessException e) {
                tag_val = -1;
            }
            String tag_name = f.getName();
            map.put(new Integer(tag_val), tag_name);
        }
    }

    public HashMap getMap(){
        return map;
    }

    public static String getTagName(int i){
        String description = map.get(new Integer(i));
        return description;
    }
}
