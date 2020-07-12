package com.accelerators;

import java.util.Arrays;
import java.util.HashSet;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.service.CorsService;

public class APIApplication extends Application {

    public APIApplication() {
        CorsService corsService = new CorsService();
        corsService.setAllowingAllRequestedHeaders( true );
        corsService.setAllowedOrigins( new HashSet( Arrays.asList( "*" ) ) );
        corsService.setAllowedCredentials( true) ;

        getServices().add( corsService );
    }

	@Override
	public Restlet createInboundRoot() {
		// Create a router Restlet that defines routes.
        Router router = new Router( getContext() );
            
		// Defines a route for the resource "list of items"
		router.attach( "/getOffers", EntityAnalyser.class );
        router.attach( "/adps", ADPsAPI.class );
        router.attach( "/adps/{id}", ADPsAPI.class );

       router.attach( "/states", StatesAPI.class );
       router.attach( "/states/{id}", StatesAPI.class );

       router.attach( "/events", EventsAPI.class );
       router.attach( "/events/{id}", EventsAPI.class );

       router.attach( "/entities", EntitiesAPI.class );
       router.attach( "/entities/{id}", EntitiesAPI.class );

		return router;
	}
}