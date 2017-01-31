function mergePopulations(path)

origin = cd(path);

files = dir('*.dat');
mergedPopulation = java.util.HashMap;
for i=1:length(files)
    fis = java.io.FileInputStream(java.io.File(files{i}.name));
    ois = java.io.ObjectInputStream(fis);
    population = ois.readObject;
    mergedPopulation.putall(population);
    fis.close;
    ois.close;
end

iter = mergedPopulation.iterator;
popSize = mergedPopulation.size;
objectives = zeros(popSize,2);
decisions = zeros(popSize,65);
mass = zeros(popSize,1);
dutycycle = zeros(popSize,1);
packingefficiency = zeros(popSize,1);
synergy = zeros(popSize,1);
interference = zeros(popSize,1);
instorb = zerps(popSize, 1);
nfe = zeros(popSize,1);

i = 1;
while(iter.hasNext)
    solution = iter.next;
    for j=0:solution.getNumberOfDecisions
        decisions(i,j+1) = solution.getDecision(j);
    end
    objectives(i,:) = solution.getObjectives();
    
    mass(i) = solution.getAttribute('massViolationSum');
    dutycycle(i) = solution.getAttribute('dcViolationSum');
    packingefficiency(i) = solution.getAttribute('packingEfficiencyViolationSum');
    synergy(i) = solution.getAttribute('synergyViolationSum');
    interference(i) = solution.getAttribute('interferenceViolationSum');
    instorb(i) = solution.getAttribute('instrumentOrbitAssingmentViolationSum');
    
    nfe(i) = char(solution.getAttribute('NFE'));
    i = i+1;
end

cd(origin)

save data.mat

