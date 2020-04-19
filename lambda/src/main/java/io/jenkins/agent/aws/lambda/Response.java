package io.jenkins.agent.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;

import java.util.Map;

public class Response {

    private int exitCode = 200;
    private String message = "";
    private Context context;
	private Map<String, String> headers = new java.util.HashMap<String, String>();

    public Response() {
        context = null;
    }

    public Response(Context context, int exitCode, String message) {
        this.context = context;
        this.exitCode = exitCode;
        this.message = message;
    }

    public Response(Context context, int exitCode, String message, Map<String, String> headers) {
        this.context = context;
        this.exitCode = exitCode;
        this.message = message;
        this.headers = headers;
    }

    int getExitCode() {
        return this.exitCode;
    }

    public String getMessage() {
        return this.message;
    }

	public Map<String, String> getHeaders() {
		return headers;
	}

	public Context getContext() {
		return context;
	}
}
