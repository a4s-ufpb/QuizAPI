package br.ufpb.dcx.apps4society.quizapi.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MinioBucketInitializer {
    private static final Logger logger = LoggerFactory.getLogger(MinioBucketInitializer.class);

    private final MinioClient minioClient;
    private final String bucket;

    public MinioBucketInitializer(MinioClient minioClient, @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    // Bucket público para leitura (GET anônimo, mais rápido pra servir as
    // imagens), mas toda escrita (PutObject) continua exigindo as credenciais
    // assinadas do MinioClient — nunca é anônima.
    @PostConstruct
    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(publicReadPolicy(bucket))
                    .build());
        } catch (Exception e) {
            logger.warn("Não foi possível inicializar o bucket MinIO '{}': {}", bucket, e.getMessage());
        }
    }

    private String publicReadPolicy(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
    }
}
