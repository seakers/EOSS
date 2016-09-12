/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.operators;

import eoss.problem.EOSSArchitecture;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * separate3,-1,10,7,0
 *
 * @author nozomihitomi
 */
public class Knowledge4 extends AbstractEOSSOperator {

    @Override
    protected EOSSArchitecture evolve(EOSSArchitecture child) {
        EOSSArchitecture result = new EOSSArchitecture(child); //creat copy
        Random rand = new Random();
        ArrayList<Integer> instRid = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ArrayList<Integer> inst = result.getInstrumentsInOrbit(i);
            Iterator<Integer> iter = inst.iterator();
            while (iter.hasNext()) {
                int num = iter.next();
                if (num == 10 || num == 7 || num == 0) {
                    instRid.add(num);
                }
                while (instRid.size() > 1) {
                    int randNum = rand.nextInt(instRid.size());
                    result.removeInstrumentFromOrbit(instRid.get(randNum), i);
                    instRid.remove(randNum);
                }
            }
        }
        return result;
    }

    @Override
    public int getArity() {
        return 1;
    }

}
