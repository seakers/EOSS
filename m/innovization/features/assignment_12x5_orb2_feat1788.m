function bool =assignment_12x5_orb2_feat1788(soln)
bool = false;
if( not( logical( abs( soln(13:24) -[0,1,1,0,1,1,1,1,1,0,1,1]))))
bool = true;
end
