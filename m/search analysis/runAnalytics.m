%runs the post-run analysis
%reads .res file and puts all results data into one mfile

% selectors = {'AdaptivePursuit'};
% selectors = {'RandomSelect'};
% selectors = {'EpsilonMOEA'};
selectors = {''};
% creditDef = {'SI-A_moreCrossNoInter10'};
creditDef = {''};

% path = 'C:\Users\SEAK2\Nozomi\EOSS\problems\climateCentric';
path = '/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric';
nFiles = length(selectors)*length(creditDef);
filesProcessed = 1;
h = waitbar(filesProcessed/nFiles,'Processing files...');
ind = 1;
for j=1:length(selectors)
    for i=1:length(creditDef)
            [fHV,ET] = getAllResults(strcat(path,filesep,'result/AIAA SciTech/innov_4ops_resetCredits'),selectors{j},creditDef{i});
            if(isempty(fHV))
                disp(strcat(selectors{j},creditDef{i}))
            end
            res.fHV = fHV(end,:);
            res.ET = ET(end,:);
            res.allfHV = fHV;
            res.allET = ET;
            save(strcat(selectors{j},'_',creditDef{i},'.mat'),'res');

        filesProcessed = filesProcessed + 1;
        waitbar(filesProcessed/nFiles,h);
        ind = ind + 1;
    end
end
close(h)

