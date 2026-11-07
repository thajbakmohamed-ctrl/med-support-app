
package com.medsupport.identity_service.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public final class FileValidationUtil {

    private FileValidationUtil() {
    }

    public static String validateFile(
            MultipartFile file,
            boolean allowPdf) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File is required");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "Maximum file size is 5 MB");
        }

        byte[] header = new byte[8];

        int bytesRead;
        try (InputStream input = file.getInputStream()) {
            bytesRead = input.readNBytes(
                    header, 0, header.length);
        }

        boolean isPdf = bytesRead >= 5
                && header[0] == '%'
                && header[1] == 'P'
                && header[2] == 'D'
                && header[3] == 'F'
                && header[4] == '-';

        boolean isJpeg = bytesRead >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;

        boolean isPng = bytesRead >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 'P'
                && header[2] == 'N'
                && header[3] == 'G'
                && (header[4] & 0xFF) == 0x0D
                && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A
                && (header[7] & 0xFF) == 0x0A;

        String contentType = file.getContentType();

        if (allowPdf
                && isPdf
                && "application/pdf".equals(contentType)) {
            return ".pdf";
        }

        if (isJpeg
                && "image/jpeg".equals(contentType)) {
            return ".jpg";
        }

        if (isPng
                && "image/png".equals(contentType)) {
            return ".png";
        }

        throw new IllegalArgumentException(
                "Invalid or unsupported file content");
    }
}
