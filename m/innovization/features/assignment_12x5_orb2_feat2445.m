function bool =assignment_12x5_orb2_feat2445(soln)
bool = false;
if( not( logical( abs( soln(13:24) -[1,0,0,1,1,0,0,0,1,1,0,0]))))
bool = true;
end
