package au.com.waterlogged.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "species")
public class Species {
    @PrimaryKey @NonNull public String id;
    @NonNull public String commonName;
    @NonNull public String scientificName;
    @NonNull public String aliases;
    public boolean favourite;

    public Species(
            @NonNull String id,
            @NonNull String commonName,
            @NonNull String scientificName,
            @NonNull String aliases,
            boolean favourite) {
        this.id = id;
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.aliases = aliases;
        this.favourite = favourite;
    }
}
