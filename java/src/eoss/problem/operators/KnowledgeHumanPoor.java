/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.operators;

import eoss.problem.EOSSArchitecture;
import java.util.ArrayList;
import java.util.Collections;

/**
 * separate3,-1,6,4,2
 *
 * @author nozomihitomi
 */
public class KnowledgeHumanPoor extends AbstractEOSSOperator {

    @Override
    protected EOSSArchitecture evolve(EOSSArchitecture child) {
        EOSSArchitecture result = new EOSSArchitecture(child); //creat copy
        int[] featureToAdd = new int[]{10,31,52};
        int[] featureToRemove = new int[]{7,14,16,17,18,21,22,26,27,32,41,42,44,46,47};
        
        ArrayList<Boolean> indList = new ArrayList<>();
        indList.add(true);
        indList.add(false);
        Collections.shuffle(indList);
        int attempts = 0;
        boolean same = true;
        while( attempts < 2 && same){
            if(indList.get(attempts))
                same = evolve(result, featureToAdd, indList.get(attempts));
            else
                same = evolve(result, featureToRemove, indList.get(attempts));
            attempts++;
        }
        
        return result;
    }
    
    private boolean evolve(EOSSArchitecture result, int[] featureList, boolean add){
        int attempts = 0;
        boolean same = true;
        int ind;
        
        ArrayList<Integer> indList = new ArrayList<>();
        for(int i=0; i <featureList.length; i++){
            indList.add(i);
        }
        Collections.shuffle(indList);
        
        
        while(attempts<featureList.length && same){
            ind = indList.get(attempts);
            int orbIndex = Math.floorDiv((featureList[ind]-1), 12);
            int instInd = (featureList[ind] - 1) % 12;
            if(add){
                same = result.addInstrumentToOrbit(instInd, orbIndex);
            }else{
                same = result.removeInstrumentFromOrbit(instInd, orbIndex);
            }
            attempts++;
        }
        
        return !same;
    }

    @Override
    public int getArity() {
        return 1;
    }

}
