package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.service.exception.ImageStorageException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.UUID;

@Service
public class ImageStorageService {
    private static final String OBJECT_PREFIX = "questions/";

    private final MinioClient minioClient;
    private final String bucket;
    private final String publicUrl;

    public ImageStorageService(MinioClient minioClient,
                                @Value("${minio.bucket}") String bucket,
                                @Value("${minio.public-url}") String publicUrl) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.publicUrl = publicUrl;
    }

    /**
     * Recebe uma imagem em base64 (upload novo) ou uma URL já armazenada no
     * MinIO (edição sem trocar a imagem) e devolve sempre uma URL pública.
     * Converte imagens novas para webp antes de subir (mais leve pra web).
     */
    public String upload(String imageBase64OrUrl) {
        if (imageBase64OrUrl == null || imageBase64OrUrl.isBlank()) {
            return null;
        }
        if (imageBase64OrUrl.startsWith("http://") || imageBase64OrUrl.startsWith("https://")) {
            return imageBase64OrUrl;
        }

        byte[] webpBytes = toWebp(imageBase64OrUrl);
        String objectKey = OBJECT_PREFIX + UUID.randomUUID() + ".webp";

        try (ByteArrayInputStream input = new ByteArrayInputStream(webpBytes)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(input, webpBytes.length, -1)
                    .contentType("image/webp")
                    .build());
        } catch (Exception e) {
            throw new ImageStorageException("Falha ao enviar imagem para o MinIO", e);
        }

        return publicUrl + "/" + bucket + "/" + objectKey;
    }

    private byte[] toWebp(String base64Image) {
        String data = base64Image;
        int commaIndex = data.indexOf(',');
        if (data.startsWith("data:") && commaIndex != -1) {
            data = data.substring(commaIndex + 1);
        }

        byte[] decoded = Base64.getDecoder().decode(data);

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(decoded));
            if (image == null) {
                throw new ImageStorageException("Formato de imagem inválido", null);
            }

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
            if (!writers.hasNext()) {
                throw new ImageStorageException("Nenhum encoder webp disponível", null);
            }
            ImageWriter writer = writers.next();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(output)) {
                writer.setOutput(imageOutput);
                ImageWriteParam params = writer.getDefaultWriteParam();
                writer.write(null, new IIOImage(image, null, null), params);
            } finally {
                writer.dispose();
            }

            return output.toByteArray();
        } catch (IOException e) {
            throw new ImageStorageException("Falha ao converter imagem para webp", e);
        }
    }
}
