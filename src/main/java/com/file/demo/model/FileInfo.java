package com.file.demo.model;

public class FileInfo {
    private Long size;
    private String sourceFileName;
    private String sourcePath;
    private Integer createTime;
    private String documentType;

    public static FileInfo create() {
        return new FileInfo();
    }


    public Long getSize() {
        return size;
    }

    public FileInfo setSize(Long size) {
        this.size = size;
        return this;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public FileInfo setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
        return this;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public FileInfo setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
        return this;
    }

    public Integer getCreateTime() {
        return createTime;
    }

    public FileInfo setCreateTime(Integer createTime) {
        this.createTime = createTime;
        return this;
    }

    public String getDocumentType() {
        return documentType;
    }

    public FileInfo setDocumentType(String documentType) {
        this.documentType = documentType;
        return this;
    }
}
