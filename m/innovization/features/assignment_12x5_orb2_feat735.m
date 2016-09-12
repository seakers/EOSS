function bool =assignment_12x5_orb2_feat735(soln)
bool = false;
if( not( logical( abs( soln(13:24) -[0,0,1,0,1,1,0,1,1,1,1,0]))))
bool = true;
end
