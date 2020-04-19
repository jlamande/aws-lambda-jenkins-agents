package io.jenkins.agent.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AgentHandler implements RequestHandler<Map<String, Object>, Response> {

    private static final Logger logger = LogManager.getLogger(AgentHandler.class);

    private static final String HOME_DIR = System.getProperty("HOME_DIR", "/tmp");

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public Response handleRequest(Map<String, Object> event, Context context) {
        debugContext(event, context);
        try {
            int runResult = this.runAgent(event);
            if (runResult != 0) {
                return new Response(context, 500, "Failed to run agent");
            }
        } catch (IOException | InterruptedException e) {
            logger.fatal("Failed to run agent");
            // throw new RuntimeException(e);
            return new Response(context, 500, "Failed to run agent : " + e.getMessage());
        }
        return new Response(context, 200, "The function executed successfully !");
    }

    void debugContext(Map<String,Object> event, Context context) {
        // log execution details
        logger.debug("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
        logger.debug("CONTEXT: " + gson.toJson(context));
        // process event
        logger.debug("EVENT: " + gson.toJson(event));
        logger.debug("EVENT TYPE: " + event.getClass().toString());
    }

    public int runAgent(Map<String, Object> event) throws IOException, InterruptedException {
        String command = "";
        if(event.get("command") != null) {
            command = (String) event.get("command");
        } else if (event.get("url") != null) {
            String url = (String) event.get("url");
            String nodeName = (event.get("node_name") != null) ? (String) event.get("node_name") : "";
            String nodeSecret = (event.get("node_secret") != null) ? (String) event.get("node_secret") : "";
            // real parameters for jenkins jnlp slave execution
            command = "java -cp .:/opt/java/lib/* -Duser.home=" + HOME_DIR + " -Dhudson.remoting.Engine.socketTimeout=1000 -Dorg.jenkinsci.remoting.engine.JnlpProtocol3.disabled=true -Dhudson.remoting.SynchronousCommandTransport.failOnSocketTimeoutInReader=true hudson.remoting.jnlp.Main -noreconnect -headless -url " + url + " " + nodeSecret + " " + nodeName;
        } else {
            command = "/bin/ls -al";
        }
        logger.info("Will execute command : " + command);
        ProcessBuilder builder = new ProcessBuilder(command.split(" "));
        Map<String, String> env = builder.environment();
        env.put("HOME", HOME_DIR);
        env.put("JENKINS_HOME", HOME_DIR);
        Process p = builder.inheritIO().start();
        int waitFor = p.waitFor();
        return waitFor;
    }
}
