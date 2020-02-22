package karate.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelloKarate {

    public int get(){
        return 1234;
    }

    public Map<String,String> getMap(){
        HashMap<String,String> map = new HashMap<>();
        map.put("key", "KKK");
        map.put("value", "VVV");
        return map;
    }

    public List<Map<String,String>> getList(){
        ArrayList<Map<String,String>> list = new ArrayList<>();

        for(int i=1; i<4; i++) {
            HashMap<String,String> map = new HashMap<>();
            map.put("value", new Integer(i*10).toString());
            list.add(map);
        }
        return list;
    }
}
