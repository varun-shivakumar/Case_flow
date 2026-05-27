package com.caseflow.appeals.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * File storage helper — appeal-service writes attached documents to a
 * dedicated subdirectory under the shared caseflow-uploads root so case
 * documents and appeal documents stay logically separated on disk.
 */
public final class FileStorageUtil {

    private FileStorageUtil() {}

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final String[] ALLOWED_EXTENSIONS = {
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "jpg", "jpeg", "png", "gif", "zip", "rar"
    };
    private static final String UPLOAD_DIR = "./caseflow-uploads/appeals";

    public static String saveFile(MultipartFile file) throws IOException {
        validateFile(file);

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileExtension = getFileExtension(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID() + "." + fileExtension;

        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.write(filePath, file.getBytes());

        return filePath.toString();
    }

    public static byte[] readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found at path: " + filePath);
        }
        return Files.readAllBytes(path);
    }

    public static void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            // best-effort: even if disk delete fails, the metadata row is gone
        }
    }

    public static String generateFileUrl(Long documentId) {
        return "/api/appeals/documents/" + documentId + "/download";
    }

    public static void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File is empty or null");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum limit of 10MB");
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isEmpty()) {
            throw new IOException("Invalid file name");
        }
        String ext = getFileExtension(name).toLowerCase();
        if (!isAllowedExtension(ext)) {
            throw new IOException("File type '" + ext + "' is not allowed");
        }
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    public static boolean isAllowedExtension(String ext) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(ext)) return true;
        }
        return false;
    }

    public static String getMimeType(String fileName) {
        return switch (getFileExtension(fileName).toLowerCase()) {
            case "pdf"           -> "application/pdf";
            case "doc"           -> "application/msword";
            case "docx"          -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls"           -> "application/vnd.ms-excel";
            case "xlsx"          -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt"           -> "application/vnd.ms-powerpoint";
            case "pptx"          -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt"           -> "text/plain";
            case "jpg", "jpeg"   -> "image/jpeg";
            case "png"           -> "image/png";
            case "gif"           -> "image/gif";
            case "zip"           -> "application/zip";
            case "rar"           -> "application/x-rar-compressed";
            default              -> "application/octet-stream";
        };
    }
}
