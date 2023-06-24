package mil.nga.mgrs.app.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import mil.nga.mgrs.app.entities.Observation;

@Dao
public
interface ObservationDAO {
    /**
     * CREATE
     */

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insert(List<Observation> observations);

    /**
     * RETRIEVE
     * @return
     */
    @Query("SELECT * FROM Observation")
    Single<List<Observation>> getAllObservationList();


    @Query("SELECT * FROM Observation WHERE mgrs LIKE :mgrs")
    Single<List<Observation>> getAllObservationMGRSZoomLevelList(String mgrs);


    /**
     * When you code a parameter, Room uses SQLite binding to replace the parameter (e.g. :channel) with the supplied value. SQLite does not allow such substitution (parameter binding) for component names (table, columns, triggers, views, indexes).
     *
     * As such you would have to build the query with the component name using a rawQuery or you could use the CASE WHEN THEN ELSE END construct
     *
     * https://stackoverflow.com/questions/73118139/android-room-when-set-the-dynamic-column-by-param-like-select-param-from-cant
     *
     * @param field
     * @param mgrsLikeInput
     * @return
     */
    @Query("select cast(avg(lte) as int) as AVG, id, mgrs, lte, wifi, noise, created_at from Observation where mgrs like :mgrsLikeInput  group by mgrs ")
    Single<List<Observation>> getAvgLTEObservationColumn(String mgrsLikeInput);

    @Query("select cast(avg(wifi) as int) as AVG, id, mgrs, lte, wifi, noise, created_at from Observation where mgrs like :mgrsLikeInput  group by mgrs ")
    Single<List<Observation>> getAvgWIFIObservationColumn(String mgrsLikeInput);

    @Query("select cast(avg(noise) as int) as AVG, id, mgrs, lte, wifi, noise, created_at from Observation where mgrs like :mgrsLikeInput  group by mgrs ")
    Single<List<Observation>> getAvgNoiseObservationColumn(String mgrsLikeInput);


/*
    @Query("SELECT * FROM Observation WHERE mgrs LIKE :mgrs and length(mgrs) = 11 ")
    Single<List<Observation>> getAllObservationMGRSHUNDRED_METERList(String mgrs);
*/


    /**
     * UPDATE
     */



    /**
     * DELETE
     */


}
