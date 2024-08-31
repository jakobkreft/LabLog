package si.uni_lj.fe.lablog.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Key.class, Entry.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract KeyDao keyDao();
    public abstract EntryDao entryDao();
}
