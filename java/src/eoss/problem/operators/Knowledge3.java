/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.operators;

import eoss.problem.EOSSArchitecture;

/**
 * absent,-1,11,-1,-1
 *
 * @author nozomihitomi
 */
public class Knowledge3 extends AbstractEOSSOperator {

    @Override
    protected EOSSArchitecture evolve(EOSSArchitecture child) {
        EOSSArchitecture result = new EOSSArchitecture(child); //creat copy
        for (int i = 0; i < 5; i++) {
            result.removeInstrumentFromOrbit(11, i);

        }
        return result;
    }

    @Override
    public int getArity() {
        return 1;
    }

}
