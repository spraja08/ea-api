/**
* REST API managing entities
*
* @author  Raja SP
*/

package com.amazonaws.entityanalytics.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class EntitiesAPI extends ServerResource {

    private String id;

    @Override
    protected void doInit() throws ResourceException {
        id = (String) getRequest().getAttributes().get("id");
    }

    @Override
    protected JsonRepresentation post(Representation entity) throws ResourceException {
        JsonObject result = new JsonObject();
        try {
            JsonObject obj = (JsonObject) JsonParser.parseString(entity.getText());
            ScriptsDef.add("entities", id, obj);
            JsonRepresentation rep = new JsonRepresentation(ScriptsDef.get("entities").toString());
            return rep;
        } catch (Exception e) {
            e.printStackTrace();
            result.addProperty("status", "FAILED");
            result.addProperty("reason", e.getMessage());
            JsonRepresentation rep = new JsonRepresentation(result.toString());
            return rep;
        }
    }

    @Override
    protected JsonRepresentation get() throws ResourceException {
        JsonRepresentation rep = null;
        if (id == null)
            rep = new JsonRepresentation(ScriptsDef.get("entities").toString());
        else
            rep = new JsonRepresentation(ScriptsDef.get("entities", id).getAsJsonObject().toString());
        return rep;
    }
}
