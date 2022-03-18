package fi.vr.runscope.utils;

public class AppUtils {

    // KEEP EMPTY IN PROD
    public static String API_KEY = "";

    public static boolean EDIT_IF_EXISTS_OPTION = false;
    public static boolean CREATE_IF_MISSING_OPTION = true;
    public static boolean PRINT_INFO_OPTION = true;
    public static boolean IMPORT_DEFAULT_TEST_ENVIRONMENT_OPTION = false;

    public final static int API_KEY_LENGTH = 36;

    // COMMANDS
    public final static String SET_APIKEY = "setapikey";
    public final static String ACCOUNT = "account";
    public final static String EXPORT = "export";
    public final static String IMPORT = "import";
    public final static String SETTINGS = "settings";
    public final static String HELP = "help";
    public final static String QUIT = "quit";
    public final static String REPLACE_IF_EXISTS = "replaceIfExists";
    public final static String CREATE_IF_MISSING = "createIfMissing";
    public final static String PRINT_INFO = "printInfo";
    public final static String IMPORT_DEFAULT_TEST_ENVIRONMENT = "importDefaultTestEnvironment";
    public final static String TRUE = "true";
    public final static String FALSE = "false";
    public final static String YES = "y";
    public final static String NO = "n";


    // MESSAGES
    public final static String CONTINUE_Y_N =       "CONTINUE? (Y/N)";
    public final static String API_KEY_REQUIRED =   "API KEY IS REQUIRED. SET API KEY WITH FOLLOWING COMMAND: " + AppUtils.SET_APIKEY + " key";
    public final static String INVALID_COMMAND =    "INVALID COMMAND";


    // STRINGS
    public final static String SHARED_ENVIRONMENTS = "shared_environments";
    public final static String LAST_RUN = "last_run";
    public final static String TESTS = "tests";

    public static void print(String text) {
        System.out.println(text);
    }

    public static void printInfo(String text) {
        if (PRINT_INFO_OPTION) {
            System.out.println(text);
        }
    }

    public static void printErr(String text) {
        System.err.println(text);
    }

    public static void printCommands() {
        print("");
        print("COMMANDS:");
        print("");
        print(SET_APIKEY + " key");
        print(" - SET RUNSCOPE API KEY (REQUIRED)");
        print("");
        print(ACCOUNT);
        print(" - GET ACCOUNT DETAILS");
        print("");
        print(EXPORT);
        print(" - EXPORT ALL");
        print("");
        print(EXPORT + " bucketKey1 bucketKey2...");
        print(" - EXPORT SPECIFIED BUCKETS");
        print("");
        print(EXPORT + " 'param'");
        print(" - EXPORT BUCKETS WHICH HAVE 'param' IN NAME");
        print("");
        print(IMPORT);
        print(" - IMPORT ALL");
        print("");
        print(IMPORT + " bucketKey1 bucketKey2...");
        print(" - IMPORT SPECIFIED BUCKETS");
        print("");
        print(IMPORT + " 'param'");
        print(" - IMPORT BUCKETS WHICH HAVE 'param' IN NAME");
        printSettings();
        print(" - CURRENT SETTINGS, CHANGE TRUE/FALSE");
        print("");
        print(QUIT);
        print(" - QUIT");
        print("");
    }

    public static void printSettings() {
        print("");
        print(SETTINGS + " " +
                "" + REPLACE_IF_EXISTS + "=" + EDIT_IF_EXISTS_OPTION + " " +
                "  " + CREATE_IF_MISSING + "=" + CREATE_IF_MISSING_OPTION + " " +
                "  " + PRINT_INFO + "=" + PRINT_INFO_OPTION + " " +
                "  " + IMPORT_DEFAULT_TEST_ENVIRONMENT + "=" + IMPORT_DEFAULT_TEST_ENVIRONMENT_OPTION);
    }
}
