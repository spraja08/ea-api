
/**
* ElasticSearch API interface
*
* @author  Raja SP
*/

package com.accelerators;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticEntity360Store {
    private static ElasticEntity360Store instance = null;
    private RestHighLevelClient client = null;
    public static String elasticSearchIP = null;
    public static int elasticSearchPort;

    private ElasticEntity360Store() {
        client = new RestHighLevelClient(
            RestClient.builder( new HttpHost(elasticSearchIP, elasticSearchPort, "http")));
            try {
                DeleteIndexRequest deleteRequest = new DeleteIndexRequest("config"); 
                client.indices().delete(deleteRequest, RequestOptions.DEFAULT);
                deleteRequest = new DeleteIndexRequest("entity360"); 
                client.indices().delete(deleteRequest, RequestOptions.DEFAULT);
            }
            catch( Exception e ) {
                e.printStackTrace();
            }
    }

    public static RestHighLevelClient getClient() {
        if( instance == null )
            instance = new ElasticEntity360Store();
        return instance.client;    
    }
}