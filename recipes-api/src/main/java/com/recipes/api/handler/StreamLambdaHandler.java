package com.recipes.api.handler;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.recipes.api.RecipesApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * AWS Lambda entry point for the Recipes API.
 *
 * <p>This class is the handler configured in AWS Lambda (and in {@code template.yaml} for local
 * SAM testing). When API Gateway receives an HTTP request, it serialises it as an
 * {@link AwsProxyRequest} and invokes {@link #handleRequest} on this class.
 *
 * <p><b>How it works:</b><br>
 * {@link SpringBootLambdaContainerHandler} (from the AWS Serverless Java Container library)
 * initialises the full Spring Boot application context and translates each incoming
 * {@link AwsProxyRequest} into a servlet request that Spring MVC's {@code DispatcherServlet}
 * can process. The response is translated back into an {@link AwsProxyResponse} and written
 * to the output stream for API Gateway to return to the caller.
 *
 * <p><b>Cold starts:</b><br>
 * The {@code SpringBootLambdaContainerHandler} is initialised once in a {@code static} block.
 * On the first Lambda invocation (cold start), this triggers the full Spring Boot context
 * initialisation, which takes a few seconds. Subsequent invocations on a warm container
 * reuse the already-initialised context and respond much faster.
 *
 * <p><b>Local development:</b><br>
 * This class is <em>not</em> used when running locally via the {@code local} Maven/Spring
 * profile. In that mode, the application starts as a standard Spring Boot app with an
 * embedded Tomcat server on {@code localhost:8080}. Use AWS SAM ({@code sam local start-api})
 * to test the Lambda execution path locally.
 */
public class StreamLambdaHandler implements RequestStreamHandler {

    private static final SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> HANDLER;

    static {
        try {
            HANDLER = SpringBootLambdaContainerHandler.getAwsProxyHandler(RecipesApplication.class);
        } catch (ContainerInitializationException e) {
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        HANDLER.proxyStream(inputStream, outputStream, context);
    }
}
