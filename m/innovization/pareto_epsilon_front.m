function [e_pareto, i_pareto] =  pareto_epsilon_front(data, epsilon)
%This function finds the fuzzy Pareto front (i.e. Pareto epsilon front) of
%the given data using epsilon dominance. The data should be an mxn matrix
%with m solutions and n objectives. The function assumes a minimization
%problem in all objectives. The epislon value [0,1] determines the fraction
%of solutions you want in the fuzzy Pareto front The output of the function
%is the set of solutions from the data set that are epsilon-non-dominated
%and their corresponding index in the data set. epsilon = 1 is equivalent
%as the normal Pareto dominance relationship



%normalize data using min and max of each objective
[numSoln,numObj] = size(data);
signs = min(data) < 0;
mins = min(abs(data),[],1);
%in case all values in one objective are negative 
mins = mins.*(-1*signs);
maxs = max(abs(data),[],1);
normdata = (data - repmat(mins,numSoln,1))./repmat(maxs,numSoln,1);

i_nondominated = false(numSoln,1);

for arch_i = 1 : size(data,1)
    count_i = 0;
    for arch_j = 1 : size(data,1)
        if arch_i==arch_j
            continue;
        else
            
            %vector logical compare evaluates to true only if all elements are
            %true
            %         if(normdata(arch_i,:) < epsilon*normdata(arch_j,:))
            %             count_j = count_j + 1;
            %         else
            if(normdata(arch_i,:) > epsilon*normdata(arch_j,:))
                
                count_i = count_i + 1;
            end
        end
    end
    if (count_i == 0)
        i_nondominated(arch_i) = true;
    end
end

ind = 1:numSoln;
i_pareto = ind(i_nondominated)';
% get the pareto frontier points
e_pareto = data(i_pareto,:);