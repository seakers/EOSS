/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.operators;

import eoss.problem.EOSSArchitecture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

/**
 * separate3,-1,6,4,2
 *
 * @author nozomihitomi
 */
public class KnowledgeHumanGood extends AbstractEOSSOperator {

    @Override
    protected EOSSArchitecture evolve(EOSSArchitecture child) {
        EOSSArchitecture result = new EOSSArchitecture(child); //creat copy
        int[] featureToAdd = new int[]{10,31,45,48,52};
        int[] featureToRemove = new int[]{1,3,7,8,11,12,13,14,15,16,17,18,19,21,22,23,24,26,27,28,30,32,36,41,42,43,46,47,54,57};
        
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
            int orbIndex = Math.floorDiv((featureList[ind] - 1), 12);
            int instInd = (featureList[ind]-1) % 12;
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
