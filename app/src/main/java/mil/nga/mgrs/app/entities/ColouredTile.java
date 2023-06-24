package mil.nga.mgrs.app.entities;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;

import java.util.List;

public class ColouredTile {
    List<LatLng> tilesCorner;
    String mgrs;
    Polygon polygonTile;

    public ColouredTile(List<LatLng> tilesCorner, String mgrs, Polygon polygonTile) {
        this.tilesCorner = tilesCorner;
        this.mgrs = mgrs;
        this.polygonTile = polygonTile;
    }

    public ColouredTile(List<LatLng> tilesCorner, String mgrs) {
        this.tilesCorner = tilesCorner;
        this.mgrs = mgrs;
    }

    public ColouredTile() {
    }

    public List<LatLng> getTilesCorner() {
        return tilesCorner;
    }

    public void setTilesCorner(List<LatLng> tilesCorner) {
        this.tilesCorner = tilesCorner;
    }

    public String getMgrs() {
        return mgrs;
    }

    public void setMgrs(String mgrs) {
        this.mgrs = mgrs;
    }

    public Polygon getPolygonTile() {
        return polygonTile;
    }

    public void setPolygonTile(Polygon polygonTile) {
        this.polygonTile = polygonTile;
    }
}
