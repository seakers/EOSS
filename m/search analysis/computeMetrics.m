function [NFE, HV, IGD] = computeMetrics(jarpath, filepath)
%This function computes the metrics based on the solution history stored in
%the datafile. The hypervolume and inverted generational distance are
%computed based on the reference population. Step determines how often to
%compute these metrics

path = strcat(jarpath,filesep,'problems',filesep, 'climateCentric');
refPopPath = strcat(path, filesep, 'result', filesep, 'ASC paper', filesep, 'for Ref Pop', filesep, 'refPop.pop');
maxNFE = 5000;
step = 5;
epsilonDouble = [0.001,10];
h = waitbar(0, 'Processing populations...');
try
    EOSS_init(jarpath);
    origin = cd(filepath);
    files = dir('*all.pop');
    NFE = zeros(maxNFE/step,length(files));
    HV = zeros(maxNFE/step,length(files));
    IGD = zeros(maxNFE/step,length(files));
    
    refPop = org.moeaframework.core.NondominatedPopulation(architecture.io.ResultIO.loadPopulation(refPopPath));
    %initialize problem
    eoss.problem.EOSSDatabase.getInstance();
    eoss.problem.EOSSDatabase.loadBuses(java.io.File(strcat(path,filesep,'config',filesep,'candidateBuses.xml')));
    eoss.problem.EOSSDatabase.loadInstruments(java.io.File(strcat(path,filesep,'xls',filesep,'Instrument Capability Definition.xls')));
    eoss.problem.EOSSDatabase.loadOrbits(java.io.File(strcat(path,filesep,'config',filesep,'candidateOrbits.xml')));
    eoss.problem.EOSSDatabase.loadLaunchVehicles(java.io.File(strcat(path,filesep,'config',filesep,'candidateLaunchVehicles.xml')));
    prob = eoss.problem.assignment.InstrumentAssignment2(path, 5,...
        eoss.problem.evaluation.RequirementMode.FUZZYATTRIBUTE, true);
    refPoint = org.moeaframework.core.Solution([1.1,1.1]);
    fhv = org.moeaframework.core.indicator.jmetal.FastHypervolume(prob, refPop, refPoint);
    igd = org.moeaframework.core.indicator.InvertedGenerationalDistance(prob, refPop);
    
    for i=1:length(files)
        
        population = architecture.io.ResultIO.loadPopulation(files(i).name);
        map = java.util.HashMap;
        iter = population.iterator();
        %sort solutions by NFE
        while(iter.hasNext())
            sltn = iter.next;
            sltnNFE = sltn.getAttribute('NFE');
            if(~map.containsKey(sltnNFE))
                map.put(sltnNFE,java.util.ArrayList);
            end
            map.get(sltnNFE).add(sltn);
        end
        
        nfeList = java.util.ArrayList(map.keySet());
        java.util.Collections.sort(nfeList);
        
        %go over the sorted nfe
        archive = org.moeaframework.core.EpsilonBoxDominanceArchive(epsilonDouble);
        currentNFE = 0;
        k = 1;
        for j=0:step:maxNFE
            while (currentNFE < nfeList.size() && nfeList.get(currentNFE) <= j)
                archive.addAll(map.get(nfeList.get(currentNFE)));
                currentNFE = currentNFE + 1;
            end
            NFE(k,i) = j;
            HV(k,i) = fhv.evaluate(archive);
            IGD(k,i) = igd.evaluate(archive);
            k = k + 1;
        end
        
        waitbar(i/length(files), h);
    end
catch me
    cd(origin)
    clear refPop refPoint iter map population archive fhv igd prob sltn
    EOSS_end(jarpath);
    disp(me.message);
    close(h)
end
close(h)
clear refPop refPoint iter map population archive fhv igd prob sltn
cd(origin)
EOSS_end(jarpath);

end