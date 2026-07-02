package br.ufpb.dcx.apps4society.quizapi.util;

public class ImageValidator {
    public static final int MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024;
    public static final int MAX_TOTAL_IMAGES_SIZE_BYTES = 4 * 1024 * 1024;

    private ImageValidator() {
    }

    public static int decodedSizeInBytes(String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            return 0;
        }

        String data = base64Image;
        int commaIndex = data.indexOf(',');
        if (data.startsWith("data:") && commaIndex != -1) {
            data = data.substring(commaIndex + 1);
        }

        int padding = 0;
        if (data.endsWith("==")) {
            padding = 2;
        } else if (data.endsWith("=")) {
            padding = 1;
        }

        return (data.length() * 3) / 4 - padding;
    }
}
