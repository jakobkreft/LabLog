package si.uni_lj.fe.lablog.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EntryDao {
    @Insert
    void insertEntry(Entry entry);

    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    List<Entry> getAllEntries();

    @Delete
    void deleteEntry(Entry entry);
}
