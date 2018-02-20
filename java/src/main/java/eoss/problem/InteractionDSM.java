///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package eoss.problem;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.concurrent.Future;
//import org.paukov.combinatorics.Factory;
//import org.paukov.combinatorics.Generator;
//import org.paukov.combinatorics.ICombinatoricsVector;
//import rbsa.eoss.ArchitectureEvaluator;
//import rbsa.eoss.NDSM;
//import rbsa.eoss.Nto1pair;
//
///**
// * this class creates the synergy, interference and superfluous DSMs
// *
// * @author nozomihitomi
// */
//public class InteractionDSM {
//
//    /**
//     * This method computes the interaction DSM
//     *
//     * @param dimension the nth order of interactions
//     * @param withSynergies problem to evaluate the architectures with synergy rules
//     * @param noSynergies problem to evaluate the architectures without synergy rules
//     */
//    public void computeNDSM(int dimension, EOSSProblem withSynergies, EOSSProblem noSynergies) {        
//        ICombinatoricsVector<Instrument> originalVector = Factory.createVector(EOSSDatabase.getInstruments());
//        Generator<Instrument> gen = Factory.createSimpleCombinationGenerator(originalVector, dimension - 1);
//
//        for (int i = 0; i < EOSSDatabase.getOrbits().size(); i++) {
//            Orbit orbit = EOSSDatabase.getOrbits().get(i);
//            String key_s = "SDSM" + dimension + "@" + orbit;
//            String key_e = "EDSM" + dimension + "@" + orbit;
//            String key_r = "RDSM" + dimension + "@" + orbit;
//            NDSM sdsm = new NDSM(EOSSDatabase.getInstruments(), key_s);
//            NDSM edsm = new NDSM(EOSSDatabase.getInstruments(), key_e);
//            NDSM rdsm = new NDSM(EOSSDatabase.getInstruments(), key_r);
//            
//            Iterator it = gen.iterator();
//            for (int j = 0; j < gen.getNumberOfGeneratedObjects(); j++) {
//                ICombinatoricsVector<Instrument> combination = (ICombinatoricsVector<Instrument>) it.next();
//                for (int k = 0; k < EOSSDatabase.getInstruments().size(); k++) {
//                    Instrument addedInstrument = EOSSDatabase.getInstruments().get(j);
//                    if (combination.contains(addedInstrument)) {
//                        continue;
//                    }
//                    EOSSArchitecture archWithSynergy = (EOSSArchitecture) withSynergies.newSolution();
//                    for (Instrument instrumentInBase : combination) {
//                        archWithSynergy.addInstrumentToOrbit(EOSSDatabase.findInstrumentIndex(instrumentInBase), i);
//                    }
//                    EOSSArchitecture archNoSynergy = (EOSSArchitecture) archWithSynergy.copy();
//
//                    //compute score with synergies
//                    withSynergies.evaluate(archWithSynergy);
//
//                    //compute score without synergies
//                    noSynergies.evaluate(archNoSynergy);
//
//                    double diff = archWithSynergy.getObjective(0) - archNoSynergy.getObjective(0);
//                    if (diff < 0) {
//                        //if difference is negtive then there is an interference relationship that dominates synergistic ones
//                        edsm.setInteraction(combination, addedInstrument, diff);
//                        System.out.println("interference" + diff + " :: " + archWithSynergy.payloadToString());
//                    } else {
//                        sdsm.setInteraction(combination, addedInstrument, diff);
//                        System.out.println("interference" + diff + " :: " + archWithSynergy.payloadToString());
//                    }
//
//                    Nto1pair nto10 = new Nto1pair(combination, addedInstrument);
//                    ArrayList<Instrument> als = new ArrayList<>();
//                    als.addAll(Arrays.asList(nto10.getBase()));
//                    ArrayList<Instrument> als2 = new ArrayList<>();
//                    als2.addAll(nto10.toArrayList());
//                    ArrayList<Double> subobj_scores0 = getSubobjScores(als, orbit);
//                    ArrayList<Double> subobj_scores1 = getSubobjScores(als2, orbit);
//                    double red = 0.0;
//                    for (int k = 0; k < subobj_scores0.size(); k++) {
//                        red -= (subobj_scores1.get(k) - subobj_scores0.get(k));
//                    }
//                    
//                    rdsm.setInteraction(nto10.getBase(), nto10.getAdded(), red);
//                }
//            }
//        }
//    }
//
//}
