package fi.vr.runscope.rest;

import fi.vr.runscope.parser.CustomParser;
import fi.vr.runscope.utils.AppUtils;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Request {

    private final static String BASE_URL = "https://api.runscope.com";

    private final CustomParser parser = new CustomParser();


    public void getAccountDetails() {
        Response response = sendRequest(constructGetRequest(BASE_URL + "/account"));
        if (response == null || response.isRequestFailed()) {
            AppUtils.printErr("COULD NOT GET ACCOUNT DETAILS");
            return;
        }
        parser.parseAccountDetails(response.getResponse());
    }

    public Response getAllBuckets() {
        return sendRequest(constructGetRequest(BASE_URL + "/buckets"));
    }

    private Response getBucketIfExists(String bucketKey) {
        return sendRequest(constructGetRequest(BASE_URL + "/v1/buckets/" + bucketKey));
    }


    public Response getAllSharedEnvironmentsInBucket(String bucketKey) {
        return sendRequest(constructGetRequest(BASE_URL + "/buckets/" + bucketKey + "/environments"));
    }

    private Response getSharedEnvironmentIfExists(String bucketKey, String environmentId) {
        return sendRequest(constructGetRequest(BASE_URL + "/buckets/" + bucketKey + "/environments/" + environmentId));
    }

    private Response getTestEnvironmentIfExists(String bucketKey, String testId , String testEnvironmentId) {
        return sendRequest(constructGetRequest(BASE_URL + "/buckets/" + bucketKey + "/tests/" + testId + "/environments/" + testEnvironmentId));
    }


    public Response getAllTestsInBucket(String bucketKey) {
        return sendRequest(constructGetRequest(BASE_URL + "/buckets/" + bucketKey + "/tests?count=200"));
    }

    public Response getAllSchedulesInBucketInTest(String bucketKey, String testId) {
        return sendRequest(constructGetRequest(BASE_URL + "/buckets/" + bucketKey + "/tests/" + testId + "/schedules"));
    }

    public Response getTestIfExists(String bucketKey, String testId) {
        return sendRequest(constructGetRequest(BASE_URL + "/buckets/" + bucketKey + "/tests/" + testId));
    }

    public Response getTestDetails(String bucketKey, String testId) {
        return sendRequest(constructGetRequest(BASE_URL + "/buckets/" + bucketKey + "/tests/" + testId));
    }

    private Response getStepIfExists(String bucketKey, String testId , String stepId) {
        return sendRequest(constructGetRequest(BASE_URL + "/buckets/" + bucketKey + "/tests/" + testId + "/steps/" + stepId));
    }


    public Response handleBucketExistence(JSONObject bucket) {
        String bucketKey = (String) bucket.get("key");

        Response bucketResponse = getBucketIfExists(bucketKey);
        if (bucketResponse == null) {
            AppUtils.printErr("FAILED TO RETRIEVE BUCKET BY KEY: '" + bucketKey + "'");
            return null;
        }

        if (bucketResponse.isRequestFailed()) {
            AppUtils.printInfo("> BUCKET NOT FOUND IN RUNSCOPE");

            if (AppUtils.CREATE_IF_MISSING_OPTION) {
                String bucketUrl = parser.getEncodedURLForBucket(bucket);
                if (bucketUrl == null) {
                    return null;
                }

                bucketResponse = sendRequest(constructCreateBucketRequest(bucketUrl));

                if (bucketResponse == null) {
                    AppUtils.printErr("");
                    AppUtils.printErr("FAILED TO CREATE BUCKET");
                    return null;
                }

                if (bucketResponse.isRequestFailed()) {
                    AppUtils.printErr("");
                    AppUtils.printErr(bucketResponse.getResponse());
                    return null;
                }

                AppUtils.printInfo("> BUCKET CREATED");
            } else {
                AppUtils.printInfo("> BUCKET CREATION SKIPPED");
            }
        } else {
            AppUtils.printInfo("> BUCKET FOUND IN RUNSCOPE");
        }

        return bucketResponse;
    }

    public Response handleSharedEnvironmentExistence(String bucketKey, JSONObject sharedEnvironment) {
        String environmentId = (String) sharedEnvironment.get("id");

        Response sharedEnvironmentResponse = getSharedEnvironmentIfExists(bucketKey, environmentId);
        if (sharedEnvironmentResponse == null) {
            AppUtils.printErr("FAILED TO RETRIEVE SHARED ENVIRONMENT BY ID: '" + environmentId + "', IN BUCKET: '" + bucketKey + "'");
            return null;
        }

        if (sharedEnvironmentResponse.isRequestFailed()) {
            AppUtils.printInfo("> SHARED ENVIRONMENT NOT FOUND IN RUNSCOPE");

            if (AppUtils.CREATE_IF_MISSING_OPTION) {
                sharedEnvironmentResponse = sendRequest(constructPostRequest(BASE_URL + "/buckets/" + bucketKey + "/environments", sharedEnvironment.toString()));

                AppUtils.printInfo("> SHARED ENVIRONMENT CREATED");
            } else {
                AppUtils.printInfo("> SHARED ENVIRONMENT CREATION SKIPPED");
            }
        } else {
            AppUtils.printInfo("> SHARED ENVIRONMENT FOUND IN RUNSCOPE");

            if ((AppUtils.EDIT_IF_EXISTS_OPTION)) {
                sharedEnvironmentResponse = sendRequest(constructPutRequest(BASE_URL + BASE_URL + "/buckets/" + bucketKey + "/environments/" + environmentId, sharedEnvironment.toString()));

                AppUtils.printInfo("> SHARED ENVIRONMENT EDITED");
            } else {
                AppUtils.printInfo("> SHARED ENVIRONMENT EDIT SKIPPED");
            }
        }


        return sharedEnvironmentResponse;
    }

    public Response handleTestExistence(String bucketKey, JSONObject test, boolean forceUpdate) {
        String testId = (String) test.get("id");

        Response testResponse = getTestIfExists(bucketKey, testId);
        if (testResponse == null) {
            AppUtils.printErr("FAILED TO RETRIEVE TEST BY ID: '" + testId + "', IN BUCKET: '" + bucketKey + "'");
            return null;
        }

        if (testResponse.isRequestFailed()) {
            AppUtils.printInfo("> TEST NOT FOUND IN RUNSCOPE");

            if (AppUtils.CREATE_IF_MISSING_OPTION) {
                testResponse = sendRequest(constructPostRequest(BASE_URL + "/buckets/" + bucketKey + "/tests", test.toString()));

                AppUtils.printInfo("> TEST CREATED");
            } else {
                AppUtils.printInfo("> TEST CREATION SKIPPED");
            }
        } else {
            if (!forceUpdate) {
                AppUtils.printInfo("> TEST FOUND IN RUNSCOPE");
            }

            if (AppUtils.EDIT_IF_EXISTS_OPTION || forceUpdate) {
                testResponse = sendRequest(constructPutRequest(BASE_URL + "/buckets/" + bucketKey + "/tests/" + testId, test.toString()));

                if (!forceUpdate) {
                    AppUtils.printInfo("> TEST EDITED");
                }
            } else {
                AppUtils.printInfo("> TEST EDIT SKIPPED");
            }
        }

        return testResponse;
    }

    public Response handleTestEnvironmentExistence(String bucketKey, String testId, JSONObject testEnvironment) {
        String testEnvironmentId = (String) testEnvironment.get("id");

        Response testEnvironmentResponse = getTestEnvironmentIfExists(bucketKey, testId, testEnvironmentId);
        if (testEnvironmentResponse == null) {
            AppUtils.printErr("FAILED TO RETRIEVE TEST ENVIRONMENT BY ID: '" + testEnvironmentId + "', IN BUCKET: '" + bucketKey + "', IN TEST: '" + testId + "'");
            return null;
        }

        if (testEnvironmentResponse.isRequestFailed()) {
            AppUtils.printInfo("> TEST ENVIRONMENT NOT FOUND IN RUNSCOPE");

            if (AppUtils.CREATE_IF_MISSING_OPTION) {
                testEnvironmentResponse = sendRequest(constructPostRequest(BASE_URL + "/buckets/" + bucketKey + "/tests/" + testId + "/environments", testEnvironment.toString()));

                AppUtils.printInfo("> TEST ENVIRONMENT CREATED");
            } else {
                AppUtils.printInfo("> TEST ENVIRONMENT CREATION SKIPPED");
            }
        } else {
            AppUtils.printInfo("> TEST ENVIRONMENT FOUND IN RUNSCOPE");

            if ((AppUtils.EDIT_IF_EXISTS_OPTION)) {
                testEnvironmentResponse = sendRequest(constructPutRequest(BASE_URL + "/buckets/" + bucketKey + "/tests/" + testId + "/environments/" + testEnvironmentId, testEnvironment.toString()));

                AppUtils.printInfo("> TEST ENVIRONMENT EDITED");
            } else {
                AppUtils.printInfo("> TEST ENVIRONMENT EDIT SKIPPED");
            }
        }

        return testEnvironmentResponse;
    }

    public Response handleStepExistence(String bucketKey, String testId, JSONObject step) {
        String stepId = (String) step.get("id");

        Response stepResponse = getStepIfExists(bucketKey, testId, stepId);
        if (stepResponse == null) {
            AppUtils.printErr("FAILED TO RETRIEVE STEP BY ID: '" + stepId + "', IN BUCKET: '" + bucketKey + "', IN TEST: '" + testId + "'");
            return null;
        }

        if (stepResponse.isRequestFailed()) {
            AppUtils.printInfo("> STEP NOT FOUND IN RUNSCOPE");

            if (AppUtils.CREATE_IF_MISSING_OPTION) {
                stepResponse = sendRequest(constructPostRequest(BASE_URL + "/buckets/" + bucketKey + "/tests/" + testId + "/steps", step.toString()));

                AppUtils.printInfo("> STEP CREATED");
            } else {
                AppUtils.printInfo("> STEP CREATION SKIPPED");
            }
        } else {
            AppUtils.printInfo("> STEP FOUND IN RUNSCOPE");

            if ((AppUtils.EDIT_IF_EXISTS_OPTION)) {
                stepResponse = sendRequest(constructPutRequest(BASE_URL + "/buckets/" + bucketKey + "/tests/" + testId + "/steps/" + stepId, step.toString()));

                AppUtils.printInfo("> STEP EDITED");
            } else {
                AppUtils.printInfo("> STEP EDIT SKIPPED");
            }
        }

        return stepResponse;
    }

    public boolean handleScheduleExistence(String bucketKey, String testId, String scheduleId, JSONObject schedule) {
        Response response = null;

        if (scheduleId.length() <= 0) {
            AppUtils.printInfo("> SCHEDULE NOT FOUND IN RUNSCOPE");

            if (AppUtils.CREATE_IF_MISSING_OPTION) {
                response = sendRequest(constructPostRequest(BASE_URL + "/v1/buckets/" + bucketKey + "/tests/" + testId + "/schedules", schedule.toString()));

                AppUtils.printInfo("> SCHEDULE CREATED");
            } else {
                AppUtils.printInfo("> SCHEDULE CREATION SKIPPED");
                return true;
            }
        } else {
            AppUtils.printInfo("> SCHEDULE FOUND IN RUNSCOPE");

            if ((AppUtils.EDIT_IF_EXISTS_OPTION)) {
                response = sendRequest(constructPutRequest(BASE_URL + "/buckets/" + bucketKey + "/tests/" + testId + "/schedules/" + scheduleId, schedule.toString()));

                AppUtils.printInfo("> SCHEDULE EDITED");
            } else {
                AppUtils.printInfo("> SCHEDULE EDIT SKIPPED");
                return true;
            }
        }

        return response != null && !response.isRequestFailed();
    }



    private Response sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = HttpClient
                    .newBuilder()
                    .proxy(ProxySelector.getDefault())
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            Response requestResponse = new Response();
            requestResponse.setRequestFailed(response.statusCode() != 200 && response.statusCode() != 201);
            requestResponse.setResponse(response.body());

            return requestResponse;
        } catch (IOException | InterruptedException e) {
            AppUtils.printErr("REQUEST ERROR");
            e.printStackTrace();
        }

        return null;
    }

    private HttpRequest constructGetRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + AppUtils.API_KEY)
                .GET()
                .build();
    }

    private HttpRequest constructCreateBucketRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + AppUtils.API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString("null"))
                .build();
    }

    private HttpRequest constructPostRequest(String url, String data) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + AppUtils.API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();
    }

    private HttpRequest constructPutRequest(String url, String data) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + AppUtils.API_KEY)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(data))
                .build();
    }
}
