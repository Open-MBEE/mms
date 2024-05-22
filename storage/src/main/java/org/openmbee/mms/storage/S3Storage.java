package org.openmbee.mms.storage;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.json.ElementJson;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(S3StorageCondition.class)
public class S3Storage extends AbstractArtifactStorage {

    private AmazonS3 s3Client;

    @Value("${s3.endpoint}")
    private String ENDPOINT;

    @Value("${s3.access_key:#{null}}")
    private Optional<String> ACCESS_KEY;

    @Value("${s3.secret_key:#{null}}")
    private Optional<String> SECRET_KEY;

    @Value("${s3.region}")
    private String REGION;

    @Value("${s3.bucket:#{null}}")
    private Optional<String> BUCKET;


    private AmazonS3 getClient() {
        if (s3Client == null) {
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.setSignerOverride("AWSS3V4SignerType");

            AmazonS3ClientBuilder builder = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(ENDPOINT, REGION))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfiguration);

            if (ACCESS_KEY.isPresent() && SECRET_KEY.isPresent()) {
                AWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY.get(), SECRET_KEY.get());
                s3Client = builder.withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
            } else {
                s3Client = builder.withCredentials(DefaultAWSCredentialsProviderChain.getInstance()).build();
            }
            if (!s3Client.doesBucketExistV2(getBucket())) {
                try {
                    s3Client.createBucket(getBucket());
                } catch (AmazonS3Exception e) {
                    throw new InternalErrorException(e);
                }
            }
        }

        return s3Client;
    }

    @Override
    public byte[] get(String location, ElementJson element, String mimetype) {
        GetObjectRequest rangeObjectRequest = new GetObjectRequest(getBucket(), location);
        try {
            return getClient().getObject(rangeObjectRequest).getObjectContent().readAllBytes();
        } catch (IOException ioe) {
            throw new NotFoundException(ioe);
        }
    }

    @Override
    public String store(byte[] data, ElementJson element, String mimetype) {
        String location = buildLocation(element, mimetype);
        ObjectMetadata om = new ObjectMetadata();
        om.setContentType(mimetype);
        om.setContentLength(data.length);

        PutObjectRequest por = new PutObjectRequest(getBucket(), location, new ByteArrayInputStream(data), om);

        try {
            getClient().putObject(por);
        } catch (RuntimeException e) {
            logger.error("Error storing artifact: ", e);
            throw new InternalErrorException(e);
        }
        return location;
    }

    private String getBucket() {
        String bucket = "mms";
        if (BUCKET.isPresent()) {
            bucket = BUCKET.get();
        }
        return bucket;
    }

    
}
