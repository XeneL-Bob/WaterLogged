package au.com.waterlogged.model;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import java.util.List;

@Dao
public interface SessionDao {
    @Query("SELECT * FROM sessions WHERE activeSlot = 1 LIMIT 1")
    FishingSession active();

    @Query("SELECT * FROM sessions WHERE activeSlot = 1 LIMIT 1")
    LiveData<FishingSession> watchActive();

    @Query("SELECT * FROM sessions ORDER BY startedAt ASC, id ASC")
    List<FishingSession> all();

    @Query("SELECT * FROM sessions WHERE id = :id")
    FishingSession find(String id);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(FishingSession session);

    @Update
    void update(FishingSession session);

    @Query(
            "UPDATE sessions SET endedAt = MAX(startedAt, :at), activeSlot = NULL WHERE id = :id"
                    + " AND activeSlot = 1")
    void end(String id, long at);

    @Query(
            "SELECT s.*, COUNT(c.id) AS entries, COALESCE(SUM(c.quantity),0) AS totalFish,"
                + " COALESCE(SUM(CASE WHEN c.disposition='Kept' THEN c.quantity ELSE 0 END),0) AS"
                + " kept, COALESCE(SUM(CASE WHEN c.disposition='Released' THEN c.quantity ELSE 0"
                + " END),0) AS released FROM sessions s LEFT JOIN catches c ON c.sessionId=s.id"
                + " GROUP BY s.id ORDER BY s.startedAt DESC, s.id ASC")
    LiveData<List<SessionSummary>> summaries();
}
