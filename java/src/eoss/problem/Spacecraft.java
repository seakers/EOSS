/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

/**
 *
 * @author nozomihitomi
 */
public class Spacecraft implements Serializable{
    private static final long serialVersionUID = 42723869369021765L;
    
    /**
     * The spacecraft's payload
     */
    private final Collection<Instrument> paylaod;
    
    /**
     * Properties pertaining to this spacecraft
     */
    private final HashMap<String, String> properties;

    public Spacecraft(Collection<Instrument> paylaod) {
        this.paylaod = paylaod;
        this.properties = new HashMap<>();
    }
    
    public void setProperty(String key, String value){
        properties.put(key, value);
    }
    
    public String getProperty(String key){
        return properties.get(key);
    }

    /**
     * Gets the payload hosted by this spacecraft
     * @return the payload hosted by this spacecraft
     */
    public Collection<Instrument> getPaylaod() {
        return paylaod;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.paylaod);
        hash = 67 * hash + Objects.hashCode(this.properties);
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
        final Spacecraft other = (Spacecraft) obj;
        if (!Objects.equals(this.paylaod, other.paylaod)) {
            return false;
        }
        if (!Objects.equals(this.properties, other.properties)) {
            return false;
        }
        return true;
    }
    
    
}
