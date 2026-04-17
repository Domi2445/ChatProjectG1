package Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ImageSaver {

    private final Path saveDirectory;

    public ImageSaver() throws IOException {
        this.saveDirectory = getDownloadsFolder().resolve("ChatImages");
        Files.createDirectories(saveDirectory);
    }

    public ImageSaver(Path saveDirectory) throws IOException {
        this.saveDirectory = saveDirectory;
        Files.createDirectories(saveDirectory);
    }

    public Path saveImage(byte[] imageBytes, String originalFileName) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty.");
        }

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or blank.");
        }

        String extension = getExtension(originalFileName);
        String uuid = UUID.randomUUID().toString();
        Path filePath = saveDirectory.resolve(uuid + extension);
        Files.write(filePath, imageBytes);
        return filePath;
    }

    private Path getDownloadsFolder() {
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, "Downloads");
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return fileName.substring(dotIndex).toLowerCase();
    }
}