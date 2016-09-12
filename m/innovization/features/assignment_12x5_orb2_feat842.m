function bool =assignment_12x5_orb2_feat842(soln)
bool = false;
if( not( logical( abs( soln(13:24) -[0,0,1,1,0,1,0,0,1,0,0,1]))))
bool = true;
end
