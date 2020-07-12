/**
* SageMaker API Invocation interface
*
* @author  Raja SP
*/

package com.accelerators;

import java.nio.ByteBuffer;
import com.amazonaws.services.sagemakerruntime.AmazonSageMakerRuntime;
import com.amazonaws.services.sagemakerruntime.AmazonSageMakerRuntimeClientBuilder;
import com.amazonaws.services.sagemakerruntime.model.InvokeEndpointRequest;
import com.amazonaws.services.sagemakerruntime.model.InvokeEndpointResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EA {

    private static AmazonSageMakerRuntime amazonSageMaker; 

    public static void initialise() {
        amazonSageMaker = AmazonSageMakerRuntimeClientBuilder.defaultClient();
    }
    
    public static double invokeSageMakerModel( double input, String endpointName ) {
        ByteBuffer bodyBuffer = ByteBuffer.wrap(Double.valueOf( input ).toString().getBytes());

        InvokeEndpointRequest invokeEndpointRequest = new InvokeEndpointRequest();
        invokeEndpointRequest.setBody(bodyBuffer);
        invokeEndpointRequest.setContentType("text/csv");
        invokeEndpointRequest.setEndpointName("ea-discounts-regression-v1");
        invokeEndpointRequest.setAccept("application/json");

        InvokeEndpointResult invokeEndpointResult = amazonSageMaker.invokeEndpoint(invokeEndpointRequest);
        String bodyResponse = new String(invokeEndpointResult.getBody().array());
        JsonObject jobj = (JsonObject) JsonParser.parseString(bodyResponse);
        return jobj.get( "predictions" ).getAsJsonArray().get( 0 ).getAsJsonObject().get( "score").getAsDouble();
    }

}