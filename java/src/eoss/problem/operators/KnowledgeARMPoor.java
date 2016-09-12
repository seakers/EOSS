/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.operators;

import eoss.problem.EOSSArchitecture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

/**
 * separate3,-1,6,4,2
 *
 * @author nozomihitomi
 */
public class KnowledgeARMPoor extends AbstractEOSSOperator {

    @Override
    protected EOSSArchitecture evolve(EOSSArchitecture child) {
        EOSSArchitecture result = new EOSSArchitecture(child); //creat copy
                
        int orbIndex = 0;
        int[] feature = new int[]{0,1,0,0,1,1,1,1,0,1,0,0};
        
        ArrayList<Integer> indList = new ArrayList<>();
        for(int i=0; i <feature.length; i++){
            indList.add(i);
        }
        Collections.shuffle(indList);
        
        int attempts = 0;
        boolean same = true;
        int ind;
        while(attempts<feature.length && same) {
            ind = indList.get(attempts);
            if (feature[ind] == 1) {
                same = result.removeInstrumentFromOrbit(ind, orbIndex);
            } else {
                same = result.addInstrumentToOrbit(ind, orbIndex);
            }
        }
        return result;
    }

    @Override
    public int getArity() {
        return 1;
    }

}
