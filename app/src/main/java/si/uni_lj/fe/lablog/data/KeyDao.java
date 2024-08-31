package si.uni_lj.fe.lablog.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface KeyDao {
    @Insert
    void insertKey(Key key);

    @Query("SELECT * FROM keys")
    List<Key> getAllKeys();
}
