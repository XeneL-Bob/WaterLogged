package au.com.waterlogged.model;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.room.Room;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Serialises writes and delivers results to controllers on the main thread. */
public final class WaterRepository {
    public interface Result<T> {
        void complete(T value, Exception error);
    }

    private static volatile WaterRepository instance;
    private final WaterDatabase database;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final File photoDirectory;

    private WaterRepository(Context context) {
        database =
                Room.databaseBuilder(context, WaterDatabase.class, "water_logged.db")
                        .addMigrations(
                                WaterDatabase.MIGRATION_1_2,
                                WaterDatabase.MIGRATION_2_3,
                                WaterDatabase.MIGRATION_3_4)
                        .build();
        photoDirectory = new File(context.getFilesDir(), "photos");
        io.execute(
                () -> {
                    List<Species> catalogue = SpeciesCatalogue.get(context).species();
                    // INSERT IGNORE adds new fish without resetting anyone's carefully curated
                    // favourites.
                    if (!catalogue.isEmpty()) {
                        database.species().seedCatalogue(catalogue);
                        return;
                    }
                    database.species()
                            .seed(
                                    Arrays.asList(
                                            new Species(
                                                    "flathead",
                                                    "Dusky flathead",
                                                    "Platycephalus fuscus",
                                                    "flathead lizard",
                                                    true),
                                            new Species(
                                                    "snapper",
                                                    "Snapper",
                                                    "Chrysophrys auratus",
                                                    "Pagrus auratus pink snapper squire",
                                                    true),
                                            new Species(
                                                    "bream",
                                                    "Yellowfin bream",
                                                    "Acanthopagrus australis",
                                                    "bream",
                                                    true),
                                            new Species(
                                                    "barramundi",
                                                    "Barramundi",
                                                    "Lates calcarifer",
                                                    "barra",
                                                    false),
                                            new Species(
                                                    "tailor",
                                                    "Tailor",
                                                    "Pomatomus saltatrix",
                                                    "bluefish",
                                                    false),
                                            new Species(
                                                    "trevally",
                                                    "Giant trevally",
                                                    "Caranx ignobilis",
                                                    "trevally GT",
                                                    false)));
                });
    }

    public static WaterRepository get(Context context) {
        if (instance == null)
            synchronized (WaterRepository.class) {
                if (instance == null)
                    instance = new WaterRepository(context.getApplicationContext());
            }
        return instance;
    }

    public LiveData<List<CatchRecord>> catches(boolean smallest, long start, long end) {
        return smallest
                ? database.catches().smallest(start, end)
                : database.catches().newest(start, end);
    }

    public LiveData<List<Species>> species(String term) {
        return database.species().search(term);
    }

    public interface Task<T> {
        T run(WaterDatabase db, File photos) throws Exception;
    }

    /** Backup, restore and ordinary edits share one worker so files and records agree. */
    public <T> void execute(Task<T> task, Result<T> callback) {
        io.execute(
                () -> {
                    try {
                        T result = task.run(database, photoDirectory);
                        main.post(() -> callback.complete(result, null));
                    } catch (Exception error) {
                        main.post(() -> callback.complete(null, error));
                    }
                });
    }

    public LiveData<List<SessionSummary>> sessions() {
        return database.sessions().summaries();
    }

    public LiveData<FishingSession> activeSession() {
        return database.sessions().watchActive();
    }

    public void sessionDefaults(Result<FishingSession> callback) {
        execute((db, p) -> db.sessions().active(), callback);
    }

    public void saveSession(FishingSession session, boolean create, Result<Void> callback) {
        execute(
                (db, p) -> {
                    db.runInTransaction(
                            () -> {
                                if (create) {
                                    if (db.sessions().active() != null)
                                        throw new IllegalStateException(
                                                "A fishing session is already active.");
                                    db.sessions().insert(session);
                                } else db.sessions().update(session);
                            });
                    return null;
                },
                callback);
    }

    public void endSession(String id, Result<Void> callback) {
        execute(
                (db, p) -> {
                    db.sessions().end(id, System.currentTimeMillis());
                    return null;
                },
                callback);
    }

    public void addSpecies(Species species, Result<Void> callback) {
        execute(
                (db, p) -> {
                    for (Species current : db.species().all())
                        if (current.commonName.equalsIgnoreCase(species.commonName)
                                && current.scientificName.equalsIgnoreCase(species.scientificName))
                            throw new IllegalArgumentException(
                                    "That species is already in your catalogue. Search for it"
                                            + " first.");
                    db.species().insert(species);
                    return null;
                },
                callback);
    }

    public LiveData<List<CatchRecord>> searchCatches(
            boolean smallest,
            long start,
            long end,
            String term,
            String status,
            String method,
            String session) {
        return smallest
                ? database.catches().searchSmallest(start, end, term, status, method, session)
                : database.catches().searchNewest(start, end, term, status, method, session);
    }

    public void toggleFavourite(String id) {
        io.execute(() -> database.species().toggleFavourite(id));
    }

    public void findSpecies(String id, Result<Species> callback) {
        io.execute(
                () -> {
                    try {
                        Species s = database.species().find(id);
                        main.post(() -> callback.complete(s, null));
                    } catch (Exception e) {
                        main.post(() -> callback.complete(null, e));
                    }
                });
    }

    public void find(String id, Result<CatchRecord> callback) {
        io.execute(
                () -> {
                    try {
                        CatchRecord r = database.catches().find(id);
                        main.post(() -> callback.complete(r, null));
                    } catch (Exception e) {
                        main.post(() -> callback.complete(null, e));
                    }
                });
    }

    public void save(CatchRecord record, Result<Void> callback) {
        io.execute(
                () -> {
                    try {
                        CatchRecord previous = database.catches().find(record.id);
                        database.catches().save(record);
                        if (previous != null
                                && previous.photoPath != null
                                && !previous.photoPath.equals(record.photoPath))
                            deletePhoto(previous.photoPath);
                        main.post(() -> callback.complete(null, null));
                    } catch (Exception e) {
                        main.post(() -> callback.complete(null, e));
                    }
                });
    }

    public void delete(CatchRecord record, Result<Void> callback) {
        io.execute(
                () -> {
                    try {
                        CatchRecord stored = database.catches().find(record.id);
                        database.catches().delete(record.id);
                        if (stored != null) deletePhoto(stored.photoPath);
                        deletePhoto(record.photoPath);
                        main.post(() -> callback.complete(null, null));
                    } catch (Exception e) {
                        main.post(() -> callback.complete(null, e));
                    }
                });
    }

    public void discardUncommittedPhoto(String id, String path) {
        if (path == null) return;
        io.execute(
                () -> {
                    try {
                        CatchRecord stored = database.catches().find(id);
                        if (stored == null || !path.equals(stored.photoPath)) deletePhoto(path);
                    } catch (Exception ignored) {
                        /* Preserve files when the database cannot be read. */
                    }
                });
    }

    private void deletePhoto(String path) {
        if (path == null) return;
        try {
            File f = new File(path);
            if (photoDirectory.getCanonicalPath().equals(f.getCanonicalFile().getParent()))
                f.delete();
        } catch (Exception ignored) {
            /* A failed file cleanup never loses a catch record. */
        }
    }
}
