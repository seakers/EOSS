function bool =assignment_12x5_orb2_feat3558(soln)
bool = false;
if( not( logical( abs( soln(13:24) -[1,1,0,1,1,1,1,0,0,1,0,1]))))
bool = true;
end
