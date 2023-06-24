package mil.nga.mgrs.app.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.grid.GridType;

@Entity
public class Observation implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String mgrs;
    public int lte;
    public int wifi;
    public int noise;
    public int AVG;

    @ColumnInfo
    Long created_at;


    public Observation(String mgrs, int lte , int wifi , int noise){

        this.mgrs = mgrs;
        this.noise = noise;
        this.wifi = wifi;
        this.lte = lte;
        this.created_at = new Date().getTime();

    }

    public Observation(){
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMgrs() {
        return mgrs;
    }

    public void setMgrs(String mgrs) {
        this.mgrs = mgrs;
    }

    public int getLte() {
        return lte;
    }

    public void setLte(int lte) {
        this.lte = lte;
    }

    public int getWifi() {
        return wifi;
    }

    public void setWifi(int wifi) {
        this.wifi = wifi;
    }

    public int getNoise() {
        return noise;
    }

    public void setNoise(int noise) {
        this.noise = noise;
    }

    public Long getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Long created_at) {
        this.created_at = created_at;
    }
}