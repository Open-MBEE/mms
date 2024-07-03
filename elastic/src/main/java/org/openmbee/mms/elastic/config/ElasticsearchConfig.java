package org.openmbee.mms.elastic.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host}")
    private String elasticsearchHost;
    @Value("${elasticsearch.port}")
    private int elasticsearchPort;
    @Value("${elasticsearch.http:http}")
    private String elasticsearchHttp;

    @Value("${elasticsearch.password:#{null}}")
    private String elasticsearchPassword;
    @Value("${elasticsearch.username:#{null}}")
    private String elasticsearchUsername;

    @Value("${elasticsearch.truststore:#{null}}")
    private String elasticsearchTruststore;

    @Value("${elasticsearch.storepass:#{null}}")
    private String elasticStorepass;

    private static Logger logger = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Bean(name = "clientElastic", destroyMethod = "close")
    public RestHighLevelClient restClient() {

        RestClientBuilder builder = RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort, elasticsearchHttp));
        builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(10000).setSocketTimeout(1000000));
        
        

        builder.setHttpClientConfigCallback(new HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(
                    HttpAsyncClientBuilder httpClientBuilder) {
                if (elasticsearchPassword != null && elasticsearchUsername != null && !elasticsearchPassword.isEmpty() && !elasticsearchUsername.isEmpty()) {
                    final CredentialsProvider credentialsProvider =new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }
                if (elasticsearchHttp != null &&  elasticsearchHttp == "https" && elasticsearchTruststore != null && elasticStorepass != null) {  
                    try {
                        // SSLFactory sslFactory = SSLFactory.builder()
                        // .withDefaultTrustMaterial() // JDK trusted CA's
                        // .withSystemTrustMaterial()  // OS trusted CA's
                        // .withTrustMaterial(trustStorePath, password)
                        // .build();
                        // SSLContextBuilder sslBuilder = SSLContexts.custom().loadTrustMaterial(new File(elasticsearchTruststore), elasticStorepass.toCharArray());
                        // SSLContext sslContext = sslBuilder.build();
                        // httpClientBuilder.setSSLContext(sslContext);
                    } catch (Exception e ){
                        logger.debug("Error unable to load ssl truststore: " + e.getMessage());
                        return httpClientBuilder;
                    }   
                }
                return httpClientBuilder;
            }
        });
        

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }

}