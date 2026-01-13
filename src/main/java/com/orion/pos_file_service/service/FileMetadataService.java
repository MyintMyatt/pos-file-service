package com.orion.pos_file_service.service;

import com.orion.pos_file_service.entity.FileMetadata;
import com.orion.pos_file_service.repository.FileMetadataRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FileMetadataService {

    private final FileMetadataRepo fileMetadataRepo;
    private final Logger log = LoggerFactory.getLogger(FileMetadataService.class);

    public void saveFileMetadata(String fileId, String fileName, String fileType, long fileSize, Path targetFile, String ownerService, String ownerId) {

        FileMetadata fileMetadata = FileMetadata.builder()
                .fileId(fileId)
                .filename(fileName)
                .filetype(fileType)
                .size(fileSize)
                .storagePath(String.valueOf(targetFile.toAbsolutePath()))
                .ownerService(ownerService)
                .ownerId(ownerId)
                .createdAt(Instant.now())
                .isDeleted(false)
                .build();

        fileMetadataRepo.save(fileMetadata);
        log.info("successfully saved file metadata in db");
    }

    public String getFilePathById(String fileId) {
        com.orion.pos_file_service.entity.FileMetadata metadata = fileMetadataRepo.getReferenceById(fileId);
        return metadata.getStoragePath();
    }
}
