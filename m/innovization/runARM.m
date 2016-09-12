function [stats] = runARM(decData,objData, epsilonGood, epsilonPoor, features)
%This function runs association rule mining using the objective and
%decision values of solutions in a data set. decData is the data set
%containing the decision values and objData is the data set containing the
%corresponding objective values. 
%This implementation uses epsilon dominance to determine a good and poor
%region. Epsilon values should be [0,1] where epsilon = 1 is equivalent to 
%the normal Pareto dominance relationship (e.g. epsilonGood = 0.8, 
%epsilonPoor = 0.3) The epsilon value is a parameter set by the user.
%The features are the features that will be used to compute the support,
%confidence, and lift values. The features should be input as cell 
%collection of functions that return true if the solution contains the
%feature and false otherwise.
%
%OUTPUT: The output contains a structure in the variable stats that has 9
%fields
%1) Support1: the support of the feature (in vector form. each element
%corresponds to a feature)
%2) Support2: the support of the good region
%3) Support3: the support of the poor region
%4) Confidence1: chance of the feature given the solution is in the good
%region
%5) Confidence2: chance of the good region given the solution is has the
%feature
%6) Confidence3: chance of the feature given the solution is in the poor
%region
%7) Confidence4: chance of the poor region given the solution is has the
%feature
%8) Lift1: lift of the feature and in good region
%9) Lift2: lift of the feature and in poor region

numSoln = size(decData,1);
numFeat = length(features);

%flag solns with features
hasFeat = false(numSoln,numFeat);
for i = 1:numSoln
    for j = 1:numFeat
        feat = features{j};
        if feat(decData(i,:))
            hasFeat(i,j) = true;
        end
    end
end

%flag solns in good and poor regions
solnGood = false(numSoln,1);
solnPoor = true(numSoln,1);
fprintf('Computing good Pareto epislon front....')
[~, indGood] =  pareto_epsilon_front(objData, epsilonGood);
fprintf('Done\n')
fprintf('Computing poor Pareto epislon front....')
[~, indPoor] =  pareto_epsilon_front(objData, epsilonPoor);
fprintf('Done\n')
solnGood(indGood) = true;
solnPoor(indPoor) = false;

%compute supports
stats.support1 = sum(hasFeat,1)/numSoln;
stats.support1ind = hasFeat;
stats.support2 = sum(solnGood)/numSoln;
stats.support2ind = solnGood;
stats.support3 = sum(solnPoor)/numSoln;
stats.support3ind = solnPoor;

%compute confidence
stats.confidence1 = zeros(1,numFeat);
stats.confidence2 = zeros(1,numFeat);
stats.confidence3 = zeros(1,numFeat);
stats.confidence4 = zeros(1,numFeat);
h = waitbar(0,'Scanning features');
for i = 1:numFeat
    hasFeatAndGood = sum(and(hasFeat(:,i),solnGood));
    hasFeatAndPoor = sum(and(hasFeat(:,i),solnPoor));
    stats.confidence1(i) = hasFeatAndGood/stats.support2;
    stats.confidence2(i) = hasFeatAndGood/stats.support1(i);
    stats.confidence3(i) = hasFeatAndPoor/stats.support3;
    stats.confidence4(i) = hasFeatAndPoor/stats.support1(i);
    waitbar(i/numFeat,h)
end
close(h)
scatt
%compute lift
stats.lift1 = zeros(1,numFeat);
stats.lift2 = zeros(1,numFeat);
for i = 1:numFeat
    stats.lift1(i) = stats.confidence1(i)/stats.support1(i);
    stats.lift2(i) = stats.confidence3(i)/stats.support1(i);
end

stats.lift1(isnan(stats.lift1)) = -1;
stats.lift2(isnan(stats.lift2)) = -1;

end