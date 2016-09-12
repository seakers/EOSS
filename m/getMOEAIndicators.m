function [fHV,ET] = getMOEAIndicators(filename,npts)
%reads the csv values starting from the 2nd column
%filename must include path and extension
%npts is the number of first readings desired
%EI is the epsilon indicator
%GD is the generational distnace
%HV is the hypervolume
%IGD is the inverted generational distance

try
    % data = csvread(filename,0,1);
    fid = fopen(filename,'r');
    fHV = zeros(1,npts);
    ET = zeros(1,npts);
    while(~feof(fid))
        line = strsplit(fgetl(fid),',');
        switch line{1}
            case{'Elapsed Time'}
                tET = readLine(line);
            case{'FastHypervolume'}
                tfHV = readLine(line);
            otherwise
                continue;
        end
    end
    fclose(fid);
    len = length(tfHV);
    %get end of run indicator values
    temp = min([len,npts]);
    fHV(1:temp) = tfHV(1:temp);
    ET(1:temp) = tET(1:temp);
catch ME
    warning(strcat('Problem file: ', filename));
    rethrow(ME);
end
end

function [out] = readLine(line)
out = zeros(length(line)-1,1);
for i=1:length(line)-1
    out(i)=str2double(line{i+1});
end
end