package au.com.waterlogged.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CatchDao {
    @Query("SELECT * FROM catches ORDER BY caughtAt ASC, id ASC")
    List<CatchRecord> all();

    @Query(
            "SELECT c.* FROM catches c LEFT JOIN species s ON s.id=c.speciesId WHERE (:start=0 OR"
                + " c.caughtAt>=:start) AND (:end=0 OR c.caughtAt<:end) AND (:status='' OR"
                + " c.disposition=:status) AND (:session='' OR c.sessionId=:session) AND"
                + " instr(lower(c.method),lower(:method))>0 AND"
                + " (instr(lower(c.title),lower(:term))>0 OR"
                + " instr(lower(c.locationName),lower(:term))>0 OR"
                + " instr(lower(c.notes),lower(:term))>0 OR"
                + " instr(lower(COALESCE(s.commonName,'')),lower(:term))>0 OR"
                + " instr(lower(COALESCE(s.scientificName,'')),lower(:term))>0 OR"
                + " instr(lower(COALESCE(s.aliases,'')),lower(:term))>0) ORDER BY c.caughtAt DESC,"
                + " c.id ASC")
    LiveData<List<CatchRecord>> searchNewest(
            long start, long end, String term, String status, String method, String session);

    @Query(
            "SELECT c.* FROM catches c LEFT JOIN species s ON s.id=c.speciesId WHERE (:start=0 OR"
                    + " c.caughtAt>=:start) AND (:end=0 OR c.caughtAt<:end) AND (:status='' OR"
                    + " c.disposition=:status) AND (:session='' OR c.sessionId=:session) AND"
                    + " instr(lower(c.method),lower(:method))>0 AND"
                    + " (instr(lower(c.title),lower(:term))>0 OR"
                    + " instr(lower(c.locationName),lower(:term))>0 OR"
                    + " instr(lower(c.notes),lower(:term))>0 OR"
                    + " instr(lower(COALESCE(s.commonName,'')),lower(:term))>0 OR"
                    + " instr(lower(COALESCE(s.scientificName,'')),lower(:term))>0 OR"
                    + " instr(lower(COALESCE(s.aliases,'')),lower(:term))>0) ORDER BY c.lengthCm IS"
                    + " NULL ASC, c.lengthCm ASC, c.caughtAt DESC, c.id ASC")
    LiveData<List<CatchRecord>> searchSmallest(
            long start, long end, String term, String status, String method, String session);

    @Query(
            "SELECT * FROM catches WHERE (:start = 0 OR (caughtAt >= :start AND caughtAt < :end))"
                    + " ORDER BY caughtAt DESC, id ASC")
    LiveData<List<CatchRecord>> newest(long start, long end);

    // NULL measurements follow measured fish; both sort choices execute inside SQLite.
    @Query(
            "SELECT * FROM catches WHERE (:start = 0 OR (caughtAt >= :start AND caughtAt < :end))"
                    + " ORDER BY lengthCm IS NULL ASC, lengthCm ASC, caughtAt DESC, id ASC")
    LiveData<List<CatchRecord>> smallest(long start, long end);

    @Query("SELECT * FROM catches WHERE id = :id LIMIT 1")
    CatchRecord find(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(CatchRecord record);

    @Query("DELETE FROM catches WHERE id = :id")
    void delete(String id);
}
