function createReferencePopulation(jarpath, filepath, saveFileName)
%this function creates a reference nondominated population from the
%population files resulting from the search.

try
    EOSS_init(jarpath);
    origin = cd(filepath);
    files = dir('*.pop');
    h = waitbar(0, 'Processing populations...');
    refPop = org.moeaframework.core.NondominatedPopulation;
    for i=1:length(files)
        refPop.addAll(architecture.io.ResultIO.loadPopulation(files(i).name));
        waitbar(i/length(files), h);
    end
    
    architecture.io.ResultIO.savePopulation(refPop, saveFileName)
    
catch me
    close(h)
    clear refPop
    fprintf(me.message)
    cd(origin)
    EOSS_end(jarpath);
    disp(me.message);
end
close(h)
clear refPop
cd(origin)
EOSS_end(jarpath);

end