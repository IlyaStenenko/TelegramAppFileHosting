package org.example.controller;

import lombok.extern.log4j.Log4j;
import org.example.dao.AppDocumentDAO;
import org.example.dao.AppKeyDAO;
import org.example.dao.AppPhotoDAO;
import org.example.entity.AppKey;
import org.example.utils.AES;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.example.service.FileService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Log4j
@RequestMapping("/file")
@RestController
public class FileController {
    private final FileService fileService;
    private final AppPhotoDAO appPhotoDAO;

    private final AppDocumentDAO appDocumentDAO;

    private final AppKeyDAO appKeyDAO;

    public FileController(FileService fileService, AppPhotoDAO appPhotoDAO, AppDocumentDAO appDocumentDAO, AppKeyDAO appKeyDAO) {
        this.fileService = fileService;
        this.appPhotoDAO = appPhotoDAO;
        this.appDocumentDAO = appDocumentDAO;
        this.appKeyDAO = appKeyDAO;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/get-doc")
    public ResponseEntity<?> getDoc(@RequestParam("id") String id) {
        //TODO для формирования badRequest добавить ControllerAdvice
        var doc = fileService.getDocument(id);
        System.out.println(doc.getDocName());
        if (doc == null) {
            return ResponseEntity.badRequest().build();
        }
        var binaryContent = binaryDecoding(doc.getBinaryContent().getFileAsArrayOfBytes(),
                doc.getBinaryContent().getId());


        // Проверяем, является ли имя документа пустым или null
        String filename = doc.getDocName() != null ? doc.getDocName() : "file"; // По умолчанию "file", если имя документа не определено
        // Кодируем имя файла, чтобы избежать проблем с не-ASCII символами
        filename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

        var fileSystemResource = fileService.getFileSystemResource(binaryContent,doc.getBinaryContent().getId());
        if (fileSystemResource == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(fileSystemResource);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/get-photo")
    public ResponseEntity<?> getPhoto(@RequestParam("id") String id) {
        //TODO для формирования badRequest добавить ControllerAdvice
        var photo = fileService.getPhoto(id);
        if (photo == null) {
            return ResponseEntity.badRequest().build();
        }
        var binaryContent = binaryDecoding(photo.getBinaryContent().getFileAsArrayOfBytes(),
                photo.getBinaryContent().getId());

        // Проверяем, является ли имя документа пустым или null
        String filename = photo.getPhotoName() != null ? photo.getPhotoName() : "file.jpeg"; // По умолчанию "file", если имя документа не определено
        // Кодируем имя файла, чтобы избежать проблем с не-ASCII символами
        filename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

        var fileSystemResource = fileService.getFileSystemResource(binaryContent,photo.getBinaryContent().getId());
        if (fileSystemResource == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(MediaType.IMAGE_JPEG.toString()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(fileSystemResource);
    }

    public byte[] binaryDecoding(byte[] binaryContent, long contentId) {
        AppKey appKey = appKeyDAO.getById(contentId);
        byte[] decryptFile = AES.decrypt(binaryContent,appKey.getKey());
        return decryptFile;
    }
}
