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
public interface BusComponentFactory {

    /**
     * Sizes and sets bus component for the given spacecraft
     *
     * @param s the spacecraft
     * @param o the orbit the spacecraft will fly in
     * @param lifetime the lifetime of the spacecraft
     * @return a new instance of a bus component sized to the spacecraft s
     * flying in orbit o with the given lifetime
     */
    public BusComponent create(Spacecraft s, Orbit o, double lifetime);
}
