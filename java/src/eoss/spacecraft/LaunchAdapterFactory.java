/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

import eoss.problem.Orbit;

/**
 *
 * @author nozomihitomi
 */
public class LaunchAdapterFactory implements BusComponentFactory{
    
    /**
     * Computes launch adapter mass as 1% of satellite dry mass
     */
    private static final double adapterMassCoeff = 0.01;
    

    @Override
    public BusComponent create(Spacecraft s, Orbit o, double lifetime) {
        return new LaunchAdapter(s.getDryMass()* adapterMassCoeff, 0.0);
    }
    
}
