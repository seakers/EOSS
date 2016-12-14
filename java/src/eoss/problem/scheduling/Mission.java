/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.scheduling;

import java.util.Arrays;

/**
 *
 * @author nozomihitomi
 */
public class Mission {
    
    /**
     * The cost profile of the mission. $/year over a number of years
     */
    private final double[] profile;
    
    /**
     * Lifetime of the missions in years
     */
    private final double lifetime;

    public Mission(int devYears, double costPerYear, double lifetime) {
        this.profile = new double[devYears];
        Arrays.fill(this.profile, costPerYear);
        this.lifetime = lifetime;
    }

    /**
     * Gets the cost profile of the mission over a number of years ($/yr)
     * @return the cost profile of the mission over a number of years ($/yr)
     */
    public double[] getCostProfile() {
        return Arrays.copyOf(profile, profile.length);
    }

    /**
     * Gets the lifetime of this mission in years
     * @return the lifetime of this mission in years
     */
    public double getLifetime() {
        return lifetime;
    }
    
    
}
