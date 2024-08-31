package si.uni_lj.fe.lablog.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "keys")
public class Key {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String type;
}
