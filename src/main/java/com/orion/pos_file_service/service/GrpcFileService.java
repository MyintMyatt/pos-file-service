package com.orion.pos_file_service.service;

import com.google.protobuf.ByteString;
import com.orion.pos_file_service.entity.FileMetadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.server.service.GrpcService;
import orion.grpc.file_service.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@GrpcService
public class GrpcFileService extends FileServiceGrpc.FileServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcFileService.class);

    @Value("${storage.path}")
    private String storagePath;

    @Autowired
    private FileMetadataService fileMetadataService;

    @Override
    public StreamObserver<UploadRequest> uploadFile(StreamObserver<UploadResponse> responseObserver) {
        return new StreamObserver<UploadRequest>() {
            private OutputStream writer;
            private String fileId;
            private String fileName;
            private String fileType;
            private long fileSize;
            private Path targetFile;
            private String ownerService;
            private String ownerId;

            @Override
            public void onNext(UploadRequest request) {
                try {
                    if (request.hasMetadata()) {

                        log.info("start file id generation and create target file operation......");

                        fileId = UUID.randomUUID().toString();
                        fileName = request.getMetadata().getFilename();
                        fileType = request.getMetadata().getFiletype();
                        fileSize = (long) request.getMetadata().getFilesize();
                        ownerService = request.getMetadata().getOwnerService();
                        ownerId = request.getMetadata().getOwnerId();
                        targetFile = Path.of(storagePath, fileId + "_" + fileName);
                        Files.createDirectories(targetFile.getParent());
                        writer = Files.newOutputStream(
                                targetFile,
                                StandardOpenOption.CREATE_NEW
                        );

                        log.info("completed file path generation : {}" + targetFile);
//                        writer = Files.newOutputStream(storagePath.resolve(fileId + "_" + fileName));
                    } else if (request.hasChunk()) {
                        if (writer == null) {
                            throw new IllegalStateException("Metadata must be sent first");
                        }

                        log.info("writing chunk.......");
                        request.getChunk().writeTo(writer);
                    }
                } catch (IOException e) {

                    log.error("error occur : " + e.getMessage());

                    responseObserver.onError(
                            Status.INTERNAL.withDescription("file write filed : " + e.getMessage())
                                    .withCause(e)
                                    .asRuntimeException()
                    );
                }
            }

            @Override
            public void onError(Throwable throwable) {
                cleanup();
                log.error("error occur : " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                cleanup();

                responseObserver.onNext(
                        UploadResponse.newBuilder()
                                .setFileId(fileId)
                                .setStatus(200)
                                .setMessage("successfully uploaded")
                                .build()
                );
                log.info("successfully uploaded file. file id => " + fileId);
                responseObserver.onCompleted();


                log.info("saving file metadata to database.......");
                fileMetadataService.saveFileMetadata(fileId, fileName, fileType, fileSize, targetFile, ownerService, ownerId);

            }

            private void cleanup() {
                try {
                    if (writer != null) writer.close();
                    ;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public void downloadFile(DownloadRequest request, StreamObserver<DownloadResponse> responseObserver) {
        String fileId = request.getFileId();

        try {
            String filePath = fileMetadataService.getFilePathById(fileId);
            log.info("file path {} find by file id {}", filePath, fileId);
            Path path = Path.of(filePath);

            try (InputStream in = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    responseObserver.onNext(
                            DownloadResponse.newBuilder()
                                    .setChunk(ByteString.copyFrom(buffer, 0, bytesRead))
                                    .build()
                    );
                }
            }

            responseObserver.onCompleted();
        } catch (IOException e) {
            log.warn("error : {}", e.getMessage());
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException());
            throw new RuntimeException(e);
        }
    }
}
