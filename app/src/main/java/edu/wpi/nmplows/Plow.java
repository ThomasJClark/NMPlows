package edu.wpi.nmplows;

import java.util.ArrayList;
import java.util.List;

public class Plow {

    private static final int MAX_HISTORY = 60;

    public final int id;
    public long time;
    public double speed;
    public String bearing;
    public double latitude;
    public double longitude;
    public final List<AvlRecord> history = new ArrayList<>(MAX_HISTORY);

    public Plow(int id) {
        this.id = id;
    }

    public void addRecord(AvlRecord record) {
        if (history.size() >= MAX_HISTORY) {
            history.remove(0);
        }

        history.add(record);
        latitude = record.latitude;
        longitude = record.longitude;
    }
}
