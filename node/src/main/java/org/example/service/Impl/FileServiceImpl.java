package org.example.service.Impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.example.dao.AppDocumentDAO;
import org.example.dao.AppPhotoDAO;
import org.example.dao.BinaryContentDAO;
import org.example.entity.AppDocument;
import org.example.entity.AppPhoto;
import org.example.entity.BinaryContent;
import org.example.exceptions.UploadFileException;
import org.example.service.FileService;
//import org.example.service.enums.LinkType;
//import org.example.utils.CryptoTool;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

    @Log4j
    @Service
    public class FileServiceImpl implements FileService {

        @Value("${token}")
        private String token;

        @Value("${service.file_info.uri}")
        private String fileInfoUri;

        @Value("${service.file_storage.uri}")
        private String fileStorageUri;

        //@Value("${link.address}")
        //private String linkAddress;

        private final AppDocumentDAO appDocumentDAO;

        private final AppPhotoDAO appPhotoDAO;

        private final BinaryContentDAO binaryContentDAO;

        public FileServiceImpl(AppDocumentDAO appDocumentDAO, AppPhotoDAO appPhotoDAO, BinaryContentDAO binaryContentDAO){
            this.appDocumentDAO = appDocumentDAO;
            this.appPhotoDAO = appPhotoDAO;
            this.binaryContentDAO = binaryContentDAO;
        }

        //private final CryptoTool cryptoTool;
        @Override
        public AppDocument processDoc(Message telegramMessage) {
            var telegramDoc = telegramMessage.getDocument();
            var fileId = telegramDoc.getFileId();
            var response = getFilePath(fileId);
            if (response.getStatusCode() == HttpStatus.OK) {
                var persistentBinaryContent = getPersistentBinaryContent(response);
                var transientAppDoc = buildTransientAppDoc(telegramDoc, persistentBinaryContent);
                return appDocumentDAO.save(transientAppDoc);
            } else {
                throw new UploadFileException("Bad response from telegram service: " + response);
            }
        }

        @Override
        public AppPhoto processPhoto(Message telegramMessage) {
            //TODO пока обрабатываем только одно фото в сообщении
            var telegramPhoto = telegramMessage.getPhoto().get(0);
            var fileId = telegramPhoto.getFileId();
            var response = getFilePath(fileId);
            if (response.getStatusCode() == HttpStatus.OK) {
                var persistentBinaryContent = getPersistentBinaryContent(response);
                var transientAppPhoto = buildTransientAppPhoto(telegramPhoto, persistentBinaryContent);
                return appPhotoDAO.save(transientAppPhoto);
            } else {
                throw new UploadFileException("Bad response from telegram service: " + response);
            }
        }

        private BinaryContent getPersistentBinaryContent(ResponseEntity<String> response) {
            var filePath = getFilePath(response);
            var fileInByte = downloadFile(filePath);
            var transientBinaryContent = BinaryContent.builder()
                    .fileAsArrayOfBytes(fileInByte)
                    .build();
            return binaryContentDAO.save(transientBinaryContent);
        }

        private String getFilePath(ResponseEntity<String> response) {
            var jsonObject = new JSONObject(response.getBody());
            return String.valueOf(jsonObject
                    .getJSONObject("result")
                    .getString("file_path"));
        }

        private AppDocument buildTransientAppDoc(Document telegramDoc, BinaryContent persistentBinaryContent) {
            return AppDocument.builder()
                    .telegramFileId(telegramDoc.getFileId())
                    .docName(telegramDoc.getFileName())
                    .binaryContent(persistentBinaryContent)
                    .mimeType(telegramDoc.getMimeType())
                    .fileSize(telegramDoc.getFileSize())
                    .build();
        }

        private AppPhoto buildTransientAppPhoto(PhotoSize telegramPhoto, BinaryContent persistentBinaryContent) {
            return AppPhoto.builder()
                    .telegramFileId(telegramPhoto.getFileId())
                    .binaryContent(persistentBinaryContent)
                    .fileSize(telegramPhoto.getFileSize())
                    .build();
        }

        private ResponseEntity<String> getFilePath(String fileId) {
            var restTemplate = new RestTemplate();
            var headers = new HttpHeaders();
            var request = new HttpEntity<>(headers);

            return restTemplate.exchange(
                    fileInfoUri,
                    HttpMethod.GET,
                    request,
                    String.class,
                    token, fileId
            );
        }

        private byte[] downloadFile(String filePath) {
            var fullUri = fileStorageUri.replace("{token}", token)
                    .replace("{filePath}", filePath);
            URL urlObj = null;
            try {
                urlObj = new URL(fullUri);
            } catch (MalformedURLException e) {
                throw new UploadFileException(e);
            }

            //TODO подумать над оптимизацией
            try (InputStream is = urlObj.openStream()) {
                return is.readAllBytes();
            } catch (IOException e) {
                throw new UploadFileException(urlObj.toExternalForm(), e);
            }
        }

}
