package fi.vr.runscope.handlers;

import fi.vr.runscope.utils.AppUtils;
import fi.vr.runscope.Main;
import fi.vr.runscope.parser.CustomParser;
import fi.vr.runscope.rest.Request;
import fi.vr.runscope.rest.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

public class ExportHandler {

    private final Request requestHandler = new Request();
    private final CustomParser parser = new CustomParser();
    private final FileHandler fileHandler = new FileHandler();

    private List<JSONObject> bucketsToExport = new ArrayList<>();
    private JSONArray responseBuckets = new JSONArray();


    public void constructExportData(List<String> bucketKeys, String keyword) {
        if (responseBuckets.size() <= 0) {
            Response response = requestHandler.getAllBuckets();
            if (response.isRequestFailed()) {
                AppUtils.printErr(response.getResponse());
                return;
            }

            responseBuckets = parser.parseStringToJSONArray(parser.getResponseDataString(response.getResponse()));
        }

        bucketsToExport.clear();
        bucketsToExport = parser.constructData(bucketKeys, keyword, responseBuckets, "EXPORTED");

        if (bucketsToExport.size() <= 0) {
            AppUtils.print("");
            AppUtils.print("COULD NOT FIND ANY BUCKETS BY CRITERIA");
            return;
        }

        Main.waitingForUserResponseExport = true;
    }

    @SuppressWarnings("unchecked")
    public void handleExport(String userResponse) {
        if (Main.waitingForUserResponseExport) {
            if (userResponse.equalsIgnoreCase(AppUtils.YES)) {
                AppUtils.print("");
                AppUtils.print("EXPORTING DATA, PLEASE WAIT...");

                //export shared environments and test details. Add everything to *bucketsToExport*

                for (JSONObject bucket : bucketsToExport) {
                    String bucketKey = (String) bucket.get("key");
                    String bucketName = (String) bucket.get("name");

                    AppUtils.printInfo("");
                    AppUtils.printInfo("EXPORTING BUCKET: " + bucketName);

                    // EXPORT SHARED ENVIRONMENTS
                    Response sharedEnvResponse = requestHandler.getAllSharedEnvironmentsInBucket(bucketKey);
                    if (sharedEnvResponse.isRequestFailed()) {
                        System.err.println(sharedEnvResponse.getResponse());
                        return;
                    }

                    JSONArray sharedEnvironments = parser.parseStringToJSONArray(parser.getResponseDataString(sharedEnvResponse.getResponse()));
                    bucket.put(AppUtils.SHARED_ENVIRONMENTS, sharedEnvironments);

                    AppUtils.printInfo("-- SHARED ENVIRONMENTS EXPORTED");


                    // EXPORT TEST IDs
                    Response testsResponse = requestHandler.getAllTestsInBucket(bucketKey);
                    if (testsResponse.isRequestFailed()) {
                        AppUtils.printErr(testsResponse.getResponse());
                        return;
                    }

                    JSONArray tests = parser.parseStringToJSONArray(parser.getResponseDataString(testsResponse.getResponse()));
                    JSONArray testsJSONArray = new JSONArray();

                    for (Object testObject : tests) {
                        JSONObject JSONTest = (JSONObject) testObject;

                        String testId = (String) JSONTest.get("id");

                        // EXPORT TEST DETAILS
                        Response testDetailsResponse = requestHandler.getTestDetails(bucketKey, testId);
                        if (testDetailsResponse.isRequestFailed()) {
                            AppUtils.printErr(testDetailsResponse.getResponse());
                            return;
                        }

                        JSONObject testDetails = parser.parseStringToJSONObject(parser.getResponseDataString(testDetailsResponse.getResponse()));

                        // REMOVE UNNECESSARY LAST RUN DATA
                        testDetails.remove(AppUtils.LAST_RUN);

                        testsJSONArray.add(testDetails);
                    }

                    bucket.put(AppUtils.TESTS, testsJSONArray);

                    AppUtils.printInfo("-- TESTS EXPORTED");
                    AppUtils.printInfo("BUCKET EXPORTED");
                }

                AppUtils.print("");
                AppUtils.print("ALL DATA EXPORTED SUCCESSFULLY");

                // SAVE .JSON FILE IN CURRENT DIRECTORY
                fileHandler.saveJSONStringToFile(JSONArray.toJSONString(bucketsToExport));
            } else {
                AppUtils.print("");
                AppUtils.print("EXPORT CANCELLED");
            }
            Main.waitingForUserResponseExport = false;
        }
    }
}
