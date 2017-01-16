function eoss_java_init()
%imports the jar file so that EOSS class can be accessed

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%Add the java class path for EOSS orekit jar file
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
jarFile1 = ['.',filesep,'EOSS',filesep,'dist',filesep,'EOSS.jar'];
tmp = javaclasspath;
javaclasspathadded1 = false;

%search through current dynamics paths to see if jar file is already in
%dynamic path (could occur if scenario_builder script throws an error
%before the path is removed at the end)
for i=1:length(tmp)
    if ~isempty(strfind(tmp{i},jarFile1))
        javaclasspathadded1 = true;
    end
end

if ~javaclasspathadded1
    javaaddpath(['.',filesep,'EOSS',filesep,'dist',filesep,'EOSS.jar']);
end