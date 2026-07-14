package test.cache;

import java.util.Date;

public class CacheEntry {

    int id;

    String value;

    Date date;

    public CacheEntry(int id, String value, Date date) {
        this.id = id;
        this.value = value;
        this.date = date;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
