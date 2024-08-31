package si.uni_lj.fe.lablog.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "entries")
public class Entry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String payload;  // JSON string
    public long timestamp;  // For sorting entries by time
}
