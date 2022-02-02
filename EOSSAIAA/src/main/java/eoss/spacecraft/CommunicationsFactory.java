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
public class CommunicationsFactory implements BusComponentFactory{
    
    private static final double obdhCoeff = 0.0983;

    @Override
    public BusComponent create(Spacecraft s, Orbit o, double lifetime) {
        return new Communications(s.getPayloadMass() * obdhCoeff, 0.);
    }
    
}
