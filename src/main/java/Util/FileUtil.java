package Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

public final class FileUtil {
	private static final String[] IMAGE_EXTENSIONS = { "png", "jpg", "jpeg", "gif", "bmp" };
	private static final String DIRECTORY_NAME = "ChatProjektG1-Bilder";

	private FileUtil() {
		throw new UnsupportedOperationException();
	}

	public static UUID saveFile(byte[] fileBytes, String extension, boolean isImage) throws IOException {
		if (fileBytes == null || fileBytes.length == 0) {
			throw new IllegalArgumentException("Datei darf nicht null oder leer sein");
		}

		UUID uuid = UUID.randomUUID();
		String fileName = isImage ? uuid + "." + normalizeImageExtension(extension) : uuid.toString();
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

	private static String normalizeImageExtension(String extension) {
		if (extension == null) {
			throw new IllegalArgumentException("Bild-Erweiterung darf nicht null sein");
		}

		String normalized = extension.trim().toLowerCase();
		if (normalized.startsWith(".")) {
			normalized = normalized.substring(1);
		}

		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Bild-Erweiterung darf nicht leer sein");
		}

		String extensionToCheck = normalized;
		if (Arrays.stream(IMAGE_EXTENSIONS).noneMatch(ext -> ext.equals(extensionToCheck))) {
			throw new IllegalArgumentException("Ungültige Bilderweiterung: " + extension);
		}

		return normalized;
	}
}
