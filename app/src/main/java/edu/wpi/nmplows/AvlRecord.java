package edu.wpi.nmplows;

public class AvlRecord {

    public double latitude;
    public double longitude;

    public AvlRecord() {
        this(0, 0);
    }

    public AvlRecord(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
