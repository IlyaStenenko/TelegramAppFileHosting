package org.example.service.impl;

import lombok.extern.log4j.Log4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.example.dao.AppDocumentDAO;
import org.example.dao.AppPhotoDAO;
import org.example.entity.AppDocument;
import org.example.entity.AppPhoto;
import org.example.service.FileService;
import org.example.utils.CryptoTool;
import java.io.File;
import java.io.IOException;
import java.util.UUID;


@Log4j
@Service
public class FileServiceImpl implements FileService {
    private final AppDocumentDAO appDocumentDAO;
    private final AppPhotoDAO appPhotoDAO;
    private final CryptoTool cryptoTool;

    public FileServiceImpl(AppDocumentDAO appDocumentDAO, AppPhotoDAO appPhotoDAO, CryptoTool cryptoTool) {
        this.appDocumentDAO = appDocumentDAO;
        this.appPhotoDAO = appPhotoDAO;
        this.cryptoTool = cryptoTool;
    }

    @Override
    public AppDocument getDocument(String hash) {
        var id = cryptoTool.idOf(hash);
        if (id == null) {
            return null;
        }
        AppDocument appDocument = appDocumentDAO.findById(id).orElse(null);
        if (appDocument.getDownloads() != null)
            appDocument.setDownloads(appDocument.getDownloads() + 1);
        else appDocument.setDownloads(1L);
        appDocumentDAO.save(appDocument);
        return appDocumentDAO.findById(id).orElse(null);
    }

    @Override
    public AppPhoto getPhoto(String hash) {
        var id = cryptoTool.idOf(hash);
        if (id == null) {
            return null;
        }
        AppPhoto appPhoto = appPhotoDAO.findById(id).orElse(null);
        if (appPhoto.getDownloads() != null)
            appPhoto.setDownloads(appPhoto.getDownloads() + 1);
        else appPhoto.setDownloads(1L);
        appPhotoDAO.save(appPhoto);
        return appPhotoDAO.findById(id).orElse(null);
    }

    @Override
    public FileSystemResource getFileSystemResource(byte[] binaryContent, long id) {
        try {
            AppPhoto appPhoto = appPhotoDAO.findAppPhotoByBinaryContentId(id);
            AppDocument appDocument = appDocumentDAO.findAppDocumentByBinaryContentId(id);
            String uniqueFileName = null;
            if (appDocument == null && appPhoto != null) {
                uniqueFileName = appPhoto.getPhotoName();
            } else if (appPhoto == null && appDocument != null){
                uniqueFileName = appDocument.getDocName();
            }
            if (uniqueFileName == null)
                uniqueFileName = "tempFile_" + UUID.randomUUID() + ".jpeg";

            // Создание временного файла с уникальным именем
            File tempFile = File.createTempFile("tempFile", ".bin");
            tempFile.deleteOnExit();

            // Запись содержимого во временный файл
            FileUtils.writeByteArrayToFile(tempFile, binaryContent);
            System.out.println(uniqueFileName);
            // Переименование временного файла
            File renamedFile = new File(tempFile.getParent(), uniqueFileName);
            if (tempFile.renameTo(renamedFile)) {
                log.info("Файл успешно переименован в " + renamedFile.getName());
                return new FileSystemResource(renamedFile);
            } else {
                log.error("Не удалось переименовать файл");
                return new FileSystemResource(tempFile);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}