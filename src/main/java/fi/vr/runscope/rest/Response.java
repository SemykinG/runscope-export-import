package fi.vr.runscope.rest;

public class Response {

    private boolean requestFailed = false;
    private String response = null;

    public boolean isRequestFailed() {
        return requestFailed;
    }

    public void setRequestFailed(boolean requestFailed) {
        this.requestFailed = requestFailed;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
