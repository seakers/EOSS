/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eoss.problem;

import java.util.Objects;

/**
 * Satellite Bus Information
 * @author nozomihitomi
 */
public class Bus {
    
    private final double cost;
    
    private final double lifetime;
    
    private final double size; 
    
    private final String name;

    public Bus(String name, double size, double cost, double lifetime) {
        this.name = name;
        this.size = size;
        this.cost = cost;
        this.lifetime = lifetime;
    }

    public String getName() {
        return name;
    }

    public double getCost() {
        return cost;
    }

    public double getLifetime() {
        return lifetime;
    }

    public double getSize() {
        return size;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.cost) ^ (Double.doubleToLongBits(this.cost) >>> 32));
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.lifetime) ^ (Double.doubleToLongBits(this.lifetime) >>> 32));
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.size) ^ (Double.doubleToLongBits(this.size) >>> 32));
        hash = 53 * hash + Objects.hashCode(this.name);
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
        final Bus other = (Bus) obj;
        if (Double.doubleToLongBits(this.cost) != Double.doubleToLongBits(other.cost)) {
            return false;
        }
        if (Double.doubleToLongBits(this.lifetime) != Double.doubleToLongBits(other.lifetime)) {
            return false;
        }
        if (Double.doubleToLongBits(this.size) != Double.doubleToLongBits(other.size)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }
    
    
    
    
}
