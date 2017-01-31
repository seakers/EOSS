function mergePopulations(jarpath,filepath)


try
    EOSS_init(jarpath);
    origin = cd(filepath);
    files = dir('*_all.pop');
    mergedPopulation = org.moeaframework.core.Population;
    for i=1:length(files)
        mergedPopulation.addAll(architecture.io.ResultIO.loadPopulation(files(i).name));
    end
    
    iter = mergedPopulation.iterator;
    popSize = mergedPopulation.size;
    objectives = zeros(popSize,2);
    decisions = zeros(popSize,65);
    mass = zeros(popSize,5);
    dutycycle = zeros(popSize,5);
    packingefficiency = zeros(popSize,5);
    synergy = zeros(popSize,1);
    interference = zeros(popSize,1);
    instorb = zeros(popSize, 1);
    nfe = zeros(popSize,1);
    
    i = 1;
    while(iter.hasNext)
        solution = iter.next;
        for j=0:solution.getNumberOfVariables-1
            try
                decisions(i,j+1) = solution.getVariable(j).get(0);
            catch
                decisions(i,j+1) = solution.getVariable(j).getValue();
            end
        end
        objectives(i,:) = solution.getObjectives();
        
        missionNameIterator = solution.getMissionNames.iterator;
        count = 1;
        while(missionNameIterator.hasNext)
            mission = solution.getMission(missionNameIterator.next);
            spacecraft = mission.getSpacecraft.keySet.iterator.next;
            mass(i,count) = spacecraft.getWetMass;
            dutycycle(i,count) = str2double(spacecraft.getProperty('duty cycle'));
            packingefficiency(i,count) = str2double(spacecraft.getProperty('packingEfficiency'));
            count = count + 1;
        end
        
        synergy(i) = solution.getAttribute('synergyViolationSum');
        interference(i) = solution.getAttribute('interferenceViolationSum');
        instorb(i) = solution.getAttribute('instrumentOrbitAssingmentViolationSum');
        
        nfe(i) = char(solution.getAttribute('NFE'));
        i = i+1;
    end
    
catch me
    cd(origin)
    EOSS_end(jarpath);
    disp(me.message);
end
cd(origin)
EOSS_end(jarpath);

clear iter missionNameIterator solution mergedPopulation mission spacecraft

save data.mat

