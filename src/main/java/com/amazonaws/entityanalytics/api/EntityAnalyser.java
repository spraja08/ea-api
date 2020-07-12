/**
* Executes snippets for ADPs and States
*
* @author  Raja SP
*/

package com.amazonaws.entityanalytics.api;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.codehaus.commons.compiler.CompileException;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class EntityAnalyser extends ServerResource {

    public static String[ ] entityTypes;

    @Override
    protected void doInit() throws ResourceException {
    }

    @Override
    protected JsonRepresentation post(Representation entity) throws ResourceException {
        JsonObject incomingEvent = null;
        JsonObject result = new JsonObject();
        try {
            System.out.println("POST Called - " + entity.toString());
            incomingEvent = (JsonObject) JsonParser.parseString(entity.getText());
        } catch (JsonSyntaxException e1) {
            e1.printStackTrace();
            result.addProperty("status", "FAILED");
            result.addProperty("reason", e1.getMessage());
            JsonRepresentation rep = new JsonRepresentation(result.toString());
            return rep;
        } catch (IOException e1) {
            e1.printStackTrace();
            result.addProperty("status", "FAILED");
            result.addProperty("reason", e1.getMessage());
            JsonRepresentation rep = new JsonRepresentation(result.toString());
            return rep;
        }
        try {
            JsonObject allEntities = ScriptsDef.get( "entities" );
            Iterator<String> keysItr = allEntities.keySet().iterator();
            while( keysItr.hasNext() ) {
                String entityType = keysItr.next();
                JsonObject entityDef = allEntities.get(entityType).getAsJsonObject();
                String idField = entityDef.get("id").getAsString();
                String entityId = null;
                // if the entity is GeoFence based, check if the event lies within the GeoFence. If not, skip the entity
                if( entityDef.has( "geoFence" ) ) {
                    if( incomingEvent.has( "latitude" ) && incomingEvent.has( "longitude" ) ) {
                        if( ! geoFenceMatch( entityDef, incomingEvent ) )
                            continue;
                        else
                            entityId = idField;    
                    } else
                        continue;
                } // For non geofence based entities, check if the EntityId(ex.customerId) is present in the Event.
                else if( incomingEvent.has( idField ) ) {
                    entityId = incomingEvent.get( idField ).getAsString();
                } else
                    continue;
                JsonObject entity360 = handleIncomingEvent( incomingEvent, entityType, entityId );
                result.add( entityId, entity360 );
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            result.addProperty("status", "FAILED");
            result.addProperty("reason", e.getMessage());
        }
        System.out.println(result.toString());
        JsonRepresentation rep = new JsonRepresentation(Entity360Store.getInstance().dumpAll().toString());
        return rep;
    }

    public static void main(String[] args) throws IOException, CompileException, InvocationTargetException {
        ScriptsDef.initalise( "/Users/rspamzn/cloudLabs/ea/cdp/resources" );

        JsonObject incomingEvent = new JsonObject();
        String customerId = "raja";
        incomingEvent.addProperty( "event", "TrainBooking" );
        incomingEvent.addProperty( "customerId", customerId );
        incomingEvent.addProperty( "timestamp", new java.util.Date().getTime() );
        incomingEvent.addProperty( "origin", "Jakarta" );
        incomingEvent.addProperty( "destination", "Bali" );
        incomingEvent.addProperty( "latitude", -6.194772 );
        incomingEvent.addProperty( "longitude", 106.815968 );
        incomingEvent.addProperty( "airline", "SQ" );
        incomingEvent.addProperty( "flightId", "SQ065" );

        EntityAnalyser api = new EntityAnalyser();
        EntityAnalyser.entityTypes = new String[3];
        EntityAnalyser.entityTypes[0] = "Customer";
        EntityAnalyser.entityTypes[1] = "Airline";
        EntityAnalyser.entityTypes[2] = "ThamrinCity";

        JsonObject result = new JsonObject();

        for (int i = 0; i < entityTypes.length; i++) {
            String entityType = EntityAnalyser.entityTypes[i].trim();
            JsonObject entityDef = ScriptsDef.get("entities").get(entityType).getAsJsonObject();
            String idField = entityDef.get("id").getAsString();
            String entityId = null;
            // if the entity is GeoFence based, check if the event lies within the GeoFence. If not, skip the entity
            if( entityDef.has( "geoFence" ) ) {
                if( incomingEvent.has( "latitude" ) && incomingEvent.has( "longitude" ) ) {
                    if( ! geoFenceMatch( entityDef, incomingEvent ) )
                        continue;
                    else
                        entityId = idField;    
                } else
                    continue;
            } // For non geofence based entities, check if the EntityId(ex.customerId) is present in the Event.
            else if( incomingEvent.has( idField ) ) {
                entityId = incomingEvent.get( idField ).getAsString();
            } else
                continue;
            JsonObject entity360 = api.handleIncomingEvent( incomingEvent, entityType, entityId );
            result.add( entityId, entity360 );
        }
        System.out.println( result );
    }

    private static boolean geoFenceMatch( JsonObject entityDef, JsonObject input ) {
		double angle = 0;
		double dblPoint1Lat;
		double dblPoint1Long;
		double dblPoint2Lat;
		double dblPoint2Long;
        double dblPi = 3.14159265;
        
        double latitude = input.get( "latitude" ).getAsDouble();
        double longitude = input.get( "longitude" ).getAsDouble();
        JsonArray dblLatArray = entityDef.get( "latArray" ).getAsJsonArray();
        JsonArray dblLongArray = entityDef.get( "lngArray" ).getAsJsonArray();

        int n = dblLatArray.size();
        
		for( int i = 0; i < n; i++ ) {
			dblPoint1Lat = dblLatArray.get( i ).getAsDouble() - latitude;
			dblPoint1Long = dblLongArray.get( i ).getAsDouble() - longitude;
			dblPoint2Lat = dblLatArray.get( ( i + 1 ) % n ).getAsDouble() - latitude;
			dblPoint2Long = dblLongArray.get( ( i + 1 ) % n ).getAsDouble() - longitude;
			angle += get2DAngle( dblPoint1Lat, dblPoint1Long, dblPoint2Lat, dblPoint2Long );
		}

		if( Math.abs( angle ) < dblPi )
			return false;
		else
            return true;    
    }

    public static double get2DAngle( double y1, double x1, double y2, double x2 ) {
		double dtheta, theta1, theta2;
        double dblPi = 3.14159265;
        double dbl2Pi = 2 * dblPi;

		theta1 = Math.atan2( y1, x1 );
		theta2 = Math.atan2( y2, x2 );
		dtheta = theta2 - theta1;
		while( dtheta > dblPi )
			dtheta -= dbl2Pi;
		while( dtheta < -dblPi )
			dtheta += dbl2Pi;

		return ( dtheta );
	}


    private JsonObject handleIncomingEvent(JsonObject inputEvent, String entityType, String entityId)
            throws InvocationTargetException {
        JsonObject entityADPs = Entity360Store.getInstance().getEntityADPs(entityId);
        Context context = new Context(entityADPs, inputEvent, new JsonObject());
        computeADPs(context, entityType );
        computeSegments(context, entityType );

        //JsonArray engagements = computeEngagements(context);
        //update latest customer state
        Entity360Store.getInstance().setEntity360( entityId, context.entityADPs, context.entityStates );
        JsonObject result = new JsonObject();
        result.add( "ADPs", context.entityADPs );
        result.add( "states", context.entityStates );
        //result.add( "engagements", engagements );
        return result;
    }

    private void computeSegments( Context context, String entityType ) throws InvocationTargetException {
        Set<String> keys = ScriptsDef.get( "states" ).keySet();
        Iterator<String> itr = keys.iterator();
        while (itr.hasNext()) {
            String id = itr.next();
            JsonObject thisState = ScriptsDef.get( "states" ).get( id ).getAsJsonObject();
            JsonArray entities = thisState.get( "entity" ).getAsJsonArray();
            boolean entityMatch = false;
            for( int i=0; i<entities.size(); i++ ) {
                if( entities.get(i).getAsString().equals( entityType ) ) {
                    entityMatch = true;
                    break;
                }
            }
            if( ! entityMatch )
               continue;
            Object result = evaluate( id, context );
            addToStore( id, result, context );
        }
    }

    private void computeADPs(Context context, String entityType ) throws InvocationTargetException {
        Set<String> keys = ScriptsDef.get( "adps" ).keySet();
        Iterator<String> itr = keys.iterator();
        while (itr.hasNext()) {
            String id = itr.next();
            JsonObject thisADP = ScriptsDef.get( "adps" ).get( id ).getAsJsonObject();
            JsonArray entities = thisADP.get( "entity" ).getAsJsonArray();
            boolean entityMatch = false;
            for( int i=0; i<entities.size(); i++ ) {
                if( entities.get(i).getAsString().equals( entityType ) ) {
                    entityMatch = true;
                    break;
                }
            }
            if( ! entityMatch )
               continue;
            boolean eventMatched = false;
            String incomingEvent = context.input.get( "event" ).getAsString();
            if( thisADP.has( "events" ) ) {
                JsonArray arrEvents = thisADP.get( "events").getAsJsonArray();
                for( int i=0; i<arrEvents.size(); i++ ) {
                    if( incomingEvent.equals( arrEvents.get(i).getAsString() ) ) {
                        eventMatched = true;
                        break;
                    }
                }
            } 
            if( eventMatched ) {
                Object result = evaluate( id, context );
                addToStore( id, result, context );
            } else {
                context.processedScripts.add( id );
            }
        }
    }

    private void addToStore( String key, Object value, Context context ) {
        JsonObject destination = context.entityADPs;
        if( ScriptsDef.get( "states" ).has( key ) ) 
            destination = context.entityStates;
        
        Class type = ScriptsDef.scriptsMap.get(key).returnTypeClass;
        
        if( type == double.class ) {
			destination.addProperty( key, ( Double ) value );
		}else if( type == int.class ) {
            destination.addProperty( key, ( Integer ) value );
        }else if( type == long.class ) {
			destination.addProperty( key, ( Long ) value );
		}else if( type == boolean.class ) {
			destination.addProperty( key, value.toString().equals( "true" ) ? true : false  );
		}else if( type == Double.class )
			destination.addProperty( key, ( Double ) value );
		else if( type == Boolean.class )
			destination.addProperty( key, ( Boolean ) value );
		else if( type == Float.class )
			destination.addProperty( key, ( Float ) value );
		else if( type == Integer.class )
			destination.addProperty( key, ( Integer ) value );
		else if( type == String.class )
			destination.addProperty( key, ( String ) value );
		else if( type == boolean.class )
			destination.addProperty( key, ( Boolean ) value );
		else if( type == JsonArray.class )
			destination.add( key, ( JsonArray ) value );
		else if( type == JsonObject.class )
			destination.add( key, ( JsonObject ) value );
    }

    private Object evaluate(String id, Context context) throws InvocationTargetException {
        if( context.processedScripts.contains( id ) )  
            return getValueFromCurrentState( id, context );
            
        Script thisScript = ScriptsDef.scriptsMap.get( id );    
        Iterator< String > paramsItr = thisScript.paramNames.iterator();
        Object[ ] parameterValues = new Object[ thisScript.paramNames.size() ];
        int i = 0;
        while( paramsItr.hasNext() ) {
            Object value = null;
            String thisParam = paramsItr.next();
            if( context.input.has( thisParam ) )
                value = context.input.get( thisParam );
            else { //This has to be an ADP or a State
                if( context.processedScripts.contains( thisParam ) || id.equals( thisParam ) )  
                    value = getValueFromCurrentState( thisParam, context );
                else if( ScriptsDef.get( "adps" ).has( thisParam ) || ScriptsDef.get( "states" ).has( thisParam ) ) {
                    value = evaluate( thisParam, context );   
                    addToStore( thisParam, value, context );
                    context.processedScripts.add( thisParam );
                } else {
                    value = null;
                }   
            }
            parameterValues[ i ] = thisScript.createObject( thisScript.paramDataTypes.get( i ), value );
            i++;
        }
        Object result = thisScript.evaluate( parameterValues );
        context.processedScripts.add( id );
        return result;
    }

    private Object getValueFromCurrentState( String thisParam, Context context) {
        if( context.entityStates.has( thisParam ) )
            return context.entityStates.get( thisParam );
        if( context.entityADPs.has( thisParam ) )
            return context.entityADPs.get( thisParam );
        return null; 
    }
}