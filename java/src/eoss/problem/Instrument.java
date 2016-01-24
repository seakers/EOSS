/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eoss.problem;

import java.util.Objects;
import java.util.Set;
import org.moeaframework.util.TypedProperties;

/**
 * Describes the instrument
 * @author nozomihitomi
 */
public class Instrument {
    
    private final String name;
    private final TypedProperties properties;

    
    /**
     * 
     * @param prop properties of the instrument
     */
    public Instrument(TypedProperties prop) {
        this.properties = prop;
        this.name = properties.getString("Name", "noname");
    }

    public String getName() {
        return name;
    }
    
    public String getProperty(String propertyName){
        return properties.getString(propertyName, "nil");
    }
    
    /**
     * Gets the key entries or the property names as a set.
     * @return 
     */
    public Set getProperties(){
        return properties.getProperties().keySet();
    }
    
    

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.properties);
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
    
}
