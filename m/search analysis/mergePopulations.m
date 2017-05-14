function mergePopulations(jarpath,filepath)
%this function merges all results into one mat file containing the relevant
%information for each solution

try
    EOSS_init(jarpath);
    origin = cd(filepath);
    files = dir('*.pop');
    mergedPopulation = org.moeaframework.core.Population;
    for i=1:length(files)
        if ~strcmp(files(i).name(end-4), 'l');
            continue
        end
        mergedPopulation.addAll(architecture.io.ResultIO.loadPopulation(files(i).name));
    end
    
    iter = mergedPopulation.iterator;
    popSize = mergedPopulation.size;
    objectives = zeros(popSize,2);
    decisions = zeros(popSize,65);
    mass = zeros(popSize,5);
    dutyCycle = zeros(popSize,5);
    packingEfficiency = zeros(popSize,5);
    synergy = zeros(popSize,1);
    constraint = zeros(popSize,1);
    interference = zeros(popSize,1);
    instorb = zeros(popSize, 1);
    nfe = zeros(popSize,1);
    
    i = 1;
    h = waitbar(0, 'Processing solutions...');
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
            dutyCycle(i,count) = str2double(spacecraft.getProperty('duty cycle'));
            packingEfficiency(i,count) = str2double(spacecraft.getProperty('packingEfficiency'));
            count = count + 1;
        end
        
        constraint(i) = solution.getAttribute('constraint');
        synergy(i) = solution.getAttribute('synergyViolationSum');
        interference(i) = solution.getAttribute('interferenceViolationSum');
        instorb(i) = solution.getAttribute('instrumentOrbitAssignmentViolationSum');
        
        nfe(i) = solution.getAttribute('NFE');
        i = i+1;
        waitbar(i/popSize, h);
    end
    close(h)
    
catch me
    fprintf(me.message)
    clear solution mission spacecraft missionNameIterator iter mergedPopulation
    cd(origin)
    EOSS_end(jarpath);
    disp(me.message);
end
cd(origin)
clear solution mission spacecraft missionNameIterator iter mergedPopulation
EOSS_end(jarpath);

save data.mat

