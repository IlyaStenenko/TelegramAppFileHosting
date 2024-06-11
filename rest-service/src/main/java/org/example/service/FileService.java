package org.example.service;

import org.springframework.core.io.FileSystemResource;
import org.example.entity.AppDocument;
import org.example.entity.AppPhoto;

public interface FileService {
    AppDocument getDocument(String id);
    AppPhoto getPhoto(String id);
    FileSystemResource getFileSystemResource(byte[] binaryContent, long id);
}
