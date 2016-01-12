/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eoss.problem;

/**
 *
 * @author nozomihitomi
 */
public class Orbit {
    private final double semimajorAxis;
    private final double inclination;
    private final double RAAN;
    private final double meanAnomaly;
    private final double eccentricity;
    private final double argPeri;
    private final String name;
    private final OrbitType type;

    /**
     * 
     * @param name
     * @param type
     * @param semimajorAxis in km
     * @param inclination in degrees
     * @param RAAN in degrees 
     * @param meanAnomaly in degrees
     * @param eccentricity between [0,1]
     * @param argPeri in degrees
     */
    public Orbit(String name, OrbitType type, double semimajorAxis, double inclination, double RAAN, double meanAnomaly, double eccentricity, double argPeri) {
        this.name = name;
        this.type = type;
        this.semimajorAxis = semimajorAxis;
        this.inclination = inclination;
        this.RAAN = RAAN;
        this.meanAnomaly = meanAnomaly;
        this.eccentricity = eccentricity;
        this.argPeri = argPeri;
    }

    public String getName() {
        return name;
    }

    public double getSemimajorAxis() {
        return semimajorAxis;
    }

    public double getInclination() {
        return inclination;
    }

    public double getRAAN() {
        return RAAN;
    }

    public double getMeanAnomaly() {
        return meanAnomaly;
    }

    public double getEccentricity() {
        return eccentricity;
    }

    public double getArgPeri() {
        return argPeri;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.semimajorAxis) ^ (Double.doubleToLongBits(this.semimajorAxis) >>> 32));
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.inclination) ^ (Double.doubleToLongBits(this.inclination) >>> 32));
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.RAAN) ^ (Double.doubleToLongBits(this.RAAN) >>> 32));
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.eccentricity) ^ (Double.doubleToLongBits(this.eccentricity) >>> 32));
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.argPeri) ^ (Double.doubleToLongBits(this.argPeri) >>> 32));
        return hash;
    }

    /**
     * This checks if all parameters (except mean anomaly) are equal  
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Orbit other = (Orbit) obj;
        if(!this.name.equalsIgnoreCase(other.getName()))
            return false;
        if (Double.doubleToLongBits(this.semimajorAxis) != Double.doubleToLongBits(other.semimajorAxis)) {
            return false;
        }
        if (Double.doubleToLongBits(this.inclination) != Double.doubleToLongBits(other.inclination)) {
            return false;
        }
        if (Double.doubleToLongBits(this.RAAN) != Double.doubleToLongBits(other.RAAN)) {
            return false;
        }
        if (Double.doubleToLongBits(this.eccentricity) != Double.doubleToLongBits(other.eccentricity)) {
            return false;
        }
        if (Double.doubleToLongBits(this.argPeri) != Double.doubleToLongBits(other.argPeri)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
    
    public String toJessSlots() {
        return 
            " (orbit-type " + type + ")"  + 
            " (orbit-altitude# "  + semimajorAxis + ")"  + 
            " (orbit-eccentricity "  + eccentricity + ")"  + 
            " (orbit-RAAN " + RAAN + ")"  + 
            " (orbit-inclination " + inclination + ")"  + 
            " (orbit-string " + this.toString() + ")";

    }

    public enum OrbitType {
        LEO, //low earth orbit
        
        MEO, //mid earth orbit
        
        HEO, //high earth orbit
        
        GEO; //geostationary orbit
    }
}
