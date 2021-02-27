/**
* Holds the definitions of all building blocks and snippets 
*
* @author  Raja SP
*/

package com.amazonaws.entityanalytics.api;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.codehaus.commons.compiler.CompileException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentType;

public class ScriptsDef {

    public static Map<String, Script> scriptsMap;

    private static JsonObject adps;
    private static JsonObject states;
    private static JsonObject triggers;
    private static JsonObject events;
    private static JsonObject entities;
    private static Map< String, Map< String, Set< String > > > entityMappedToADPs;

    public static void initalise( String resourcePath ) throws IOException, CompileException {

        String content = readFile( resourcePath + "/ADPsDef.json");
        adps = (JsonObject) JsonParser.parseString(content);

        content = readFile(resourcePath + "/StatesDef.json");
        if( content.trim().length() > 0 )
            states = (JsonObject) JsonParser.parseString(content);
        else states = new JsonObject();    

        content = readFile(resourcePath + "/TriggersDef.json");
        if( content.trim().length() > 0 )
            triggers = (JsonObject) JsonParser.parseString(content);
        else triggers = new JsonObject();   

        content = readFile( resourcePath + "/InputsDef.json");
        events = (JsonObject) JsonParser.parseString(content);

        content = readFile( resourcePath + "/EntitiesDef.json");
        entities = (JsonObject) JsonParser.parseString(content);

        IndexRequest request = new IndexRequest( "config" );
        request.id( "adps" );
        request.source( adps.toString(), XContentType.JSON );
        IndexResponse indexResponse = ElasticEntity360Store.getClient().index(request, RequestOptions.DEFAULT);

        request.id( "states" );
        request.source( states.toString(), XContentType.JSON );
        indexResponse = ElasticEntity360Store.getClient().index(request, RequestOptions.DEFAULT);

        request.id( "triggers" );
        request.source( triggers.toString(), XContentType.JSON );
        indexResponse = ElasticEntity360Store.getClient().index(request, RequestOptions.DEFAULT);

        request.id( "events" );
        request.source( events.toString(), XContentType.JSON );
        indexResponse = ElasticEntity360Store.getClient().index(request, RequestOptions.DEFAULT);

        request.id( "entities" );
        request.source( entities.toString(), XContentType.JSON );
        indexResponse = ElasticEntity360Store.getClient().index(request, RequestOptions.DEFAULT);

        compileAllExpressions();
    }

    public static JsonObject get( String buildingBlock ) {
        if( buildingBlock.equals( "adps" ) )
            return adps;
        else if( buildingBlock.equals( "states" ) )
            return states;    
        else if( buildingBlock.equals( "triggers" ) )
            return triggers;  
        else if( buildingBlock.equals( "events" ) )
            return events;  
        else if( buildingBlock.equals( "entities" ) )
            return entities;  
        return null;    
    }

    public static JsonObject get( String buildingBlock, String id ) {
        if( buildingBlock.equals( "adps" ) )
            return adps.get( id ).getAsJsonObject();
        else if( buildingBlock.equals( "states" ) )
            return states.get( id ).getAsJsonObject();  
        else if( buildingBlock.equals( "triggers" ) )
            return triggers.get( id ).getAsJsonObject();  
        else if( buildingBlock.equals( "events" ) )
            return events.get( id ).getAsJsonObject();
        else if( buildingBlock.equals( "entities" ) )
            return entities.get( id ).getAsJsonObject();  
        return null; 
    }

    public static void add( String buildingBlock, String id, JsonObject value ) throws IOException {
        JsonObject buildingBlocks = get( buildingBlock );
        buildingBlocks.add( id, value );
        IndexRequest request = new IndexRequest( "config" );
        request.id( buildingBlock );
        request.source( buildingBlocks.toString(), XContentType.JSON );
        ElasticEntity360Store.getClient().index(request, RequestOptions.DEFAULT);
    }

    public static void compileAllExpressions() throws CompileException, IOException {
        scriptsMap = new HashMap< String, Script >();
        JsonObject adps = get("adps");
        Set<String> keys = adps.keySet();
        Iterator<String> itr = keys.iterator();
        while (itr.hasNext()) {
            String key = itr.next();
            System.out.println( "compiling : " + key );
            JsonObject jObj = adps.get(key).getAsJsonObject();
            Script script = new Script( key, jObj.get("expression").getAsString(), jObj.get( "expressionType" ).getAsString() );
            try {
                script.initalise();
                scriptsMap.put( key, script );
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        JsonObject states = get("states");
        keys = states.keySet();
        itr = keys.iterator();
        while (itr.hasNext()) {
            String key = itr.next();
            JsonObject jObj = states.get(key).getAsJsonObject();
            Script script = new Script( key, jObj.get("expression").getAsString(), "boolean" );
            try {
                script.initalise();
                scriptsMap.put( key, script );
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        JsonObject triggers = get("triggers");
        keys = states.keySet();
        itr = keys.iterator();
        while (itr.hasNext()) {
            String key = itr.next();
            JsonObject jObj = triggers.get(key).getAsJsonObject();
            Script script = new Script( key, jObj.get("expression").getAsString(), "boolean" );
            try {
                script.initalise();
                scriptsMap.put( key, script );
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static String readFile( String fileName ) throws IOException {
        InputStream is = new FileInputStream( fileName );
        BufferedReader buf = new BufferedReader( new InputStreamReader( is ) );
                
        String line = buf.readLine();
        StringBuilder sb = new StringBuilder();
                
        while( line != null ){
           sb.append( line ).append( "\n" );
           line = buf.readLine();
        }
        buf.close();
        String fileAsString = sb.toString();
        return fileAsString;
    }

}