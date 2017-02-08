function EOSS_end(path)
%removes the jar file from javaclasspath assuming the path is the EOSS main directory

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%remove the java class path for the orekit jar file
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
jarFile1 = [path,filesep,'dist',filesep,'EOSS.jar'];
jarFile2 = [path,filesep,'dist',filesep,'lib',filesep,'mopAOS.jar'];
jarFile3 = [path,filesep,'dist',filesep,'lib',filesep,'SystemArchitectureProblem.jar'];
tmp = javaclasspath;
javaclasspathadded1 = false;
javaclasspathadded2 = false;
javaclasspathadded3 = false;
%search through current dynamics paths to see if jar file is in
%dynamic path. Attempt to remove only if it
%already exists in path
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

if javaclasspathadded1
    javarmpath([path,filesep,'dist',filesep,'EOSS.jar']);
end
if javaclasspathadded2
    javarmpath([path,filesep,'dist',filesep,'lib',filesep,'mopAOS.jar']);
end
if javaclasspathadded3
    javarmpath([path,filesep,'dist',filesep,'lib',filesep,'SystemArchitectureProblem.jar']);
end