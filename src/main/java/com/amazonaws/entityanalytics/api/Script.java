/**
* Represents a 'cooked' snippet
*
* @author  Raja SP
*/

package com.amazonaws.entityanalytics.api;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import org.antlr.v4.runtime.*;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.codehaus.janino.ScriptEvaluator;

public class Script {
    public String id;
    public String script;
    public List<String> paramNames;
    public List<Class> paramDataTypes;
    public List<String> paramTypes;
    public String scriptOrExp;
    public String returnType;
    public Class returnTypeClass;

    public ScriptEvaluator se;
    public ExpressionEvaluator ee;

    public void initalise() throws ClassNotFoundException, CompileException {
        org.antlr.v4.runtime.CharStream cs = new ANTLRInputStream(script);
        JavaLexer jl = new JavaLexer(cs);
        CommonTokenStream tokens = new CommonTokenStream(jl);
        JavaParser jp = new JavaParser(tokens);
        ParserRuleContext t = jp.compilationUnit();
        String st = t.toStringTree(jp);
        st.replace("(compilationUnit", "");
        st = st.substring(0, st.length() - 1);
        StringTokenizer tk = new StringTokenizer(st);
        while (tk.hasMoreTokens()) {
            String token = tk.nextToken();
            if (token.startsWith("\"") && token.endsWith("\""))
                continue;

            JsonObject match = null;
            String expressionType = null;
            String scriptType = null;
            if (ScriptsDef.get( "adps" ).has(token)) {
                match = ScriptsDef.get( "adps" ).get(token).getAsJsonObject();
                scriptType = "ADP";
                expressionType = match.get("expressionType").getAsString();
            } else if (ScriptsDef.get( "states" ).has(token)) {
                match = ScriptsDef.get( "states" ).get(token).getAsJsonObject();
                scriptType = "Segment";
                expressionType = "boolean";
            }
            if (match == null) {
                Set<String> keys = ScriptsDef.get( "events" ).keySet();
                Iterator<String> itr = keys.iterator();
                while (itr.hasNext()) {
                    String key = itr.next();
                    JsonObject input = ScriptsDef.get( "events" ).get(key).getAsJsonObject();
                    if (input.has(token)) {
                        match = input;
                        scriptType = "Input";
                        expressionType = input.get(token).getAsString();
                    }
                }
            }
            if (match == null)
                continue;

            if (paramNames.contains(token))
                continue;

            paramNames.add(token);
            paramTypes.add(scriptType);
            paramDataTypes.add(stringToType(expressionType));
        }
        String[] paramNamesArray = new String[paramNames.size()];
        paramNamesArray = paramNames.toArray(paramNamesArray);

        Class[] paramDataTypesArray = new Class[paramDataTypes.size()];
        paramDataTypesArray = paramDataTypes.toArray(paramDataTypesArray);

        // determine if this is an expression or a snippet
        // cook and keep
        System.out.println( "Compiling Script : " + this.toString() );
        if (script.split(";").length > 1) {
            se = new ScriptEvaluator();
            se.setDefaultImports( new String[] { "com.google.gson.*", "com.amazonaws.entityanalytics.api.*" } );
            se.setParameters(paramNamesArray, paramDataTypesArray);
            se.setReturnType(returnTypeClass);
            se.cook(this.script);
            scriptOrExp = "script";
        } else {
            ee = new ExpressionEvaluator();
            ee.setDefaultImports( new String[] { "com.google.gson.*", "com.amazonaws.entityanalytics.api.*" } );
            ee.setParameters(paramNamesArray, paramDataTypesArray);
            ee.setExpressionType(returnTypeClass);
            ee.cook(this.script);
            scriptOrExp = "expression";
        }
    }

    public Script(String id, String script, String returnType) {
        this.id = id;
        this.script = script;
        this.returnType = returnType;
        this.returnTypeClass = stringToType( returnType );
        paramNames = new ArrayList<String>();
        paramDataTypes = new ArrayList<Class>();
        paramTypes = new ArrayList<String>();
        ee = null;
        se = null;
    }

    private Class stringToType(String s) {
        if (s.equals("double"))
            return double.class;
        else if (s.equals("boolean"))
            return boolean.class;
        else if (s.equals("long"))
            return long.class;
        else if (s.equals("int"))
            return int.class;
        else if (s.equals("void"))
            return void.class;
        else if (s.equals("char"))
            return char.class;
        else if (s.equals("byte"))
            return byte.class;
        else if (s.equals("short"))
            return short.class;
        else if (s.equals("float"))
            return float.class;
        else if (s.equals("double[]"))
            return double[].class;
        else if (s.contains("String[]"))
            return new String[0].getClass();
        else if (s.contains("java.util.HashMap"))
            return new HashMap().getClass();
        else if (s.contains("JsonArray"))
            return new com.google.gson.JsonArray().getClass();
        else if (s.contains("String"))
            return new String().getClass();
        else if (s.contains("JsonObject"))
            return new com.google.gson.JsonObject().getClass();
        else if (s.equals("Integer"))
            return Integer.class;
        else if (s.equals("Long"))
            return Long.class;
        else if (s.equals("Double"))
            return Double.class;
        else if (s.equals("Boolean"))
            return Boolean.class;
        else if (s.equals("Float"))
            return Float.class;
        else if (s.equals("Double[]"))
            return Double[].class;
        else if (s.equals("Integer[]"))
            return Integer[].class;
        else if (s.equals("Long[]"))
            return Long[].class;

        try {
           return Class.forName( s );
        }catch( ClassNotFoundException ex ) {
            ex.printStackTrace();
        }
        System.out.println( "Not able to find match stringToType. Returning Null");
        return null;
    }

    public Object createObject(Class type, Object jvalue) {
        if (type.isPrimitive()) {
            type = (type == double.class ? Double.class
                    : type == boolean.class ? Boolean.class
                            : type == long.class ? Long.class
                                    : type == int.class ? Integer.class
                                            : type == byte.class ? Byte.class
                                                    : type == short.class ? Short.class
                                                            : type == float.class ? Float.class
                                                                    : type == char.class ? Character.class
                                                                            : void.class);
        }
        if (type == String.class) {
            if (jvalue == null) {
                return new String();
            } else {
                if (jvalue.getClass() == String.class)
                    return (String) jvalue;

                return ((JsonElement) jvalue).getAsString();
            }
        } else if (type == Double.class) {
            if (jvalue == null) {
                return new Double(0);
            } else {
                if (jvalue.getClass() == Double.class)
                    return (Double) jvalue;
                return ((JsonElement) jvalue).getAsDouble();
            }
        } else if (type == Long.class) {
            if (jvalue == null) {
                return new Long(0);
            } else {
                if (jvalue.getClass() == Long.class)
                    return (Long) jvalue;
                return ((JsonElement) jvalue).getAsLong();
            }
        } else if (type == Integer.class) {
            if (jvalue == null) {
                return new Integer(0);
            } else {
                if (jvalue.getClass() == Integer.class)
                    return (Integer) jvalue;
                return ((JsonElement) jvalue).getAsInt();
            }
        } else if (type == Float.class) {
            if (jvalue == null) {
                return new Float(0);
            } else {
                if (jvalue.getClass() == Float.class)
                    return (Float) jvalue;
                return ((JsonElement) jvalue).getAsFloat();
            }
        } else if (type == Boolean.class) {
            if (jvalue == null) {
                return new Boolean(false);
            } else {
                if (jvalue.getClass() == Boolean.class)
                    return (Boolean) jvalue;
                return ((JsonElement) jvalue).getAsBoolean();
            }
        } else if (type == JsonArray.class) {
            if (jvalue == null) {
                return new JsonArray();
            } else {
                if (jvalue.getClass() == JsonArray.class)
                    return (JsonArray) jvalue;
                return ((JsonElement) jvalue).getAsJsonArray();
            }
        } else if (type == HashMap.class)
            return (HashMap<String, JsonObject>) jvalue;
        else if (type == JsonObject.class) {
            if (jvalue == null) {
                return new JsonObject();
            } else {
                return (JsonObject) jvalue;
            }
        } else if (type == double[].class) {
            if (jvalue == null) {
                return new double[] {};
            } else {
                if (jvalue.getClass() == double[].class)
                    return (double[]) jvalue;

                JsonArray jArr = ((JsonElement) jvalue).getAsJsonArray();
                double[] res = new double[jArr.size()];
                for (int i = 0; i < jArr.size(); i++) {
                    res[i] = jArr.get(i).getAsDouble();
                }
                return res;
            }
        } else if (type == String[].class) {
            if (jvalue == null) {
                return new String[] {};
            } else {
                if (jvalue.getClass() == String[].class)
                    return (String[]) jvalue;

                JsonArray jArr = ((JsonElement) jvalue).getAsJsonArray();
                String[] res = new String[jArr.size()];
                for (int i = 0; i < jArr.size(); i++) {
                    res[i] = jArr.get(i).getAsString();
                }
                return res;
            }
        }
        return null;
    }

    public Object evaluate(Object[] values) throws InvocationTargetException {
        if( scriptOrExp.equals( "script" ) ) 
            return se.evaluate( values );
		return ee.evaluate( values );
	}

    @Override
    public String toString() {
        return "Script [ee=" + ee + ", id=" + id + ", paramDataTypes=" + paramDataTypes + ", paramNames=" + paramNames
                + ", paramTypes=" + paramTypes + ", returnType=" + returnType + ", returnTypeClass=" + returnTypeClass
                + ", script=" + script + ", scriptOrExp=" + scriptOrExp + ", se=" + se + "]";
    }
}