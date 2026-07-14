package test.cache;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.WeakHashMap;

public class LRUCache {

    private LinkedHashMap<Integer,CacheEntry> map = new LinkedHashMap<>();

    private int maxSize = 1000;

    public LRUCache(int maxSize) {
        this.maxSize = maxSize;
    }

    public void updateCache(int key, String value){
        if(map.size() == maxSize ){
            deleteLRUEntry();
        }

        CacheEntry cacheEntry = new CacheEntry(key,value, new Date());
        map.put(key,cacheEntry);
    }

    private void deleteLRUEntry(){
        Integer idLRU =-100;
        Date leastDate = null;

        for(Integer key : map.keySet()){
            Date date = map.get(key).getDate();
            if(leastDate == null){
                leastDate = date;
            }
            if(date.getTime() < leastDate.getTime()){
                leastDate = date;
                idLRU = key;
            }

        }

        if(idLRU != -100 && idLRU != null){
            map.remove(idLRU);
        }

    }

    public String search(Integer key){
        if(!map.containsKey(key)){
            String value = DBRepository.readFromDB(key);
            updateCache(key,value);
        }
        return map.get(key).getValue();
    }

}
