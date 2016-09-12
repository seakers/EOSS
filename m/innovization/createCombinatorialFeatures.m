function createCombinatorialFeatures(m_instruments, n_orbits)
%this function is primarily for the EOSS instrument to orbit assignment
%problem. It is intended for use on one orbit and tries all combinations of
%assignments in that orbit. For m instruments, the features this will
%create is m^2 features. It will create the m^2 features for all n orbits.
%Each feature function requires a decision vector as input and outputs a
%boolean (true if the solution contains the feature, else false). It is
%assumed that the mxn binary matrix is flattened so that the first m
%elements correspond to the first orbit, the next m orbits correspond tot
%he 2nd orbit, and so on. 

%zeropad
zeroPad = cell(m_instruments);
for i=1:m_instruments
    zeroPad{i} = '0';
end

h = waitbar(0,'Creating feature files');
nfiles = 0;
for n = 1:n_orbits
    for i = 1:2^m_instruments
        filename = sprintf('assignment_%dx%d_orb%d_feat%d',m_instruments,n_orbits,n,i);
        fid = fopen(strcat(filename,'.m'),'wt');
        
        command1 = strcat('function bool =',filename,'(soln)\n',...
                            'bool = false;\n');
        [str,~] = regexp(dec2bin(i-1),'\d','match','split');
        %pad with zeros
        numZeros = m_instruments-length(str);
        featStr = [zeroPad(1:numZeros),str];
        
        feature = strjoin(featStr,',');
        
        %start and stop indices for each orbit
        startInd = m_instruments*(n-1)+1;
        stopInd = m_instruments*n;
        
        command2 = sprintf('if( not( logical( abs( soln(%d:%d) -', startInd, stopInd);
        command3 = strcat('[',feature,']))))\n');
        command4 = 'bool = true;\n';
        command5 = 'end\n';

        fprintf(fid,strcat(command1,command2,command3,command4,command5));
        fclose(fid);
        nfiles = nfiles + 1;
        waitbar(nfiles/(n_orbits*2^m_instruments),h);
    end
end
close(h)


end