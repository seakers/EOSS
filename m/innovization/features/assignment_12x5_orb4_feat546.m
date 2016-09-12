function bool =assignment_12x5_orb4_feat546(soln)
bool = false;
if( not( logical( abs( soln(37:48) -[0,0,1,0,0,0,1,0,0,0,0,1]))))
bool = true;
end
