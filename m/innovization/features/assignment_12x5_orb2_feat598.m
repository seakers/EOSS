function bool =assignment_12x5_orb2_feat598(soln)
bool = false;
if( not( logical( abs( soln(13:24) -[0,0,1,0,0,1,0,1,0,1,0,1]))))
bool = true;
end
