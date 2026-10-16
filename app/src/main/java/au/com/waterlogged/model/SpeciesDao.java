package au.com.waterlogged.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface SpeciesDao {
    @Query("SELECT * FROM species ORDER BY id ASC")
    List<Species> all();

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(Species fish);

    @Query("UPDATE species SET favourite = :value WHERE id = :id")
    void setFavourite(String id, boolean value);

    @Query(
            "SELECT * FROM species WHERE instr(lower(commonName), lower(:term)) > 0 OR"
                    + " instr(lower(scientificName), lower(:term)) > 0 OR instr(lower(aliases),"
                    + " lower(:term)) > 0 ORDER BY favourite DESC, commonName COLLATE NOCASE ASC")
    LiveData<List<Species>> search(String term);

    @Query("UPDATE species SET favourite = NOT favourite WHERE id = :id")
    void toggleFavourite(String id);

    @Query("SELECT COUNT(*) FROM species")
    int count();

    @Query("SELECT * FROM species WHERE id = :id LIMIT 1")
    Species find(String id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void seed(List<Species> species);

    @Query(
            "UPDATE species SET commonName = :name, scientificName = :scientific, aliases ="
                    + " :aliases WHERE id = :id")
    void updateReference(String id, String name, String scientific, String aliases);

    @Transaction
    default void seedCatalogue(List<Species> species) {
        seed(species);
        for (Species fish : species)
            updateReference(fish.id, fish.commonName, fish.scientificName, fish.aliases);
    }
}
