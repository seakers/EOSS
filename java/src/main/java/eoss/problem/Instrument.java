/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eoss.problem;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

/**
 * Describes the instrument
 * @author nozomihitomi
 */
public class Instrument implements Serializable{
    private static final long serialVersionUID = -5305724470339403997L;
    
    private final String name;
    private final HashMap<String, String> properties;
    
    //the field of view of the instrument
    private double fov;

    /**
     * 
     * @param name the name of the instrument
     * @param fieldOfView the half angle of the conical field of view [deg]
     * @param prop properties of the instrument
     */
    public Instrument(String name, double fieldOfView, HashMap<String, String> prop) {
        this.name = name;
        this.fov = fieldOfView;
        this.properties = prop;
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the the half angle of the conical field of view [deg]
     * @return 
     */
    public double getFieldOfView() {
        return fov;
    }    
    
    public String getProperty(String propertyName){
        return properties.get(propertyName);
    }
    
    /**
     * Gets the key entries or the property names as a set.
     * @return 
     */
    public Set getProperties(){
        return properties.keySet();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.properties);
        hash = 89 * hash + (int) (Double.doubleToLongBits(this.fov) ^ (Double.doubleToLongBits(this.fov) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Instrument other = (Instrument) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.properties, other.properties)) {
            return false;
        }
        if (Double.doubleToLongBits(this.fov) != Double.doubleToLongBits(other.fov)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
    
    /**
     * A builder pattern to set parameters for the instrument
     */
    public static class Builder implements Serializable {
        
        private static final long serialVersionUID = -1856328904365860768L;

        
        /**
         * The name of the instrument
         */
        private final String name;
        
        /**
         * the field of view of the instrument
         */
        private final double fov;

        
        /**
         * Properties of the instrument
         */
        private HashMap<String, String> prop =  new HashMap();
        
        public Builder(String name, double fieldOfView){
            this.name = name;
            this.fov = fieldOfView;
        }
        
        public Builder properties(HashMap<String, String> prop){
            this.prop = prop;
            return this;
        }
        
        /**
         * Builds an instance of a mission with all the specified parameters.
         *
         * @return
         */
        public Instrument build() {
            return new Instrument(name, fov, prop);
        }
    }
    
}
