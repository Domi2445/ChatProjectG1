package Util;

public class FileMessage extends Message {
    private final byte[] content;
    private final String fileName;
    private final FileType fileType;

    public FileMessage(User sender, byte[] content, String fileName, FileType fileType) {
        super(sender);
        this.content = content;
        this.fileName = fileName;
        this.fileType = fileType;
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    @Override
    public String getDisplayText() {
        return "";
    }

    @Override
    public void send() {

    }

    public String getFileName() {
        return fileName;
    }

    public FileType getFileType() {
        return fileType;
    }

    public enum FileType {
        FILE,
        IMAGE,
    }
}
