function EOSS_init(path)
%imports the EOSS jar files from the EOSS main directory

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%Add the java class path for the eoss jar file
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
jarFile1 = [path,filesep,'dist',filesep,'EOSS.jar'];
jarFile2 = [path,filesep,'dist',filesep,'lib',filesep,'mopAOS.jar'];
jarFile3 = [path,filesep,'dist',filesep,'lib',filesep,'SystemArchitectureProblem.jar'];
tmp = javaclasspath;
javaclasspathadded1 = false;
javaclasspathadded2 = false;
javaclasspathadded3 = false;

%search through current dynamics paths to see if jar file is already in
%dynamic path
for i=1:length(tmp)
    if ~isempty(strfind(tmp{i},jarFile1))
        javaclasspathadded1 = true;
    end
    if ~isempty(strfind(tmp{i},jarFile2))
        javaclasspathadded2 = true;
    end
    
    if ~isempty(strfind(tmp{i},jarFile3))
        javaclasspathadded3 = true;
    end
end

if ~javaclasspathadded1
    javaaddpath([path,filesep,'dist',filesep,'EOSS.jar']);
end
if ~javaclasspathadded2
    javaaddpath([path,filesep,'dist',filesep,'lib',filesep,'mopAOS.jar']);
end
if ~javaclasspathadded3
    javaaddpath([path,filesep,'dist',filesep,'lib',filesep,'SystemArchitectureProblem.jar']);
end