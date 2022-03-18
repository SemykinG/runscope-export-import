package fi.vr.runscope.parser;

import fi.vr.runscope.utils.AppUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CustomParser {

    private final JSONParser jsonParser = new JSONParser();


    public JSONObject parseStringToJSONObject(String str) {
        try {
            return (JSONObject) jsonParser.parse(str);
        } catch (ParseException e) {
            AppUtils.printErr("");
            AppUtils.printErr("JSON OBJECT PARSING ERROR");
            AppUtils.printErr(e.toString());
            AppUtils.printErr("STRING: " + str);
        }
        return null;
    }

    public JSONArray parseStringToJSONArray(String str) {
        try {
            return (JSONArray) jsonParser.parse(str);
        } catch (ParseException e) {
            AppUtils.printErr("");
            AppUtils.printErr("JSON ARRAY PARSING ERROR");
            AppUtils.printErr(e.toString());
            AppUtils.printErr("STRING: " + str);
        }
        return null;
    }

    public JSONArray parseFileReaderToJSONArray(FileReader fileReader) {
        try {
            JSONArray jsonArray = (JSONArray) jsonParser.parse(fileReader);
            fileReader.close();

            return jsonArray;
        } catch (ParseException e) {
            AppUtils.printErr("");
            AppUtils.printErr("JSON ARRAY PARSING ERROR");
            AppUtils.printErr(e.toString());
        } catch (IOException e) {
            AppUtils.printErr("");
            AppUtils.printErr("JSON FILE READING ERROR");
            AppUtils.printErr(e.toString());
        }
        return null;
    }

    public String getResponseDataString(String str) {
        try {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(str);
            Object data = jsonObject.get("data");

            String response;

            if (data instanceof JSONObject) {
                response = parseStringToJSONObject(data.toString()).toString();
            } else if (data instanceof JSONArray) {
                response = parseStringToJSONArray(data.toString()).toString();
            } else {
                AppUtils.printErr("");
                AppUtils.printErr("COULD NOT PARSE RESPONSE:");
                AppUtils.printErr(str);
                return null;
            }

            return response;
        } catch (ParseException e) {
            AppUtils.printErr("");
            AppUtils.printErr("JSON OBJECT PARSING ERROR");
            AppUtils.printErr(e.toString());
            AppUtils.printErr("STRING: " + str);
        }
        return null;
    }

    public String getEncodedURLForBucket(JSONObject bucket) {
        String bucketName = (String) bucket.get("name");

        JSONObject team = (JSONObject) bucket.get("team");
        String teamId = (String) team.get("id");

        try {
            return "https://api.runscope.com/buckets?name=" +
                    URLEncoder.encode(bucketName, StandardCharsets.UTF_8.toString()) +
                    "&team_uuid=" +
                    URLEncoder.encode(teamId, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            AppUtils.printErr("");
            AppUtils.printErr("FAILED TO ENCODE BUCKET URL");
            AppUtils.printErr("NAME: " + bucketName);
            AppUtils.printErr("TEAM ID: " + teamId);
            e.printStackTrace();
        }
        return null;
    }

    public List<JSONObject> constructData(List<String> bucketKeys, String keyword, JSONArray bucketsArrays, String message) {

        List<JSONObject> bucketsFinal = new ArrayList<>();

        if (keyword.length() > 0) {
            for (Object bucketObject : bucketsArrays) {
                JSONObject bucket = (JSONObject) bucketObject;

                if (((String) bucket.get("name")).toLowerCase().contains(keyword.toLowerCase())) {
                    bucketsFinal.add(bucket);
                }
            }
        } else {
            // FILTER BUCKETS BY BUCKET KEYS
            if (bucketKeys != null) {
                for (Object bucketObject : bucketsArrays) {
                    JSONObject bucket = (JSONObject) bucketObject;

                    for (String bucketKey : bucketKeys) {
                        if (((String) bucket.get("key")).equalsIgnoreCase(bucketKey)) {
                            bucketsFinal.add(bucket);
                            break;
                        }
                    }
                }
            } else {
                // ALL BUCKETS
                for (Object bucketObject : bucketsArrays) {
                    JSONObject bucket = (JSONObject) bucketObject;
                    bucketsFinal.add(bucket);
                }
            }
        }

        if (bucketsFinal.size() > 0) {
            AppUtils.print("");
            bucketsFinal.forEach(bucket -> AppUtils.print(((String) bucket.get("name"))));

            AppUtils.print("");
            AppUtils.print(bucketsFinal.size() + " BUCKET(S) WILL BE " + message + ", " + AppUtils.CONTINUE_Y_N);
        }

        return bucketsFinal;
    }

    public void parseAccountDetails(String response) {
        JSONObject account = parseStringToJSONObject(getResponseDataString(response));
        JSONArray teams = (JSONArray) account.get("teams");
        if (teams != null) {
            for (Object teamObject : teams) {
                JSONObject teamJSON = (JSONObject) teamObject;
                String teamName = (String) teamJSON.get("name");
                String teamId = (String) teamJSON.get("id");
                AppUtils.print("");
                AppUtils.print("TEAM NAME: " + teamName);
                AppUtils.print("TEAM ID: " + teamId);
            }
        }
    }
}
