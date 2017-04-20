package mining;


public class DrivingFeaturesParams {
	
    public static double support_threshold = 0.10;
    public static double confidence_threshold = 0.5;
    public static double lift_threshold = 1.0;

    // Maximum number of iterations for adjusting the number of rules
    public static int maxIter = 7;
    // Number of rules required
    public static int minRuleNum = 30;
    public static int maxRuleNum = 500;

    public static boolean tallMatrix = true;

    // Maximum length of features
    public static int maxLength = 3;
    
    public static boolean run_mRMR = true;
    
    public static int max_number_of_features_before_mRMR = 1000000;
    
    public static int numThreads = 2;
    
}
