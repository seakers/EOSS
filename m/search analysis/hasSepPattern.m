function hasSepPattern(filename)
%this function identifies which solutions have any (OR) of the patterns

[labels,dec1,obj] = loadPopulation(filename);

ind = true(size(dec1,1),1);

% %separate3's
%from 0th stage
% ind = and(indSepPattern({'ACE_LID', 'CLAR_ERB','CNES_KaRIN'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'DESD_SAR','GACM_SWIR'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'DESD_LID','GACM_SWIR'},dec1), ind);
% ind = and(indSepPattern({'ACE_LID', 'CNES_KaRIN','DESD_SAR'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'ACE_LID','HYSP_TIR'},dec1), ind);
% ind = and(indSepPattern({'ACE_ORCA', 'CLAR_ERB','DESD_SAR'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'ACE_LID','GACM_SWIR'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'ACE_LID','DESD_SAR'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'CLAR_ERB','CNES_KaRIN'},dec1), ind);
% ind = and(indSepPattern({'ACE_LID', 'ACE_ORCA','GACM_SWIR'},dec1), ind);

% %from 3rd stage
% ind = and(indSepPattern({'ACE_LID', 'ACE_ORCA','POSTEPS_IRS'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'ACE_LID','GACM_VIS'},dec1), ind);
% ind = and(indSepPattern({'ACE_LID', 'GACM_SWIR','POSTEPS_IRS'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'CLAR_ERB','POSTEPS_IRS'},dec1), ind);
% ind = and(indSepPattern({'CLAR_ERB', 'HYSP_TIR','POSTEPS_IRS'},dec1), ind);
% ind = and(indSepPattern({'ACE_LID', 'CNES_KaRIN','GACM_VIS'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'ACE_POL','GACM_SWIR'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'DESD_SAR','POSTEPS_IRS'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'ACE_LID','DESD_LID'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'ACE_LID','HYSP_TIR'},dec1), ind);


% %separate2's
% ind = and(indSepPattern({'CNES_KaRIN', 'GACM_SWIR'},dec1), ind);
% ind = and(indSepPattern({'DESD_LID', 'DESD_SAR'},dec1), ind);
ind = and(indSepPattern({'ACE_LID', 'ACE_ORCA'},dec1), ind);
% ind = and(indSepPattern({'DESD_SAR', 'GACM_SWIR'},dec1), ind);
% ind = and(indSepPattern({'CLAR_ERB', 'HYSP_TIR'},dec1), ind);
% ind = and(indSepPattern({'GACM_SWIR', 'POSTEPS_IRS'},dec1), ind);
% ind = and(indSepPattern({'ACE_ORCA', 'GACM_SWIR'},dec1), ind);
% ind = and(indSepPattern({'ACE_ORCA', 'ACE_POL'},dec1), ind);
% ind = and(indSepPattern({'ACE_CPR', 'DESD_LID'},dec1), ind);
% ind = and(indSepPattern({'ACE_POL', 'GACM_VIS'},dec1), ind);

figure(1)
scatter(-obj(:,1),obj(:,2)*33495.939796,'b')
hold on
scatter(-obj(labels==1,1),obj(labels==1,2)*33495.939796,'c','filled')
scatter(-obj(~ind,1),obj(~ind,2)*33495.939796,'r')
hold off

xlabel('Scientific Benefit')
ylabel('Lifecycle cost ($FY10M)')
axis([0,0.3,0,25000])
set(gca,'FontSize',16);
legend('All solutions','Top 25% of solutions','Solutions with {I_i,I_j,I_k}')

end



function p = sepDecPattern(inst)
%inst is a cell of instrument names
%p is matrix with possible separate patterns for each orbit

norbs = 5;
p = zeros(norbs,60);
for i=1:length(inst)
    inst_i = inst{i};
    if strcmp(inst_i,'ACE_ORCA')
        d = 1;
    elseif strcmp(inst_i,'ACE_POL')
        d = 2;
    elseif strcmp(inst_i,'ACE_LID')
        d = 3;
    elseif strcmp(inst_i,'CLAR_ERB')
        d = 4;
    elseif strcmp(inst_i,'ACE_CPR')
        d = 5;
    elseif strcmp(inst_i,'DESD_SAR')
        d = 6;
    elseif strcmp(inst_i,'DESD_LID')
        d = 7;
    elseif strcmp(inst_i,'GACM_VIS')
        d = 8;
    elseif strcmp(inst_i,'GACM_SWIR')
        d = 9;
    elseif strcmp(inst_i,'HYSP_TIR')
        d = 10;
    elseif strcmp(inst_i,'POSTEPS_IRS')
        d = 11;
    elseif strcmp(inst_i,'CNES_KaRIN')
        d = 12;
    else
        error('Instrument %s is not recognized',inst_i)
    end
    
    for j=1:norbs
        p(j,norbs*(d-1)+j)=1;
    end
end
end

function bool = indSepPattern(inst, dec1)
%true if the instruments are separated in all of the orbits, else false
bool = true(size(dec1,1),1);
p = sepDecPattern(inst);
ind = any(dec1*p'==length(inst),2);
bool(ind) = false;
end