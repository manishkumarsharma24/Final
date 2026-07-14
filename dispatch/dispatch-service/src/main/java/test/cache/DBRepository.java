package test.cache;

import java.util.HashMap;
import java.util.Map;

public class DBRepository {

    private static Map<Integer, String> dbMap = new HashMap<>();

    public static String readFromDB(Integer id){
        return dbMap.get(id);
    }
}
