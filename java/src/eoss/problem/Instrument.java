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

    /**
     * 
     * @param name the name of the instrument
     * @param prop properties of the instrument
     */
    public Instrument(String name, HashMap<String, String> prop) {
        this.name = name;
        this.properties = prop;
    }

    public String getName() {
        return name;
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
        hash = 97 * hash + Objects.hashCode(this.name);
        return hash;
    }

    /**
     * Checks if names are the same
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
        final Instrument other = (Instrument) obj;
        if (!Objects.equals(this.properties, other.properties)) {
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
         * Properties of the instrument
         */
        private HashMap<String, String> prop =  new HashMap();
        
        public Builder(String name){
            this.name = name;
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
            return new Instrument(name, prop);
        }
    }
    
}
