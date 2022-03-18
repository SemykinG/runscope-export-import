package fi.vr.runscope.handlers;

import fi.vr.runscope.utils.AppUtils;
import fi.vr.runscope.parser.CustomParser;
import org.json.simple.JSONArray;

import java.io.*;
import java.net.URISyntaxException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHandler {

    private final static String FILE_NAME_PREFIX = "runscope_export_";
    private final static String FILE_NAME_EXTENSION = ".json";

    private final SimpleDateFormat ISO_8601_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private final CustomParser parser = new CustomParser();


    public void saveJSONStringToFile(String jsonString) {
        String currentPath = getCurrentDirectory();
        if (currentPath == null) {
            return;
        }

        String fileName = getFileName();
        if (fileName == null) {
            return;
        }

        try {
            FileWriter file = new FileWriter(currentPath + "/" + fileName);
            file.write(jsonString);
            file.close();

            AppUtils.print("");
            AppUtils.print("FILE SAVED AS: '"+fileName+"'");
            AppUtils.print("FILE SAVED IN: '"+currentPath+"'");
        } catch (IOException e) {
            AppUtils.print("");
            AppUtils.print("FAILED TO SAVE FILE IN CURRENT DIRECTORY");
            AppUtils.printErr(e.toString());
        }
    }

    public JSONArray FilePathToJSONArray(String pathToFile) {
        FileReader fileReader = null;

        try {
            if (pathToFile == null) {
                String pathToJSONFileInCurrentDirectory = getPathToJSONFileInCurrentDirectory();

                if (pathToJSONFileInCurrentDirectory != null) {
                    fileReader = new FileReader(pathToJSONFileInCurrentDirectory);
                }
            } else {
                fileReader = new FileReader(pathToFile);
            }

            if (fileReader != null) {
                return parser.parseFileReaderToJSONArray(fileReader);
            }
        } catch (FileNotFoundException e) {
            AppUtils.printErr("");
            AppUtils.printErr("JSON FILE NOT FOUND");
        }
        return null;
    }

    private String getCurrentDirectory() {
        try {
            String currentPath = new File(FileHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();

            if (currentPath.endsWith(".jar")) {
                currentPath = currentPath.substring(0, currentPath.lastIndexOf("\\"));
            }

            return currentPath;
        } catch (URISyntaxException e) {
            AppUtils.printErr("");
            AppUtils.printErr("COULD NOT RESOLVE CURRENT DIRECTORY PATH");
            AppUtils.printErr(e.toString());
        }

        return null;
    }

    private String getFileName() {
        try {
            return FILE_NAME_PREFIX + ISO_8601_DATEFORMAT.format(new Date(System.currentTimeMillis())).replaceAll("([^0-9])", "_") + FILE_NAME_EXTENSION;
        } catch (Exception e) {
            AppUtils.printErr("");
            AppUtils.printErr("COULD NOT CONSTRUCT FILE NAME");
            AppUtils.printErr(e.toString());
        }

        return null;
    }

    private String getPathToJSONFileInCurrentDirectory() throws FileNotFoundException {
        String currentDirectory = getCurrentDirectory();

        if (currentDirectory != null) {
            Set<String> files = Stream.of(Objects.requireNonNull(new File(currentDirectory).listFiles()))
                    .filter(file -> !file.isDirectory())
                    .map(File::getName)
                    .filter(name -> name.contains(FILE_NAME_PREFIX) && name.endsWith(FILE_NAME_EXTENSION))
                    .collect(Collectors.toSet());

            if (files.size() >= 1) {
                return currentDirectory + "/" + files.iterator().next();
            } else {
                throw new FileNotFoundException();
            }
        }
        return null;
    }
}
