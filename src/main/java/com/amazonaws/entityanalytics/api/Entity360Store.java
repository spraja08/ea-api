/**
* Internal Cache for holding entity 360s
*
* @author  Raja SP
*/

package com.amazonaws.entityanalytics.api;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentType;

public class Entity360Store {
    private static Entity360Store instance = null;
    private Map<String, JsonObject> EntityADPsMap;
    private Map<String, JsonObject> EntityStatesMap;

    public JsonObject dumpAll() {
        JsonObject result = new JsonObject();
        Set<String> keys = EntityADPsMap.keySet();
        Iterator<String> itr = keys.iterator();
        while( itr.hasNext() ) {
            String thiskey = itr.next();
            JsonObject thisEntity = new JsonObject();
            thisEntity.add( "ADPs", EntityADPsMap.get( thiskey ).getAsJsonObject() );
            thisEntity.add( "states", EntityStatesMap.get( thiskey ).getAsJsonObject() );
            result.add( thiskey, thisEntity );
        }
        return result;
    }

    private Entity360Store() {
        EntityADPsMap = new java.util.HashMap<String, JsonObject>();
        EntityStatesMap = new java.util.HashMap<String, JsonObject>();
    }

    public static Entity360Store getInstance() {
        if (instance == null)
            instance = new Entity360Store();
        return instance;
    }

    public JsonObject getEntityADPs(String entityId) {
        JsonObject result = getInstance().EntityADPsMap.get(entityId);
        if (result == null)
            result = new JsonObject();
        return result;
    }

    public JsonObject getEntityStates(String entityId) {
        JsonObject result = getInstance().EntityStatesMap.get(entityId);
        if (result == null)
            result = new JsonObject();
        return result;
    }

    public void setEntityADPs(String entityId, JsonObject entityADPs) {
        getInstance().EntityADPsMap.put(entityId, entityADPs);
    }

    public void setEntityStates(String entityId, JsonObject entityStates) {
        getInstance().EntityStatesMap.put(entityId, entityStates);
    }

    public void setEntity360(String entityId, JsonObject entityADPs, JsonObject entityStates) {
        setEntityADPs(entityId, entityADPs);
        setEntityStates(entityId, entityStates);
        JsonObject result = new JsonObject();
        result.add("ADPs", entityADPs);
        result.add("states", entityStates);
        IndexRequest request = new IndexRequest("entity360");
        request.id(entityId);
        request.source(result.toString(), XContentType.JSON);
        try {
            ElasticEntity360Store.getClient().index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}