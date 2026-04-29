package Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

public final class FileUtil {
	public static final String[] IMAGE_EXTENSIONS = { "png", "jpg", "jpeg", "gif", "bmp" };
	private static final String DIRECTORY_NAME = "ChatProjektG1-Bilder";

	private FileUtil() {
		throw new UnsupportedOperationException();
	}

	public static UUID saveFile(byte[] fileBytes, String extension) throws IOException {
		if (fileBytes == null || fileBytes.length == 0) {
			throw new IllegalArgumentException("Datei darf nicht null oder leer sein");
		}

		String normalizedExtension = normalizeExtension(extension);
		boolean isImage = isImageExtension(normalizedExtension);

		UUID uuid = UUID.randomUUID();
		String fileName = isImage ? uuid + "." + normalizedExtension : uuid.toString();
		Path filePath = getDirectory().resolve(fileName);

		if (Files.exists(filePath)) {
			throw new IOException("Datei existiert bereits: " + filePath);
		}

		Files.write(filePath, fileBytes);
		return uuid;
	}

	public static FileInputStream openFile(UUID fileId, boolean isImage) throws IOException {
		if (fileId == null) {
			throw new IllegalArgumentException("Datei-ID darf nicht null sein");
		}

		Path filePath = isImage ? getImagePath(fileId) : getFilePath(fileId);
		File file = filePath.toFile();
		if (!file.isFile() || !file.canRead()) {
			throw new IOException("Datei kann nicht gelesen werden: " + filePath);
		}

		return new FileInputStream(file);
	}

	public static String getFileExtension(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			throw new IllegalArgumentException("Dateiname darf nicht null oder leer sein");
		}

		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
			throw new IllegalArgumentException("Dateiname hat keine gültige Erweiterung: " + fileName);
		}

		return fileName.substring(lastDotIndex + 1);
	}

	private static Path getDirectory() throws IOException {
		String temp = System.getProperty("java.io.tmpdir");
		if (temp == null) {
			throw new IllegalStateException("Temp-Verzeichnis nicht gefunden");
		}

		Path tempPath = Path.of(temp);
		if (!Files.isDirectory(tempPath)) {
			throw new IllegalStateException("Temp-Pfad ist kein Verzeichnis: " + temp);
		}

		Path dirPath = tempPath.resolve(DIRECTORY_NAME);
		if (!Files.exists(dirPath)) {
			try {
				Files.createDirectories(dirPath);
			} catch (IOException e) {
				throw new IOException("Verzeichnis konnte nicht erstellt werden: " + dirPath, e);
			}
		} else if (!Files.isDirectory(dirPath)) {
			throw new IllegalStateException("Pfad ist kein Verzeichnis: " + dirPath);
		}

		return dirPath;
	}

	private static Path getFilePath(UUID fileId) throws IOException {
		Path filePath = getDirectory().resolve(fileId.toString());
		if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
			throw new IOException("Datei existiert nicht: " + filePath);
		}
		return filePath;
	}

	private static Path getImagePath(UUID fileId) throws IOException {
		Path dirPath = getDirectory();
		for (String ext : IMAGE_EXTENSIONS) {
			Path filePath = dirPath.resolve(fileId + "." + ext);
			if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
				return filePath;
			}
		}
		throw new IOException("Bilddatei existiert nicht: " + fileId);
	}

	private static String normalizeExtension(String extension) {
		if (extension == null) {
			throw new IllegalArgumentException("Datei-Erweiterung darf nicht null sein");
		}

		String normalized = extension.trim().toLowerCase();
		if (normalized.startsWith(".")) {
			normalized = normalized.substring(1);
		}

		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Datei-Erweiterung darf nicht leer sein");
		}

		return normalized;
	}

	public static boolean isImageExtension(String extension) {
		return Arrays.asList(IMAGE_EXTENSIONS).contains(extension);
	}
}
