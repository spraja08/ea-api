package com.accelerators;

import org.restlet.Component;
import org.restlet.data.Protocol;

public class APIServer {

    public static void main(String[] args) throws Exception {
    	if( args.length < 4 ) {
    		System.err.println( "Usage : java -jar entity-analytics.jar <path to the resources> <Entities separated by comma> <entity360StoreIP> <entity360StorePort>" );
    		return;
    	}
        Component component = new Component();
        int port = 8111;
        component.getServers().add(Protocol.HTTP, port);

        component.getDefaultHost().attach("/api/v1", new APIApplication());

        ElasticEntity360Store.elasticSearchIP = args[2];
        ElasticEntity360Store.elasticSearchPort = Integer.parseInt( args[3]);

        EA.initialise();
        //initialse the application
        ScriptsDef.initalise( args[ 0 ] );

        String[] entities = args[ 1 ].split( "," );
        EntityAnalyser.entityTypes = entities;

        // Start the component.
        component.start();
    }
}