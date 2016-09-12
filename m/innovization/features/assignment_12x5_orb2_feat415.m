function bool =assignment_12x5_orb2_feat415(soln)
bool = false;
if( not( logical( abs( soln(13:24) -[0,0,0,1,1,0,0,1,1,1,1,0]))))
bool = true;
end
