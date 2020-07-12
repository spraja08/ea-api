/**
* Holds the context for the expression evaluation
*
* @author  Raja SP
*/

package com.amazonaws.entityanalytics.api;

import java.util.HashSet;

import com.google.gson.JsonObject;

public class Context {
    public JsonObject entityADPs;
    public JsonObject input;
    public JsonObject entityStates;
    public HashSet< String >processedScripts;

    public Context( JsonObject entityADPs, JsonObject input, JsonObject entityStates ) {
        this.entityADPs = entityADPs;
        this.input = input;
        this.entityStates = entityStates;
        processedScripts = new HashSet< String >();
    }
}