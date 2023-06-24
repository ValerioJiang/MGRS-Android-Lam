package mil.nga.mgrs.app;

import static android.content.ContentValues.TAG;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import mil.nga.grid.features.Point;
import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.app.entities.ColouredTile;
import mil.nga.mgrs.app.entities.Observation;
import mil.nga.mgrs.app.fragment.ProgressBarFragment;
import mil.nga.mgrs.app.singleton.AppDatabase;
import mil.nga.mgrs.grid.GridType;
import mil.nga.mgrs.grid.style.Grids;
import mil.nga.mgrs.tile.MGRSTileProvider;
import mil.nga.mgrs.tile.TileUtils;
import mil.nga.mgrs.utm.UTM;

/**
 * @author Valerio Jiang
 */

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMapClickListener {

    /**
     * Location permission request code
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Google map
     */
    private GoogleMap map;

    /**
     * MGRS label
     */
    private TextView mgrsLabel;

    /**
     * WGS84 coordinate label
     */
    private TextView wgs84Label;

    /**
     * Zoom label
     */
    private TextView zoomLabel;

    /**
     * Search MGRS result
     */
    private String searchMGRSResult = null;

    /**
     * Coordinate label formatter
     */
    private final DecimalFormat coordinateFormatter = new DecimalFormat("0.0####");

    /**
     * Zoom level label formatter
     */
    private final DecimalFormat zoomFormatter = new DecimalFormat("0.0");

    /**
     * MGRS tile provider
     */
    private MGRSTileProvider tileProvider = null;

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 101;

    Map<String, Integer> precisionLength = Map.ofEntries(Map.entry("METER", 10), Map.entry("TEN_METER", 8), Map.entry("HUNDRED_METER", 6), Map.entry("KILOMETER", 4));

    Map<String, Integer> zoomPrecisionLength = Map.ofEntries(Map.entry("METER", 21), Map.entry("TEN_METER", 18), Map.entry("HUNDRED_METER", 16), Map.entry("KILOMETER", 14));

    List<String> availableZooms = Arrays.asList("TEN_METER", "HUNDRED_METER", "KILOMETER");

    //Mappa dove viene indicato stringa, e lunghezza del mgrs
    Map<String, Integer> currentZoomLevelLengthMGRS = Map.ofEntries(Map.entry("TEN_METER", 13), Map.entry("HUNDRED_METER", 11), Map.entry("KILOMETER", 9));
    String currentZoomLevel = "TEN_METER";

    LatLng southWest, northWest, southEast, northEast;

    Polygon findTilePolygon, currentPolygon;


    //all coloured tiles, identified by
    Map<String, ColouredTile> colouredTile = new HashMap<>();


    private final int FINE_PERMISSION_CODE = 1;


    List<String> tipologiaMappa = Arrays.asList("NOISE", "LTE", "WIFI", "NESSUNO");

    String tipologiaMappaCorrente = "NESSUNO";

    List<String> list;

    View mapView;

    List<Observation> currentObservations;

    Fragment fragment;
    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;

    AppDatabase appDatabase;

    MGRS currentMGRSlocation;

    String mgrsToQuery;

    Map<Observation, Polygon> currentColoredPolygonTiles = new HashMap<>();

    /**
     * PERMISSION CHECKER
     */

    private static final String PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
    private static final String PERMISSION_ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String PERMISSION_ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String PERMISSION_WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final int PERMISSION_REQ_CODE = 100;

    public List<LatLng> findTilesCornerCoordinates(MGRS mgrs, GridType gridType) {
        List<LatLng> response = new ArrayList<>();
        String northing = mgrs.getEastingAndNorthing(gridType).substring(0, mgrs.getEastingAndNorthing(gridType).length() / 2);
        String easting = mgrs.getEastingAndNorthing(gridType).substring(mgrs.getEastingAndNorthing(gridType).length() / 2);

        Long northWestNum = Long.valueOf(northing) + 1;
        Long southEastNum = Long.valueOf(easting) + 1;

        northing = String.format("%0" + mgrs.getEastingAndNorthing(gridType).length() / 2 + "d", northWestNum);
        easting = String.format("%0" + mgrs.getEastingAndNorthing(gridType).length() / 2 + "d", southEastNum);
        if (mgrs.getEastingAndNorthing(gridType).length() >= precisionLength.get(gridType.name())) {
            try {

                MGRS southWestMgrs = MGRS.parse(mgrs.getZone() + String.valueOf(mgrs.getBand()) + mgrs.getColumnRowId() + mgrs.getEastingAndNorthing(gridType).substring(0, mgrs.getEastingAndNorthing(gridType).length() / 2) + mgrs.getEastingAndNorthing(gridType).substring(mgrs.getEastingAndNorthing(gridType).length() / 2));// MGRS.create(mgrs.getZone(), mgrs.getBand(), mgrs.getColumn(), mgrs.getRow(),  northWestNum, mgrs.getEasting());
                MGRS northWestMgrs = MGRS.parse(mgrs.getZone() + String.valueOf(mgrs.getBand()) + mgrs.getColumnRowId() + mgrs.getEastingAndNorthing(gridType).substring(0, mgrs.getEastingAndNorthing(gridType).length() / 2) + easting);// MGRS.create(mgrs.getZone(), mgrs.getBand(), mgrs.getColumn(), mgrs.getRow(),  northWestNum, mgrs.getEasting());
                MGRS southEastMgrs = MGRS.parse(mgrs.getZone() + String.valueOf(mgrs.getBand()) + mgrs.getColumnRowId() + northing + mgrs.getEastingAndNorthing(gridType).substring(mgrs.getEastingAndNorthing(gridType).length() / 2));//MGRS.create(mgrs.getZone(), mgrs.getBand(), mgrs.getColumn(), mgrs.getRow(),  mgrs.getNorthing(), southEastNum);
                MGRS northEastMgrs = MGRS.parse(mgrs.getZone() + String.valueOf(mgrs.getBand()) + mgrs.getColumnRowId() + northing + easting);//MGRS.create(mgrs.getZone(), mgrs.getBand(), mgrs.getColumn(), mgrs.getRow(),  mgrs.getNorthing(), southEastNum);
                southWest = new LatLng(southWestMgrs.toPoint().getLatitude(), southWestMgrs.toPoint().getLongitude());
                northWest = new LatLng(northWestMgrs.toPoint().getLatitude(), northWestMgrs.toPoint().getLongitude());
                southEast = new LatLng(southEastMgrs.toPoint().getLatitude(), southEastMgrs.toPoint().getLongitude());
                northEast = new LatLng(northEastMgrs.toPoint().getLatitude(), northEastMgrs.toPoint().getLongitude());
                response.add(southWest);
                response.add(northWest);
                response.add(northEast);
                response.add(southEast);
            } catch (Exception e) {
                Log.e(MapsActivity.class.getSimpleName(),
                        "Unsupported coordinate: " + e);
            }
        } else {
            Log.e(MapsActivity.class.getSimpleName(),
                    "Unsupported coordinate: " + mgrs.coordinate() + " With Gridtype: " + gridType.name());
        }
        return response;
    }

    public void populateDB() {

        Observable insertInit = appDatabase.observationDAO().insert(Arrays.asList(
                new Observation("32TPQ62017201", 1, 1, 2),
                new Observation("32TPC62017201", 1, 1, 2),
                new Observation("32MNC62017201", 1, 1, 2),
                new Observation("32MNC62027201", 1, 1, 2),
                new Observation("32MNC62037201", 1, 1, 2),
                new Observation("38PMT8758022980", 1, 1, 2),
                new Observation("32MNC62047201", 1, 1, 2),
                new Observation("32MNC62057201", 1, 1, 2),
                new Observation("32TPZ62067201", 1, 1, 2),
                new Observation("32TPQ62087202", 2, 3, 2),
                new Observation("32TPQ62037203", 1, 1, 1),
                new Observation("32TPQ62047204", 3, 2, 1),
                new Observation("32TPQ62057205", 1, 3, 2),
                new Observation("32TPQ62067206", 2, 2, 1),
                new Observation("32TPQ62077207", 3, 1, 3),
                new Observation("32TPQ62087208", 1, 2, 2),
                new Observation("32TPQ62097209", 2, 1, 1),
                new Observation("32TPQ62107210", 3, 3, 2),
                new Observation("32TPQ62117211", 1, 1, 1),
                new Observation("32TPQ62127212", 2, 3, 1),
                new Observation("32TPQ62137213", 3, 2, 2),
                new Observation("32TPQ62947239", 3, 2, 2),
                new Observation("32TPQ62147214", 1, 3, 3),
                new Observation("32TPQ62727232", 1, 3, 3),
                new Observation("32TPQ62727232", 1, 3, 3),
                new Observation("32TPQ62157215", 2, 2, 2),
                new Observation("32TPQ62167216", 3, 1, 1),
                new Observation("32TPQ62167216", 3, 1, 1),
                new Observation("32TPQ62947239", 3, 1, 1),
                new Observation("32TPQ62177217", 1, 2, 3),
                new Observation("32TPQ62187218", 2, 1, 3),
                new Observation("32TPQ62197219", 3, 3, 1),
                new Observation("32TPQ62727232", 3, 3, 1),
                new Observation("32TPQ62207220", 1, 1, 2),
                new Observation("32TPQ62217221", 2, 3, 3),
                new Observation("32TPQ62227222", 3, 2, 1),

                new Observation("32TPQ62947239", 3, 2, 1),
                new Observation("32TPQ62937239", 3, 2, 1),
                new Observation("32TPQ62927239", 3, 2, 1),
                new Observation("32TPQ62917239", 3, 2, 1),
                new Observation("32TPQ62917239", 1, 2, 3),
                new Observation("32TPQ62917238", 1, 2, 3),
                new Observation("32TPQ62917237", 1, 2, 3),
                new Observation("32TPQ62917236", 1, 2, 2),


                new Observation("32TPQ62917235", 3, 1, 3),
                new Observation("32TPQ62917235", 1, 1, 3),
                new Observation("32TPQ62917235", 1, 1, 3),
                new Observation("32TPQ62917235", 1, 1, 3),
                new Observation("32TPQ62917235", 1, 1, 3),


                new Observation("32TPQ62947235", 1, 2, 3),

                new Observation("32TPQ62237223", 1, 3, 2),
                new Observation("32TPQ62247224", 2, 2, 3),
                new Observation("32TPQ62257225", 3, 1, 1),
                new Observation("32TPQ62727232", 3, 1, 1),
                new Observation("32TPQ62267226", 1, 2, 2),
                new Observation("32TPQ62277227", 2, 1, 1),
                new Observation("32TPQ62287228", 3, 3, 2),
                new Observation("32TPQ62947239", 3, 3, 2),
                new Observation("32TPQ62297229", 1, 1, 1),
                new Observation("32TPQ62307230", 2, 3, 2),
                new Observation("32TPQ62317231", 3, 2, 1),
                new Observation("32TPQ62327232", 1, 3, 3),
                new Observation("32TPQ62337233", 2, 2, 2),
                new Observation("32TPQ62347234", 3, 1, 1),
                new Observation("32TPQ62357235", 1, 2, 3),
                new Observation("32TPQ62367236", 2, 1, 2),
                new Observation("32TPQ62377237", 3, 3, 1),
                new Observation("32TPQ62387238", 1, 1, 2),
                new Observation("32TPQ62397239", 2, 3, 3),
                new Observation("32TPQ62407240", 3, 2, 3),
                new Observation("32TPQ62417241", 1, 3, 1),
                new Observation("32TPQ62427242", 2, 2, 2),
                new Observation("32TPQ62947239", 2, 2, 2),
                new Observation("32TPQ62437243", 3, 1, 2),
                new Observation("32TPQ62447244", 1, 2, 3),
                new Observation("32TPQ62457245", 2, 1, 1),
                new Observation("32TPQ62467246", 3, 3, 3),
                new Observation("32TPQ62477247", 1, 1, 2),
                new Observation("32TPQ62487248", 2, 3, 3),
                new Observation("32TPQ62497249", 3, 2, 2),
                new Observation("38PMT8758022980", 3, 2, 2),
                new Observation("32TPQ62507250", 1, 3, 1),
                new Observation("32TPQ62517251", 2, 2, 2),
                new Observation("32TPQ62527252", 3, 1, 2),
                new Observation("32TPQ62537253", 1, 2, 1),
                new Observation("32TPQ62547254", 2, 1, 2),
                new Observation("32TPQ62557255", 3, 3, 3),
                new Observation("32TPQ62567256", 1, 1, 1),
                new Observation("32TPQ62577257", 2, 3, 1),
                new Observation("32TPQ62587258", 3, 2, 2),
                new Observation("32TPQ62597259", 1, 3, 3),
                new Observation("32TPQ62607260", 2, 2, 1),
                new Observation("32TPQ62617261", 3, 1, 2),
                new Observation("32TPQ62627262", 1, 2, 1),
                new Observation("38PMT8758022980", 1, 2, 1),
                new Observation("32TPQ62637263", 2, 1, 3),
                new Observation("32TPQ62647264", 3, 3, 1),
                new Observation("32TPQ62657265", 1, 1, 3),
                new Observation("32TPQ62667266", 2, 3, 2),
                new Observation("32TPQ62677267", 3, 2, 3),
                new Observation("32TPQ62687268", 1, 3, 2),
                new Observation("32TPQ62697269", 2, 2, 1),
                new Observation("32TPQ62707270", 3, 1, 1),
                new Observation("32TPQ62717271", 1, 2, 2),
                new Observation("32TPQ62727272", 2, 1, 3),
                new Observation("32TPQ62737273", 3, 3, 2),
                new Observation("32TPQ62747274", 1, 1, 3),
                new Observation("32TPQ62757275", 2, 3, 1),
                new Observation("32TPQ62767276", 3, 2, 2),
                new Observation("32TPQ62777277", 1, 3, 3),
                new Observation("32TPQ62787278", 2, 2, 3),
                new Observation("32TPQ62797279", 3, 1, 1),
                new Observation("32TPQ62807280", 1, 2, 3),
                new Observation("38PMT8758022980", 1, 2, 3),
                new Observation("32TPQ62817281", 2, 1, 1),
                new Observation("32TPQ62827282", 3, 3, 2),
                new Observation("32TPQ62837283", 1, 1, 1),
                new Observation("38PMT8758022980", 1, 1, 1),
                new Observation("32TPQ62847284", 2, 3, 1),
                new Observation("32TPQ62857285", 3, 2, 3),
                new Observation("32TPQ62867286", 1, 3, 1),
                new Observation("32TPQ62877287", 2, 2, 1),
                new Observation("32TPQ62887288", 3, 1, 2),
                new Observation("32TPQ62897289", 1, 2, 1),
                new Observation("32TPQ62907290", 2, 1, 3),
                new Observation("32TPQ62917291", 3, 3, 1),
                new Observation("32TPQ62927292", 1, 1, 3),
                new Observation("32TPQ62937293", 2, 3, 2),
                new Observation("32TPQ62947294", 3, 2, 3),
                new Observation("32TPQ62957295", 1, 3, 2),
                new Observation("32TPQ62967296", 2, 2, 1),
                new Observation("32TPQ62977297", 3, 1, 2),
                new Observation("32TPQ62987298", 1, 2, 1),
                new Observation("32TPQ62997299", 2, 3, 3),
                new Observation("32TPQ63007300", 3, 2, 3),
                new Observation("32TPQ63017301", 1, 3, 1),
                new Observation("32TPQ63027302", 2, 2, 3),
                new Observation("32TPQ63037303", 3, 1, 3),
                new Observation("32TPQ63047304", 1, 2, 2),
                new Observation("32TPQ63057305", 2, 1, 1),
                new Observation("32TPQ63067306", 3, 3, 2),
                new Observation("32TPQ63077307", 1, 1, 3),
                new Observation("32TPQ63087308", 2, 3, 2),
                new Observation("32TPQ63097309", 3, 2, 3),
                new Observation("32TPQ63107310", 1, 3, 1),
                new Observation("32TPQ63117311", 2, 2, 2),
                new Observation("32TPQ63127312", 3, 1, 2),
                new Observation("32TPQ63137313", 1, 2, 1),
                new Observation("32TPQ63147314", 2, 1, 2),
                new Observation("32TPQ63147314", 2, 1, 2),
                new Observation("32TPQ62947239", 2, 1, 2),
                new Observation("32TPQ63157315", 3, 3, 3),
                new Observation("32TPQ63167316", 1, 1, 1),
                new Observation("32TPQ62947239", 1, 1, 1),
                new Observation("32TPQ63177317", 2, 3, 1),
                new Observation("32TPQ63187318", 3, 2, 2),
                new Observation("32TPQ63197319", 1, 3, 3),
                new Observation("32TPQ62947239", 1, 3, 3),
                new Observation("32TPQ63207320", 2, 2, 1),
                new Observation("32TPQ63217321", 3, 1, 2)
        )).subscribeOn(Schedulers.io()).toObservable();


        Observable selectAll = appDatabase.observationDAO().getAllObservationList().subscribeOn(Schedulers.io()).toObservable();

        Observable.merge(insertInit
                , selectAll).subscribe(result -> {
            if (result != null) {
                //allObservations.addAll((Collection<? extends Observation>) result);
            }
        });


    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appDatabase = AppDatabase.getInstance(this);
        populateDB();

        /**
         * Far funzionare il fragment
         */
        requestRuntimePermission();


        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int numberOfLevels = 5;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);


        /**
         * Excellent >-50 dBm
         *
         * Good -50 to -60 dBm
         *
         * Fair -60 to -70 dBm
         *
         * Weak < -70 dBm
         */


        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String strength = null;

        /**
         * RF Quality	RSRP (dbm)
         * Excellent	>= -80
         * Good	        -80 to -90
         * 'Mid Cell'	-90 to -100
         * 'Cell Edge'	< -100
         */


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();   //This will give info of all sims present inside your mobile
        if(cellInfos != null) {
            for (int i = 0; i < cellInfos.size(); i++) {
                if (cellInfos.get(i).isRegistered()) {
                    if (cellInfos.get(i) instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                        CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                        strength = String.valueOf(cellSignalStrengthLte.getDbm());
                    }
                }
            }
        }

        /**
         * spec
         * , for all three types of
         * measurements, the student should identify a scale with at least 3 different ranges
         * identifying, for instance, the classes “poor”, “average” and “good” (for the signal
         * strength) or “loud”, “average” and “quiet” (for the sound)
         */




    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomGesturesEnabled(false);
        map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
        map.setOnCameraIdleListener(this);
        map.setOnMapClickListener(this);


        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
                CameraUpdate update;

                update = CameraUpdateFactory.newLatLngZoom(new LatLng(place.getLatLng().latitude, place.getLatLng().longitude), map.getCameraPosition().zoom);

                map.animateCamera(update);

            }


            @Override
            public void onError(@NonNull Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        /**
         * MUOVE il current location sotto la barra di ricerca
         */
        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 30);
        }


    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCameraIdle() {
        CameraPosition cameraPosition = map.getCameraPosition();
        LatLng center = cameraPosition.target;
        float zoom = cameraPosition.zoom;
        String mgrs;
        if (searchMGRSResult != null) {
            mgrs = searchMGRSResult;
            searchMGRSResult = null;
        } else {
            mgrs = tileProvider.getCoordinate(center, (int) zoom);
        }
        mgrsLabel.setText(mgrs);

        try {
            currentMGRSlocation = MGRS.parse(mgrs);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        wgs84Label.setText(getString(R.string.wgs84_label_format,
                coordinateFormatter.format(center.longitude),
                coordinateFormatter.format(center.latitude)));


        zoomLabel.setText(zoomFormatter.format(zoom));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        map.setMyLocationEnabled(true);

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            currentLocation = location;
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), zoomPrecisionLength.get(currentZoomLevel)));


                        }

                    }
                });

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    /**
     * Handle map type click
     */
    private void onMapTypeClick() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.map_type_title);

        // Add an OnClickListener to the dialog, so that the selection will be handled.
        builder.setSingleChoiceItems(
                new CharSequence[]{
                        getString(R.string.map_type_normal),
                        getString(R.string.map_type_satellite),
                        getString(R.string.map_type_terrain),
                        getString(R.string.map_type_hybrid)},
                map.getMapType() - 1,
                (dialog, item) -> {
                    map.setMapType(item + 1);
                    dialog.dismiss();
                }
        );

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

    }

    /**
     * Zoom selection click
     */
    private void onZoomLevelClick() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.zoom_level_dialog_title);

        // Add an OnClickListener to the dialog, so that the selection will be handled.
        builder.setSingleChoiceItems(
                new CharSequence[]{
                        "TEN_METER",
                        "HUNDRED_METER",
                        "KILOMETER"
                },
                availableZooms.indexOf(currentZoomLevel),
                (dialog, item) -> {
                    CameraUpdate update;
                    currentZoomLevel = availableZooms.get(item);

                    int zoom = zoomPrecisionLength.get(currentZoomLevel);
                    CameraPosition cameraPosition = map.getCameraPosition();
                    LatLng center = cameraPosition.target;

                    update = CameraUpdateFactory.newLatLngZoom(new LatLng(center.latitude, center.longitude), zoom);
                    map.animateCamera(update);


                    dialog.dismiss();
                }
        );

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

    }


    /**
     * Copy text to the clipboard
     *
     * @param label label
     * @param text  text
     */
    private void copyToClipboard(String label, CharSequence text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);

    }

    /**
     * ImageButtons listeners
     */

    private void onSearchClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.search_title);
        final EditText input = new EditText(this);
        input.setSingleLine();
        builder.setView(input);

        builder.setPositiveButton(R.string.search, (dialog, which) -> search(input.getText().toString()));
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void removeColoredTiles(){
        for (Map.Entry<Observation, Polygon> entry : currentColoredPolygonTiles.entrySet()) {
            if(entry.getValue() != null){
                Polygon polygon = entry.getValue();
                polygon.remove();
                entry.getValue().remove();
            }
        }
    }


    private void onSettingsClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Selezionare tipologia di mappa
        builder.setTitle(R.string.settings_dialog_title);

        try {
            MGRS currentMgrs = MGRS.parse(tileProvider.getCoordinate(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), zoomPrecisionLength.get(currentZoomLevel).intValue()));

            mgrsToQuery = currentMgrs.coordinate(GridType.valueOf(currentZoomLevel));

            String formattedEasting = currentMgrs.getEastingAndNorthing(GridType.valueOf(currentZoomLevel)).substring(0, currentMgrs.getEastingAndNorthing(GridType.valueOf(currentZoomLevel)).length() / 2 - 1);
            String formattedNorthing = currentMgrs.getEastingAndNorthing(GridType.valueOf(currentZoomLevel)).substring(currentMgrs.getEastingAndNorthing(GridType.valueOf(currentZoomLevel)).length() / 2, currentMgrs.getEastingAndNorthing(GridType.valueOf(currentZoomLevel)).length() - 1);

            mgrsToQuery = (String.valueOf(currentMgrs.getZone()) + currentMgrs.getBand() + currentMgrs.getColumn() + currentMgrs.getRow() + "%" + formattedEasting + "_%%" + formattedNorthing + "_%");


            // Add an OnClickListener to the dialog, so that the selection will be handled.
            builder.setSingleChoiceItems(
                    new CharSequence[]{
                            "NOISE",
                            "LTE",
                            "WIFI",
                            "NESSUNO"
                    },
                    tipologiaMappa.indexOf(tipologiaMappaCorrente),
                    (dialog, item) -> {
                        //32TPQ%62_%72_% for ten meter 11
                        //32TPQ%6294%%7239%
                        if(tipologiaMappa.get(item).equals("LTE")){
                            appDatabase.observationDAO().getAvgLTEObservationColumn(mgrsToQuery).subscribeOn(Schedulers.computation())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            new SingleObserver<List<Observation>>() {
                                                @Override
                                                public void onSubscribe(Disposable d) {
                                                    fragment = new ProgressBarFragment();
                                                    fragmentManager = getSupportFragmentManager();
                                                    fragmentTransaction = fragmentManager.beginTransaction();
                                                    fragmentTransaction.replace(R.id.fragmentContainer, fragment);
                                                    fragmentTransaction.commit();
                                                }

                                                @Override
                                                public void onSuccess(List<Observation> observations) {
                                                    removeColoredTiles();
                                                    fragmentManager.beginTransaction().remove(fragment).commit();

                                                    currentObservations = observations;

                                                    //usare

                                                    int fillTileColor = Color.argb(0, 0, 0, 0);


                                                    //Color.argb(100,0,255,0) rosso 1 male
                                                    //Color.argb(100,0,255,0) marrone 2 normale
                                                    //Color.argb(100,255,0,0) verde 3 buono


                                                    for (Observation y : currentObservations) {
                                                        MGRS mgrsCurrObs = null;
                                                        try {
                                                            mgrsCurrObs = MGRS.parse(y.getMgrs());
                                                        } catch (ParseException e) {
                                                            e.printStackTrace();
                                                        }
                                                        if (y.AVG == 1) {
                                                            fillTileColor = Color.argb(100, 255, 0, 0);
                                                        } else if (y.AVG == 2) {
                                                            fillTileColor = Color.argb(100, 165, 42, 42);
                                                        } else if (y.AVG == 3) {
                                                            fillTileColor = Color.argb(100, 0, 255, 0);
                                                        }

                                                        PolygonOptions polygonOptions = new PolygonOptions();

                                                        List<LatLng> tileCornersLatLng = findTilesCornerCoordinates(mgrsCurrObs, GridType.valueOf(currentZoomLevel));

                                                        polygonOptions.addAll(tileCornersLatLng);
                                                        polygonOptions.strokeColor(Color.RED)
                                                                .fillColor(fillTileColor);

                                                        Polygon polygon = currentColoredPolygonTiles.get(y);

                                                        if (polygon != null) {
                                                            polygon.remove();
                                                        }

                                                        polygon = map.addPolygon(new PolygonOptions()
                                                                .add(tileCornersLatLng.get(0), tileCornersLatLng.get(1), tileCornersLatLng.get(2), tileCornersLatLng.get(3))
                                                                .strokeColor(Color.RED)
                                                                .fillColor(fillTileColor));

                                                        currentColoredPolygonTiles.put(y, polygon);


                                                    }


                                                    tipologiaMappaCorrente = tipologiaMappa.get(item);
                                                    dialog.dismiss();
                                                }

                                                @Override
                                                public void onError(Throwable e) {
                                                    removeColoredTiles();
                                                    dialog.dismiss();
                                                }
                                            }
                                    );

                        }

                        else if(tipologiaMappa.get(item).equals("WIFI")){
                            appDatabase.observationDAO().getAvgWIFIObservationColumn(mgrsToQuery).subscribeOn(Schedulers.computation())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            new SingleObserver<List<Observation>>() {
                                                @Override
                                                public void onSubscribe(Disposable d) {
                                                    fragment = new ProgressBarFragment();
                                                    fragmentManager = getSupportFragmentManager();
                                                    fragmentTransaction = fragmentManager.beginTransaction();
                                                    fragmentTransaction.replace(R.id.fragmentContainer, fragment);
                                                    fragmentTransaction.commit();
                                                }

                                                @Override
                                                public void onSuccess(List<Observation> observations) {
                                                    removeColoredTiles();
                                                    fragmentManager.beginTransaction().remove(fragment).commit();

                                                    currentObservations = observations;

                                                    //usare

                                                    int fillTileColor = Color.argb(0, 0, 0, 0);


                                                    //Color.argb(100,0,255,0) rosso 1 male
                                                    //Color.argb(100,0,255,0) marrone 2 normale
                                                    //Color.argb(100,255,0,0) verde 3 buono


                                                    for (Observation y : currentObservations) {
                                                        MGRS mgrsCurrObs = null;
                                                        try {
                                                            mgrsCurrObs = MGRS.parse(y.getMgrs());
                                                        } catch (ParseException e) {
                                                            e.printStackTrace();
                                                        }
                                                        if (y.AVG == 1) {
                                                            fillTileColor = Color.argb(100, 255, 0, 0);
                                                        } else if (y.AVG == 2) {
                                                            fillTileColor = Color.argb(100, 165, 42, 42);
                                                        } else if (y.AVG == 3) {
                                                            fillTileColor = Color.argb(100, 0, 255, 0);
                                                        }

                                                        PolygonOptions polygonOptions = new PolygonOptions();

                                                        List<LatLng> tileCornersLatLng = findTilesCornerCoordinates(mgrsCurrObs, GridType.valueOf(currentZoomLevel));

                                                        polygonOptions.addAll(tileCornersLatLng);
                                                        polygonOptions.strokeColor(Color.RED)
                                                                .fillColor(fillTileColor);

                                                        Polygon polygon = currentColoredPolygonTiles.get(y);

                                                        if (polygon != null) {
                                                            polygon.remove();
                                                        }

                                                        polygon = map.addPolygon(new PolygonOptions()
                                                                .add(tileCornersLatLng.get(0), tileCornersLatLng.get(1), tileCornersLatLng.get(2), tileCornersLatLng.get(3))
                                                                .strokeColor(Color.RED)
                                                                .fillColor(fillTileColor));

                                                        currentColoredPolygonTiles.put(y, polygon);


                                                    }


                                                    tipologiaMappaCorrente = tipologiaMappa.get(item);
                                                    dialog.dismiss();
                                                }

                                                @Override
                                                public void onError(Throwable e) {
                                                    removeColoredTiles();
                                                    dialog.dismiss();
                                                }
                                            }
                                    );

                        }

                        else if(tipologiaMappa.get(item).equals("NOISE")){
                            appDatabase.observationDAO().getAvgNoiseObservationColumn(mgrsToQuery).subscribeOn(Schedulers.computation())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            new SingleObserver<List<Observation>>() {
                                                @Override
                                                public void onSubscribe(Disposable d) {
                                                    fragment = new ProgressBarFragment();
                                                    fragmentManager = getSupportFragmentManager();
                                                    fragmentTransaction = fragmentManager.beginTransaction();
                                                    fragmentTransaction.replace(R.id.fragmentContainer, fragment);
                                                    fragmentTransaction.commit();
                                                }

                                                @Override
                                                public void onSuccess(List<Observation> observations) {
                                                    removeColoredTiles();
                                                    fragmentManager.beginTransaction().remove(fragment).commit();

                                                    currentObservations = observations;

                                                    //usare

                                                    int fillTileColor = Color.argb(0, 0, 0, 0);


                                                    //Color.argb(100,0,255,0) rosso 1 male
                                                    //Color.argb(100,0,255,0) marrone 2 normale
                                                    //Color.argb(100,255,0,0) verde 3 buono


                                                    for (Observation y : currentObservations) {
                                                        MGRS mgrsCurrObs = null;
                                                        try {
                                                            mgrsCurrObs = MGRS.parse(y.getMgrs());
                                                        } catch (ParseException e) {
                                                            e.printStackTrace();
                                                        }
                                                        if (y.AVG == 1) {
                                                            fillTileColor = Color.argb(100, 255, 0, 0);
                                                        } else if (y.AVG == 2) {
                                                            fillTileColor = Color.argb(100, 165, 42, 42);
                                                        } else if (y.AVG == 3) {
                                                            fillTileColor = Color.argb(100, 0, 255, 0);
                                                        }

                                                        PolygonOptions polygonOptions = new PolygonOptions();

                                                        List<LatLng> tileCornersLatLng = findTilesCornerCoordinates(mgrsCurrObs, GridType.valueOf(currentZoomLevel));

                                                        polygonOptions.addAll(tileCornersLatLng);
                                                        polygonOptions.strokeColor(Color.RED)
                                                                .fillColor(fillTileColor);

                                                        Polygon polygon = currentColoredPolygonTiles.get(y);

                                                        if (polygon != null) {
                                                            polygon.remove();
                                                        }

                                                        polygon = map.addPolygon(new PolygonOptions()
                                                                .add(tileCornersLatLng.get(0), tileCornersLatLng.get(1), tileCornersLatLng.get(2), tileCornersLatLng.get(3))
                                                                .strokeColor(Color.RED)
                                                                .fillColor(fillTileColor));

                                                        currentColoredPolygonTiles.put(y, polygon);


                                                    }


                                                    tipologiaMappaCorrente = tipologiaMappa.get(item);
                                                    dialog.dismiss();
                                                }

                                                @Override
                                                public void onError(Throwable e) {
                                                    removeColoredTiles();
                                                    dialog.dismiss();
                                                }
                                            }
                                    );

                        }

                        if(tipologiaMappa.get(item).equals("NESSUNO")){
                            removeColoredTiles();
                            tipologiaMappaCorrente = tipologiaMappa.get(item);
                            dialog.dismiss();


                        }



                    }
            );

        } catch (Exception e) {
            Log.e(MapsActivity.class.getSimpleName(),
                    "Unsupported coordinate ");
        }


        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }


    private void onInputSettingsClick() {

        list = new ArrayList<>();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Selezionare tipo di rilevazione: ");
        builder.setMultiChoiceItems(R.array.inputObservationTypeDialog, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                String[] arr = getResources().getStringArray(R.array.inputObservationTypeDialog);
                if (isChecked) {
                    list.add(arr[which]);
                } else if (list.contains(arr[which])) {
                    list.remove(arr[which]);
                }
            }
        });

        //Registrazione col microfono
        builder.setPositiveButton(R.string.search, (dialog, which) -> dialog.cancel());

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Search and move to the coordinate
     *
     * @param coordinate MGRS, UTM, or WGS84 coordinate
     */
    private void search(String coordinate) {
        searchMGRSResult = null;
        Point point = null;
        Integer zoom = null;

        float currentZoom = map.getCameraPosition().zoom;
        try {
            coordinate = coordinate.trim();
            if (MGRS.isMGRS(coordinate)) {
                MGRS mgrs = MGRS.parse(coordinate);
                GridType gridType = MGRS.precision(coordinate);
                List<LatLng> tileCornersLatLng = findTilesCornerCoordinates(mgrs, mgrs.precision());
                southWest = tileCornersLatLng.get(0);
                northWest = tileCornersLatLng.get(1);
                northEast = tileCornersLatLng.get(2);
                southEast = tileCornersLatLng.get(3);
                if (gridType == GridType.GZD) {
                    point = mgrs.getGridZone().getBounds().getSouthwest();
                } else {
                    point = mgrs.toPoint();
                }
                searchMGRSResult = coordinate.toUpperCase();
                currentZoomLevel = gridType.name();
                zoom = zoomPrecisionLength.get(gridType.name());
            } else if (UTM.isUTM(coordinate)) {
                point = UTM.parse(coordinate).toPoint();
            } else {
                String[] parts = coordinate.split("\\s*,\\s*");
                if (parts.length == 2) {
                    point = Point.point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
                }
            }
        } catch (Exception e) {
            Log.e(MapsActivity.class.getSimpleName(),
                    "Unsupported coordinate: " + coordinate, e);
        }
        if (point != null) {
            LatLng latLng = TileUtils.toLatLng(point);
            if (searchMGRSResult == null) {
                searchMGRSResult = tileProvider.getCoordinate(latLng, (int) currentZoom);
            }
            CameraUpdate update;
            if (zoom != null) {
                update = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            } else {
                update = CameraUpdateFactory.newLatLng(latLng);
            }


            map.animateCamera(update);
        } else {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(R.string.search_error);
            alert.setMessage(coordinate);
            alert.setPositiveButton(R.string.ok, null);
            alert.setCancelable(true);
            alert.create().show();
        }
    }


    /**
     * PERMISSIONS CHECKS
     */


    private void requestRuntimePermission() {
        if (ActivityCompat.checkSelfPermission(this, PERMISSION_RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {

            Toast.makeText(this, "Permission granted. You can use the API which requires the permission", Toast.LENGTH_LONG).show();
            setContentView(R.layout.activity_maps);
            if (!Places.isInitialized()) {
                Places.initialize(this, this.getString(R.string.maps_api_key));
            }
            //enableMyLocation();

            //qua chiama onmap ready
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapView = mapFragment.getView();
                mapFragment.getMapAsync(this);
            }

            mgrsLabel = findViewById(R.id.mgrs);
            wgs84Label = findViewById(R.id.wgs84);
            zoomLabel = findViewById(R.id.zoom);
            zoomFormatter.setRoundingMode(RoundingMode.DOWN);


            ImageButton searchButton = findViewById(R.id.search);
            searchButton.setOnClickListener(v -> onSearchClick());
            ImageButton mapTypeButton = findViewById(R.id.mapType);
            mapTypeButton.setOnClickListener(v -> onMapTypeClick());
            ImageButton settingsButton = findViewById(R.id.observationTypeMapBtn);
            settingsButton.setOnClickListener(v -> onSettingsClick());

            ImageButton zoomLevelBtn = findViewById(R.id.zoomLevelBtn);
            zoomLevelBtn.setOnClickListener(v -> onZoomLevelClick());

            ImageButton inputSettingsBtn = findViewById(R.id.inputSettingsBtn);
            inputSettingsBtn.setOnClickListener(v -> onInputSettingsClick());
            mgrsLabel.setOnLongClickListener(v -> {
                copyToClipboard(getString(R.string.mgrs_label), mgrsLabel.getText());
                return true;
            });
            wgs84Label.setOnLongClickListener(v -> {
                copyToClipboard(getString(R.string.wgs84_label), wgs84Label.getText());
                return true;
            });
            zoomLabel.setOnLongClickListener(v -> {
                copyToClipboard(getString(R.string.zoom_label), zoomLabel.getText());
                return true;
            });
            coordinateFormatter.setRoundingMode(RoundingMode.HALF_UP);

            Grids grids = Grids.create();
            grids.setLabelMinZoom(GridType.GZD, 3);

            tileProvider = MGRSTileProvider.create(this, grids);


        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_RECORD_AUDIO) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_ACCESS_COARSE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_WRITE_EXTERNAL_STORAGE)
        ) {
            setContentView(R.layout.permission_denied);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("This app requires all permissions  to work as expected.")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialogInterface, i) -> {
                        ActivityCompat.requestPermissions(MapsActivity.this, new String[]{PERMISSION_RECORD_AUDIO, PERMISSION_ACCESS_FINE_LOCATION, PERMISSION_ACCESS_COARSE_LOCATION, PERMISSION_WRITE_EXTERNAL_STORAGE}, PERMISSION_REQ_CODE);
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton("Cancel", ((dialogInterface, i) -> {
                                dialogInterface.dismiss();
                            })
                    );

            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{PERMISSION_RECORD_AUDIO, PERMISSION_ACCESS_FINE_LOCATION, PERMISSION_ACCESS_COARSE_LOCATION, PERMISSION_WRITE_EXTERNAL_STORAGE}, PERMISSION_REQ_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. You can use the API which requires the permission ", Toast.LENGTH_SHORT).show();
                setContentView(R.layout.activity_maps);
                if (!Places.isInitialized()) {
                    Places.initialize(this, this.getString(R.string.maps_api_key));
                }
                //enableMyLocation();

                //qua chiama onmap ready
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                if (mapFragment != null) {
                    mapView = mapFragment.getView();
                    mapFragment.getMapAsync(this);
                }

                mgrsLabel = findViewById(R.id.mgrs);
                wgs84Label = findViewById(R.id.wgs84);
                zoomLabel = findViewById(R.id.zoom);
                zoomFormatter.setRoundingMode(RoundingMode.DOWN);


                ImageButton searchButton = findViewById(R.id.search);
                searchButton.setOnClickListener(v -> onSearchClick());
                ImageButton mapTypeButton = findViewById(R.id.mapType);
                mapTypeButton.setOnClickListener(v -> onMapTypeClick());
                ImageButton settingsButton = findViewById(R.id.observationTypeMapBtn);
                settingsButton.setOnClickListener(v -> onSettingsClick());

                ImageButton zoomLevelBtn = findViewById(R.id.zoomLevelBtn);
                zoomLevelBtn.setOnClickListener(v -> onZoomLevelClick());

                ImageButton inputSettingsBtn = findViewById(R.id.inputSettingsBtn);
                inputSettingsBtn.setOnClickListener(v -> onInputSettingsClick());
                mgrsLabel.setOnLongClickListener(v -> {
                    copyToClipboard(getString(R.string.mgrs_label), mgrsLabel.getText());
                    return true;
                });
                wgs84Label.setOnLongClickListener(v -> {
                    copyToClipboard(getString(R.string.wgs84_label), wgs84Label.getText());
                    return true;
                });
                zoomLabel.setOnLongClickListener(v -> {
                    copyToClipboard(getString(R.string.zoom_label), zoomLabel.getText());
                    return true;
                });
                coordinateFormatter.setRoundingMode(RoundingMode.HALF_UP);

                Grids grids = Grids.create();
                grids.setLabelMinZoom(GridType.GZD, 3);

                tileProvider = MGRSTileProvider.create(this, grids);

            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_RECORD_AUDIO) ||
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_ACCESS_FINE_LOCATION) ||
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_ACCESS_COARSE_LOCATION) ||
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_WRITE_EXTERNAL_STORAGE)) {
                setContentView(R.layout.permission_denied);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("This feature is unavailable because this feature requires permission that you have denied. Please allow mic permission from settings to proceed further")
                        .setCancelable(false)
                        .setNegativeButton("Cancel", ((dialogInterface, i) -> {
                            dialogInterface.dismiss();
                            setContentView(R.layout.permission_denied);
                        }))
                        .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                                dialogInterface.dismiss();
                            }
                        });
                builder.show();
            }

        } else {
            requestRuntimePermission();
        }
    }

}
