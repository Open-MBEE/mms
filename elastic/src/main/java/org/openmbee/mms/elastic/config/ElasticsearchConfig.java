package org.openmbee.mms.elastic.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host}")
    private String elasticsearchHost;
    @Value("${elasticsearch.port}")
    private int elasticsearchPort;
    @Value("${elasticsearch.http}")
    private String elasticsearchHttp;

    @Value("${elasticsearch.password:#{null}}")
    private String elasticsearchPassword;
    @Value("${elasticsearch.username:#{null}}")
    private String elasticsearchUsername;

    @Bean(name = "clientElastic", destroyMethod = "close")
    public RestHighLevelClient restClient() {



        RestClientBuilder builder = RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort, elasticsearchHttp));
        builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(10000).setSocketTimeout(1000000));

        if (elasticsearchPassword != null && elasticsearchUsername != null && !elasticsearchPassword.isEmpty() && !elasticsearchUsername.isEmpty()) {
            final CredentialsProvider credentialsProvider =
                new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword));
            builder.setHttpClientConfigCallback(new HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(
                    HttpAsyncClientBuilder httpClientBuilder) {
                return httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider);
            }
        });
        }
        

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }

}