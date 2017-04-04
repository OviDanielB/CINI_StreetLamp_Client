package org.uniroma2.sdcc.Model;

/**
 * Created by ovidiudanielbarba on 16/03/2017.
 */

import java.sql.Timestamp;

/**
 * message sent by every street lamp, including:
 * street lamp info (ID,address,..) along with
 * natural light level and timestamp
 */
public class StreetLampMessage {

    private StreetLamp streetLamp;
    private float naturalLightLevel;
    private float timestamp;

    public StreetLampMessage() {
    }

    public StreetLampMessage(StreetLamp streetLamp, float naturalLightLevel, float timestamp) {
        this.streetLamp = streetLamp;
        this.naturalLightLevel = naturalLightLevel;
        this.timestamp = timestamp;
    }

    public StreetLamp getStreetLamp() {
        return streetLamp;
    }

    public void setStreetLamp(StreetLamp streetLamp) {
        this.streetLamp = streetLamp;
    }

    public float getNaturalLightLevel() {
        return naturalLightLevel;
    }

    public void setNaturalLightLevel(float naturalLightLevel) {
        this.naturalLightLevel = naturalLightLevel;
    }

    public float getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(float timestamp) {
        this.timestamp = timestamp;
    }
}
