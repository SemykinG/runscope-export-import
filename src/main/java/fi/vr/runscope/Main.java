package fi.vr.runscope;

import fi.vr.runscope.handlers.CustomHandler;
import fi.vr.runscope.handlers.ExportHandler;
import fi.vr.runscope.handlers.ImportHandler;
import fi.vr.runscope.rest.Request;
import fi.vr.runscope.utils.AppUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static boolean canDoCustom = false;

    private final Request requestHandler = new Request();
    private final ExportHandler exportHandler = new ExportHandler();
    private final ImportHandler importHandler = new ImportHandler();

    public static boolean waitingForUserResponseExport = false;
    public static boolean waitingForUserResponseImport = false;


    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("DEV")) {
                canDoCustom = true;
            }
        }

        Main main = new Main();
        main.inputLoop();
    }

    private void inputLoop() {
        Scanner scanner = new Scanner(System.in);
        AppUtils.print("");
        AppUtils.print("--- RUNSCOPE EXPORT & IMPORT TOOL ---");

        String nextLine = "Hello World!";

        while (!(nextLine = scanner.nextLine()).equalsIgnoreCase(AppUtils.QUIT)) {
            configureArguments(nextLine);
        }

        AppUtils.print("--- HAVE A GOOD DAY! ---");
    }

    private void configureArguments(String line) {
        String[] arguments = line.replaceAll("\\s{2,}", " ").trim().split(" ");

        if (arguments.length > 0) {

            if (canDoCustom) {
                if (arguments[0].equalsIgnoreCase("custom")) {
                    CustomHandler customBehaviour = new CustomHandler();
                    customBehaviour.doCustom();
                    return;
                }
            }

            if (waitingForUserResponseExport || waitingForUserResponseImport) {
                if (arguments[0].equalsIgnoreCase(AppUtils.YES)) {
                    if (waitingForUserResponseExport) {
                        exportHandler.handleExport(AppUtils.YES);
                    } else {
                        importHandler.handleImport(AppUtils.YES);
                    }
                    return;
                }

                if (arguments[0].equalsIgnoreCase(AppUtils.NO)) {
                    if (waitingForUserResponseExport) {
                        exportHandler.handleExport(AppUtils.NO);
                    } else {
                        importHandler.handleImport(AppUtils.NO);
                    }
                    return;
                }

                AppUtils.print("");
                AppUtils.print(AppUtils.CONTINUE_Y_N);
                return;
            }

            if (arguments[0].equalsIgnoreCase("")) {
                return;
            }

            if (arguments[0].equalsIgnoreCase(AppUtils.HELP)) {
                AppUtils.printCommands();
                return;
            }

            if (arguments[0].equalsIgnoreCase(AppUtils.SET_APIKEY)) {
                if (arguments.length <= 1) {
                    AppUtils.print("");
                    AppUtils.print(AppUtils.API_KEY_REQUIRED);
                    return;
                }

                if (arguments[1].length() != AppUtils.API_KEY_LENGTH) {
                    AppUtils.print("");
                    AppUtils.print("API KEY IS INVALID. SET API KEY WITH FOLLOWING COMMAND: '" + AppUtils.SET_APIKEY + " <key>'");
                    return;
                }

                AppUtils.API_KEY = arguments[1];
                AppUtils.print("");
                AppUtils.print("API KEY SET");
                return;
            }

            if (arguments[0].equalsIgnoreCase(AppUtils.SETTINGS)) {
                if (arguments.length <= 1) {
                    AppUtils.printSettings();
                    return;
                } else {
                    if (parseBooleanInLine(line)) {
                        return;
                    }
                }
                AppUtils.print(AppUtils.INVALID_COMMAND);
                return;
            }

            if (AppUtils.API_KEY.length() <= 0) {
                AppUtils.print("");
                AppUtils.print(AppUtils.API_KEY_REQUIRED);
                return;
            }

            if (arguments[0].equalsIgnoreCase(AppUtils.ACCOUNT)) {
                requestHandler.getAccountDetails();
                return;
            }

            if (arguments[0].equalsIgnoreCase(AppUtils.EXPORT)) {
                if (arguments.length <= 1) {
                    exportHandler.constructExportData(null, "");
                } else {
                    if (line.contains("'")) {
                        String keywords = parseKeywordsInLine(line);
                        if (keywords != null) {
                            exportHandler.constructExportData(null, keywords);
                            return;
                        }
                        AppUtils.print("");
                        AppUtils.print(AppUtils.INVALID_COMMAND);
                    } else {
                        List<String> buckets = new ArrayList<>(Arrays.asList(arguments).subList(1, arguments.length));
                        exportHandler.constructExportData(buckets, "");
                    }
                }
                return;
            }

            if (arguments[0].equalsIgnoreCase(AppUtils.IMPORT)) {
                if (arguments.length <= 1) {
                    importHandler.constructImportData(null, "");
                } else {
                    if (line.contains("'")) {
                        String keywords = parseKeywordsInLine(line);
                        if (keywords != null) {
                            importHandler.constructImportData(null, keywords);
                            return;
                        }
                        AppUtils.print("");
                        AppUtils.print(AppUtils.INVALID_COMMAND);
                    } else {
                        List<String> buckets = new ArrayList<>(Arrays.asList(arguments).subList(1, arguments.length));
                        importHandler.constructImportData(buckets, "");
                    }
                }
                return;
            }

            AppUtils.print("");
            AppUtils.print(AppUtils.INVALID_COMMAND);
        }
    }

    private String parseKeywordsInLine(String line) {
        if (line.chars().filter(ch -> ch == '\'').count() != 2) {
            return null;
        }
        return line.substring(line.indexOf("'") + 1, line.lastIndexOf("'"));
    }

    private boolean parseBooleanInLine(String line) {
        boolean settingsChanged = false;

        for (String arg : line.split(" ")) {
            if (arg.contains("=")) {
                String[] settingsArgs = arg.split("=");

                if (settingsArgs.length != 2) {
                    return false;
                }

                String option = settingsArgs[1];
                boolean optionBoolean = option.equalsIgnoreCase(AppUtils.FALSE) || option.equalsIgnoreCase(AppUtils.TRUE);

                if (!optionBoolean) {
                    return false;
                }

                if (settingsArgs[0].equalsIgnoreCase(AppUtils.CREATE_IF_MISSING)) {
                    AppUtils.CREATE_IF_MISSING_OPTION = Boolean.parseBoolean(option);
                    settingsChanged = true;
                    continue;
                }

                if (settingsArgs[0].equalsIgnoreCase(AppUtils.REPLACE_IF_EXISTS)) {
                    AppUtils.EDIT_IF_EXISTS_OPTION = Boolean.parseBoolean(option);
                    settingsChanged = true;
                    continue;
                }

                if (settingsArgs[0].equalsIgnoreCase(AppUtils.PRINT_INFO)) {
                    AppUtils.PRINT_INFO_OPTION = Boolean.parseBoolean(option);
                    settingsChanged = true;
                    continue;
                }

                if (settingsArgs[0].equalsIgnoreCase(AppUtils.IMPORT_DEFAULT_TEST_ENVIRONMENT)) {
                    AppUtils.IMPORT_DEFAULT_TEST_ENVIRONMENT_OPTION = Boolean.parseBoolean(option);
                    settingsChanged = true;
                }
            }
        }

        if (settingsChanged) {
            AppUtils.printSettings();
            return true;
        }

        return false;
    }
}
