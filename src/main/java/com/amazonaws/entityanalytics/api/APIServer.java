/**
* REST API - Restlet main entry point
*
* @author  Raja SP
*/

package com.amazonaws.entityanalytics.api;

import org.restlet.Component;
import org.restlet.data.Protocol;

public class APIServer {

    public static void main(String[] args) throws Exception {
    	if( args.length < 3 ) {
    		System.err.println( "Usage : java -jar entity-analytics.jar <path to the resources> <entity360StoreIP> <entity360StorePort>" );
    		return;
    	}
        Component component = new Component();
        int port = 8111;
        component.getServers().add(Protocol.HTTP, port);

        component.getDefaultHost().attach("/api/v1", new APIApplication());

        ElasticEntity360Store.elasticSearchIP = args[1];
        ElasticEntity360Store.elasticSearchPort = Integer.parseInt(args[2]);

        EA.initialise();
        //initialse the application
        ScriptsDef.initalise( args[ 0 ] );

        // Start the component.
        component.start();
    }
}