package fi.vr.runscope.handlers;

import fi.vr.runscope.Main;
import fi.vr.runscope.parser.CustomParser;
import fi.vr.runscope.rest.Request;
import fi.vr.runscope.rest.Response;
import fi.vr.runscope.utils.AppUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ImportHandler {

    private final String ID = "id";
    private final String KEY = "key";
    private final String NAME = "name";
    private final String STEPS = "steps";
    private final String TESTS = "tests";
    private final String SCHEDULES = "schedules";
    private final String ENVIRONMENTS = "environments";
    private final String SHARED_ENVIRONMENTS = "shared_environments";
    private final String DEFAULT_ENVIRONMENT_ID = "default_environment_id";

    private final Request requestHandler = new Request();
    private final CustomParser parser = new CustomParser();
    private final FileHandler fileHandler = new FileHandler();

    private List<JSONObject> bucketsToImport = new ArrayList<>();


    public void constructImportData(List<String> bucketKeys, String keyword) {
        JSONArray bucketsJSONArray = fileHandler.FilePathToJSONArray(null);

        // JSON FILE / DIRECTORY ERROR
        if (bucketsJSONArray == null) {
            return;
        }

        bucketsToImport.clear();
        bucketsToImport = parser.constructData(bucketKeys, keyword, bucketsJSONArray, "IMPORTED");

        if (bucketsToImport.size() <= 0) {
            AppUtils.print("");
            AppUtils.print("COULD NOT FIND ANY BUCKETS BY CRITERIA");
            return;
        }

        Main.waitingForUserResponseImport = true;
    }

    @SuppressWarnings("unchecked")
    public void handleImport(String userResponse) {
        if (Main.waitingForUserResponseImport) {
            if (userResponse.equalsIgnoreCase(AppUtils.YES)) {
                AppUtils.print("");
                AppUtils.print("IMPORTING DATA, PLEASE WAIT...");

                AppUtils.printInfo("");
                AppUtils.printInfo("-- CONFIGURING [" + bucketsToImport.size() + "] BUCKET(S)");

                for (int i = 0; i < bucketsToImport.size(); i++) {
                    JSONObject bucketJNObject = bucketsToImport.get(i);
                    String oldBucketKey = (String) bucketJNObject.get(KEY);

                    AppUtils.printInfo("");
                    AppUtils.printInfo("-- BUCKET [" + (i + 1) + "/" + bucketsToImport.size() + "], " + bucketJNObject.get(NAME));

                    Response bucketResponse = requestHandler.handleBucketExistence(bucketJNObject);
                    if (bucketResponse == null) {
                        return;
                    }

                    JSONObject bucketResponseJSONObject = parser.parseStringToJSONObject(parser.getResponseDataString(bucketResponse.getResponse()));
                    String bucketKey = (String) bucketResponseJSONObject.get(KEY);
                    if (bucketKey == null) {
                        AppUtils.printErr("COULD NOT GET BUCKET KEY FROM JSON");
                        return;
                    }

                    JSONArray environmentIdsArray = new JSONArray();
                    JSONArray testIdsArray = new JSONArray();

                    JSONArray sharedEnvironmentsJSONArray = (JSONArray) bucketJNObject.get(SHARED_ENVIRONMENTS);

                    /*  SHARED ENVIRONMENTS  */
                    if (sharedEnvironmentsJSONArray.size() > 0) {
                        AppUtils.printInfo("");
                        AppUtils.printInfo("-- CONFIGURING [" + sharedEnvironmentsJSONArray.size() + "] SHARED ENVIRONMENT(S)");

                        for (int j = 0; j < sharedEnvironmentsJSONArray.size(); j++) {
                            JSONObject sharedEnvironmentJSONObject = (JSONObject) sharedEnvironmentsJSONArray.get(j);

                            AppUtils.printInfo("");
                            AppUtils.printInfo("-- SHARED ENVIRONMENT [" + (j + 1) + "/" + sharedEnvironmentsJSONArray.size() + "], " + sharedEnvironmentJSONObject.get(NAME));

                            Response sharedEnvironmentResponse = requestHandler.handleSharedEnvironmentExistence(bucketKey, sharedEnvironmentJSONObject);
                            if (sharedEnvironmentResponse == null) {
                                return;
                            }

                            JSONObject sharedEnvironmentResponseJSONObject = parser.parseStringToJSONObject(parser.getResponseDataString(sharedEnvironmentResponse.getResponse()));

                            //                                      OLD                                                 NEW
                            addIdsObjectToArray((String) sharedEnvironmentJSONObject.get(ID), (String) sharedEnvironmentResponseJSONObject.get(ID), environmentIdsArray);
                        }
                        AppUtils.printInfo("-- SHARED ENVIRONMENTS CONFIGURED");
                    }


                    JSONArray testsJSONArray = (JSONArray) bucketJNObject.get(TESTS);

                    /*  TESTS  */
                    if (testsJSONArray.size() > 0) {

                        // REMOVE DEFAULT TEST ENVIRONMENT 'Test Settings' BECAUSE NEW ONE IS ALWAYS CREATED BY RUNSCOPE WHEN TEST IS BEING CREATED
                        if (!AppUtils.IMPORT_DEFAULT_TEST_ENVIRONMENT_OPTION) {
                            resolveDefaultTestEnvironments(testsJSONArray);
                        }

                        // LOOP THROUGH testsJSONArray AND PUT ALL TESTS THAT HAVE SUBTEST IN STEPS TO THE END
                        testsJSONArray = resolveTestOrderBySubTestsAsLast(testsJSONArray);

                        String testsStringArray = testsJSONArray.toString();

                        // REPLACE OLD BUCKET KEY WITH NEW TO PRESERVE CONNECTIONS
                        if (!oldBucketKey.equals(bucketKey)) {
                            testsStringArray = testsStringArray.replaceAll(oldBucketKey, bucketKey);
                        }
                        testsJSONArray = parser.parseStringToJSONArray(testsStringArray);

                        AppUtils.printInfo("");
                        AppUtils.printInfo("-- CONFIGURING [" + testsJSONArray.size() + "] TEST(S)");

                        for (int j = 0; j < testsJSONArray.size(); j++) {
                            JSONObject testJSONObject = (JSONObject) testsJSONArray.get(j);

                            AppUtils.printInfo("");
                            AppUtils.printInfo("---- TEST [" + (j + 1) + "/" + testsJSONArray.size() + "], " + testJSONObject.get(NAME));


                            // COLLECT ENVIRONMENTS, STEPS & SCHEDULES AND REMOVE THEM FROM TEST OBJECT
                            JSONArray environmentsJSONArray = (JSONArray) testJSONObject.get(ENVIRONMENTS);
                            testJSONObject.put(ENVIRONMENTS, new JSONArray());
                            JSONArray stepsJSONArray = (JSONArray) testJSONObject.get(STEPS);
                            testJSONObject.put(STEPS, new JSONArray());
                            JSONArray schedulesJSONArray = (JSONArray) testJSONObject.get(SCHEDULES);
                            testJSONObject.put(SCHEDULES, new JSONArray());


                            Response testResponse = requestHandler.handleTestExistence(bucketKey, testJSONObject, true);
                            if (testResponse == null) {
                                return;
                            }

                            // IF TEST IS CREATED, IT GETS NEW DEFAULT ENVIRONMENT, SO WE SET IT TO OLD ONE AND REPLACE TO NEW (CORRECT) ONE LATER.
                            JSONObject testResponseJSONObject = parser.parseStringToJSONObject(parser.getResponseDataString(testResponse.getResponse()));
                            testResponseJSONObject.put(DEFAULT_ENVIRONMENT_ID, testJSONObject.get(DEFAULT_ENVIRONMENT_ID));

                            String newTestId = (String) testResponseJSONObject.get(ID);

                            //                              old test_id
                            addIdsObjectToArray((String) testJSONObject.get(ID), newTestId, testIdsArray);

                            // REPLACE OLD TEST IDs WITH NEW IN STEPS (FOR SUBTESTS) TO PRESERVE CONNECTIONS
                            String stepsStringArray = stepsJSONArray.toString();
                            stepsStringArray = replaceOldIdsWithNewInString(testIdsArray, stepsStringArray);
                            stepsJSONArray = parser.parseStringToJSONArray(stepsStringArray);


                            /*  TEST ENVIRONMENTS  */
                            if (environmentsJSONArray.size() > 0) {
                                AppUtils.printInfo("");
                                AppUtils.printInfo("------ CONFIGURING TEST ENVIRONMENTS");

                                for (Object environmentObject : environmentsJSONArray) {
                                    JSONObject environmentJSONObject = (JSONObject) environmentObject;

                                    Response environmentResponse = requestHandler.handleTestEnvironmentExistence(bucketKey, newTestId, environmentJSONObject);
                                    if (environmentResponse == null) {
                                        return;
                                    }

                                    JSONObject environmentResponseJSONObject = parser.parseStringToJSONObject(parser.getResponseDataString(environmentResponse.getResponse()));

                                    //                                  OLD                                         NEW
                                    addIdsObjectToArray((String) environmentJSONObject.get(ID), (String) environmentResponseJSONObject.get(ID), environmentIdsArray);
                                }
                                AppUtils.printInfo("------ TEST ENVIRONMENTS CONFIGURED");
                            }

                            String currentTestString = testResponseJSONObject.toString();

                            // RESOLVE TEST'S DEFAULT ENVIRONMENT
                            currentTestString = replaceOldIdsWithNewInString(environmentIdsArray, currentTestString);

                            JSONObject currentTestJSONObject = parser.parseStringToJSONObject(currentTestString);
                            currentTestJSONObject.put(STEPS, stepsJSONArray);


                            // UPDATE CURRENT TEST TO HAVE CORRECT DEFAULT ENVIRONMENT AND STEPS
                            Response currentTestResponse = requestHandler.handleTestExistence(bucketKey, currentTestJSONObject, false);
                            if (currentTestResponse == null) {
                                return;
                            }

                            if (schedulesJSONArray.size() > 0) {
                                AppUtils.printInfo("");
                                AppUtils.printInfo("------ CONFIGURING SCHEDULES");

                                // GET ALL SCHEDULES FROM BUCKET FROM TEST
                                Response schedulesResponse = requestHandler.getAllSchedulesInBucketInTest(bucketKey, newTestId);
                                if (schedulesResponse == null) {
                                    AppUtils.printErr("FAILED TO RETRIEVE SCHEDULES IN BUCKET: '" + bucketKey + "', IN TEST: '" + newTestId + "'");
                                    return;
                                }

                                if (schedulesResponse.isRequestFailed()) {
                                    AppUtils.printErr("SCHEDULES GET REQUEST FAILED BUCKET: '" + bucketKey + "', TEST: '" + newTestId + "'");
                                    return;
                                }

                                JSONArray schedulesInRunscopeResponseJSONArray = parser.parseStringToJSONArray(parser.getResponseDataString(schedulesResponse.getResponse()));

                                for (Object scheduleObject : schedulesJSONArray) {
                                    JSONObject scheduleJSONObject = (JSONObject) scheduleObject;

                                    String currentScheduleString = scheduleJSONObject.toString();

                                    // RESOLVE SCHEDULE ENVIRONMENT
                                    currentScheduleString = replaceOldIdsWithNewInString(environmentIdsArray, currentScheduleString);

                                    scheduleJSONObject = parser.parseStringToJSONObject(currentScheduleString);

                                    String scheduleId = (String) scheduleJSONObject.get(ID);

                                    // HANDLE INVALID INTERVAL (30.0m => 30m)
                                    String interval = ((String) scheduleJSONObject.get("interval"));
                                    interval = interval.replaceAll(".0", "");
                                    scheduleJSONObject.put("interval", interval);

                                    boolean scheduleFound = false;

                                    // COMPARE SCHEDULE IDs WITH SCHEDULE IDs FROM RUNSCOPE
                                    for (Object scheduleInRunscopeObject : schedulesInRunscopeResponseJSONArray) {
                                        JSONObject scheduleInRunscopeJSONObject = (JSONObject) scheduleInRunscopeObject;
                                        String scheduleInRunscopeId = (String) scheduleInRunscopeJSONObject.get(ID);

                                        if (scheduleInRunscopeId.equals(scheduleId)) {
                                            scheduleFound = true;
                                            break;
                                        }
                                    }

                                    // IF SCHEDULE ID IS NOT FOUND => SCHEDULE WAS DELETED IN RUNSCOPE => CREATE IT
                                    if (!scheduleFound) {
                                        scheduleId = "";
                                        scheduleJSONObject.put(ID, "");
                                    }

                                    if (!requestHandler.handleScheduleExistence(bucketKey, newTestId, scheduleId, scheduleJSONObject)) {
                                        AppUtils.printErr("FAILED TO CREATE / UPDATE SCHEDULE WITH ID: '" + scheduleId + "', IN BUCKET: '" + bucketKey + "', IN TEST: '" + newTestId + "'");
                                        AppUtils.printErr("SCHEDULE JSON: '" + scheduleJSONObject + "'");
                                        return;
                                    }
                                }
                                AppUtils.printInfo("------ SCHEDULES CONFIGURED");
                            }
                            AppUtils.printInfo("---- TEST CONFIGURED");
                        }
                        AppUtils.printInfo("-- TESTS CONFIGURED");
                    }
                    AppUtils.printInfo("BUCKET CONFIGURED");
                }
                AppUtils.print("");
                AppUtils.print("ALL DATA IMPORTED SUCCESSFULLY");
            } else {
                AppUtils.print("");
                AppUtils.print("IMPORT CANCELLED");
            }
            Main.waitingForUserResponseImport = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void addIdsObjectToArray(String oldId, String newId, JSONArray idsArray) {
        JSONObject idObject = new JSONObject();
        idObject.put("OLD_ID", oldId);
        idObject.put("NEW_ID", newId);

        idsArray.add(idObject);
    }

    private String replaceOldIdsWithNewInString(JSONArray idsArrays, String data) {
        for (Object idObject : idsArrays) {
            JSONObject idJSONObject = (JSONObject) idObject;
            String oldId = (String) idJSONObject.get("OLD_ID");
            String newId = (String) idJSONObject.get("NEW_ID");

            if (!oldId.equals(newId)) {
                data = data.replaceAll(oldId, newId);
            }
        }

        return data;
    }

    @SuppressWarnings("unchecked")
    private void resolveDefaultTestEnvironments(JSONArray testsJSONArray) {
        for (Object testObject : testsJSONArray) {
            JSONObject testJSONObject = (JSONObject) testObject;
            JSONArray testEnvironmentsJSONArray = (JSONArray) testJSONObject.get("environments");

            if (testEnvironmentsJSONArray != null) {
                Iterator<JSONObject> testEnvironmentIterator = testEnvironmentsJSONArray.iterator();

                while (testEnvironmentIterator.hasNext()) {
                    JSONObject testEnvironment = testEnvironmentIterator.next();

                    JSONObject headers = (JSONObject) testEnvironment.get("headers");
                    if (headers != null && !headers.isEmpty()) {
                        continue;
                    }

                    JSONObject initial_script_hash = (JSONObject) testEnvironment.get("initial_script_hash");
                    if (initial_script_hash != null) {
                        continue;
                    }

                    JSONObject initial_variables = (JSONObject) testEnvironment.get("initial_variables");
                    if (initial_variables != null && !initial_variables.isEmpty()) {
                        continue;
                    }

                    String script = (String) testEnvironment.get("script");
                    if (script != null && !script.isEmpty()) {
                        continue;
                    }

                    JSONObject webhooks = (JSONObject) testEnvironment.get("webhooks");
                    if (webhooks != null) {
                        continue;
                    }

                    JSONArray remote_agents = (JSONArray) testEnvironment.get("remote_agents");
                    if (remote_agents != null && !remote_agents.isEmpty()) {
                        continue;
                    }

                    JSONArray script_library = (JSONArray) testEnvironment.get("script_library");
                    if (script_library != null && !script_library.isEmpty()) {
                        continue;
                    }

                    JSONArray integrations = (JSONArray) testEnvironment.get("integrations");
                    if (integrations != null && !integrations.isEmpty()) {
                        continue;
                    }

                    String name = (String) testEnvironment.get(NAME);
                    if (!name.equals("Test Settings")) {
                        continue;
                    }

                    // AT THIS POINT TEST ENVIRONMENT IS DEFAULT WITHOUT USER MODIFICATIONS
                    // => REMOVE IT

                    testEnvironmentIterator.remove();
                }
            }
        }
    }


    @SuppressWarnings("unchecked")
    private JSONArray resolveTestOrderBySubTestsAsLast(JSONArray testsJSONArray) {
        JSONArray reorderedTestsDetailsJSONArray = new JSONArray();

        JSONArray withSubTests = new JSONArray();
        JSONArray withoutSubTests = new JSONArray();

        JSONObject testJSONObject;

        // SPLIT TESTS INTO WITH AND WITHOUT SUBTESTS
        for (Object testObject : testsJSONArray) {
            testJSONObject = (JSONObject) testObject;
            JSONArray stepsJSONArray = (JSONArray) testJSONObject.get(STEPS);

            if (foundSubTestInTest(stepsJSONArray)) {
                withSubTests.add(testJSONObject);
            } else {
                withoutSubTests.add(testJSONObject);
            }
        }

        for (int i = 0; i < withSubTests.size(); i++) {
            testJSONObject = (JSONObject) withSubTests.get(i);
            String testName = (String) testJSONObject.get(NAME);

            // hack
            if (testName.toLowerCase().contains("auth")) {
                withSubTests.remove(testJSONObject);
                withSubTests.add(0, testJSONObject);
            }
        }

        reorderedTestsDetailsJSONArray.addAll(withoutSubTests);
        reorderedTestsDetailsJSONArray.addAll(withSubTests);

        return reorderedTestsDetailsJSONArray;
    }

    private boolean foundSubTestInTest(JSONArray stepsJSONArray) {
        for (Object stepObject : stepsJSONArray) {
            JSONObject stepJSONObject = (JSONObject) stepObject;
            String stepType = (String) stepJSONObject.get("step_type");

            if (stepType.equalsIgnoreCase("subtest")) {
                return true;
            }

            if (stepType.equalsIgnoreCase("condition")) {
                JSONArray conditionStepsJSONArray = (JSONArray) stepJSONObject.get(STEPS);
                return foundSubTestInTest(conditionStepsJSONArray);
            }
        }

        return false;
    }
}
