%runs the post-run analysis
%reads .res file and puts all results data into one mfile

selectors = {'*AdaptivePursuit'};
% selectors = {'*Random'};
% selectors = {'EpsilonMOEA'};
creditDef = {'SI-A_moreCrossNoInter10'};
% creditDef = {'SI-A_moreCrossNoInter10noSingle'};
% creditDef = {''};

path ='/Users/nozomihitomi/Dropbox/EOSS';
% path = 'C:\Users\SEAK2\Nozomi\MOHEA\';
% path = 'C:\Users\SEAK1\Dropbox\MOHEA';
nFiles = length(selectors)*length(creditDef);
for j=1:length(selectors)
    for i=1:length(creditDef)
        [fHV,ET] = getAllResults(strcat(path,filesep,'problems',filesep,'climateCentric',filesep,'result'),selectors{j},creditDef{i});
        if(isempty(ET))
            disp(strcat(selectors{j},creditDef{i}))
        end
        res.fHV = squeeze(fHV);
        res.ET = squeeze(ET);
        save(strcat(selectors{j},'_',creditDef{i},'.mat'),'res');
    end
end
