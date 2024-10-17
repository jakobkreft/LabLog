package si.uni_lj.fe.lablog.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface KeyDao {
    @Insert
    void insertKey(Key key);

    @Update
    void updateKey(Key key);

    @Delete
    void deleteKey(Key key);

    @Query("SELECT * FROM keys")
    List<Key> getAllKeys();

    @Query("SELECT * FROM keys WHERE id = :keyId LIMIT 1")
    Key getKeyById(int keyId);

    @Query("SELECT * FROM keys WHERE name = :keyName LIMIT 1")
    Key getKeyByName(String keyName);
}
