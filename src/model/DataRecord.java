package model;

import java.io.*;

public abstract class DataRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    protected int id;
    protected String district;

    public DataRecord(int id, String district) {
        this.id = id;
        this.district = district;
    }

    public int getId() {
        return id;
    }

    public String getDistrict() {
        return district;
    }

    public void setId() {
        this.id = id;
    }

    public void setDistrict() {
        this.district = district;
    }

    public abstract String getSummary();
}
