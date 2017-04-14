package mining;


public class DrivingFeaturesParams {
	
    public static double support_threshold = 0.10;
    public static double confidence_threshold = 0.5;
    public static double lift_threshold = 1.0;
    public static String[] instrument_list = {"ACE_ORCA","ACE_POL",	"ACE_LID","CLAR_ERB",
                                                                                            "ACE_CPR","DESD_SAR","DESD_LID","GACM_VIS","GACM_SWIR",
                                                                                            "HYSP_TIR","POSTEPS_IRS","CNES_KaRIN"};
    public static String[] orbit_list = {"LEO-600-polar-NA", "SSO-600-SSO-AM", "SSO-600-SSO-DD", 
                                                                                            "SSO-800-SSO-DD", "SSO-800-SSO-PM"};

    // Maximum number of iterations for adjusting the number of rules
    public static int maxIter = 7;
    // Number of rules required
    public static int minRuleNum = 30;
    public static int maxRuleNum = 500;

    public static boolean tallMatrix = true;

    // Maximum length of features
    public static int maxLength = 2;
    
    public static boolean run_mRMR = true;
    
    public static int max_number_of_features_before_mRMR = 500;
    
    public static int numThreads = 2;
    
}
