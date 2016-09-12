function stats = runInnovization(epsilonGood, epsilonPoor, featurepath, filepath, filename)

load(strcat(filepath,filename))

origin = cd(featurepath);
files = dir('*feat*.m');

features = cell(length(files));

%first run all features on each orbit
h=waitbar(0,'Loading features...');
for i=1:length(files)
    featFunc = strsplit(files(i).name,'.');
    features{i} = str2func(featFunc{1});
    waitbar(i/length(files),h);
end
close(h)
cd(origin)
addpath(featurepath)
stats = runARM(dec,obj, epsilonGood, epsilonPoor, features);

[numSoln,~] = size(dec);

topN = 10;
%top good features according to confidence
[~,ind_Good] = sort(stats.confidence1,2,'descend');
top_good_feat_ind = ind_Good(1:topN);
top_good_feat = stats.support1ind(:,top_good_feat_ind);
combosGood = fullfact(2*ones(1,topN))-1;
combosGood = combosGood(2:end,:);
tmp = top_good_feat*combosGood';
hasGoodCombos = false(numSoln,size(combosGood,1));
for i=1:size(combosGood,1)
    hasGoodCombos(:,i) = tmp(:,i) == sum(combosGood(i,:));
end
stats.comboSupportGood = sum(hasGoodCombos)/numSoln;

%top poor features according to confidence
[~,ind_Poor] = sort(stats.confidence3,2,'descend');
top_poor_feat_ind = ind_Poor(1:topN);
top_poor_feat = stats.support1ind(:,top_poor_feat_ind);
combosPoor = fullfact(2*ones(1,topN))-1;
combosPoor = combosPoor(2:end,:);
tmp = top_poor_feat*combosPoor';
hasPoorCombos = false(numSoln,size(combosPoor,1));
for i=1:size(combosPoor,1)
    hasPoorCombos(:,i) = tmp(:,i) == sum(combosPoor(i,:));
end
stats.comboSupportPoor = sum(hasPoorCombos)/numSoln;

%compute confidence
stats.comboConfidence1 = zeros(1,size(combosGood,1));
stats.comboConfidence2 = zeros(1,size(combosGood,1));
stats.comboConfidence3 = zeros(1,size(combosPoor,1));
stats.comboConfidence4 = zeros(1,size(combosPoor,1));
for i = 1:size(combosGood,1)
    hasFeatAndGood = sum(and(hasGoodCombos(:,i),stats.support2));
    hasFeatAndPoor = sum(and(hasPoorCombos(:,i),stats.support3));
    stats.comboConfidence1(i) = hasFeatAndGood/stats.support2;
    stats.comboConfidence2(i) = hasFeatAndGood/stats.comboSupportGood(i);
    stats.comboConfidence3(i) = hasFeatAndPoor/stats.support3;
    stats.comboConfidence4(i) = hasFeatAndPoor/stats.comboSupportPoor(i);
end

%compute lift
stats.comboLift1 = zeros(1,size(combosGood,1));
stats.comboLift2 = zeros(1,size(combosPoor,1));
for i = 1:size(combosGood,1)
    stats.comboLift1(i) = stats.comboConfidence1(i)/stats.comboSupportGood(i);
    stats.comboLift2(i) = stats.comboConfidence3(i)/stats.comboSupportPoor(i);
end

stats.comboLift1(isnan(stats.lift1)) = -1;
stats.comboLift2(isnan(stats.lift2)) = -1;

[~,ind_Good] = sort(stats.comboConfidence1,2,'descend');
[~,ind_Poor] = sort(stats.comboConfidence3,2,'descend');
goodFeat = top_good_feat_ind(logical(combosGood(ind_Good(1),:)));
poorFeat = top_poor_feat_ind(logical(combosPoor(ind_Poor(1),:)));

save(filename)
rmpath(featurepath)
end