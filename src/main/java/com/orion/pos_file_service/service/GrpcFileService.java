package com.orion.pos_file_service.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.server.service.GrpcService;
import orion.grpc.file_service.FileServiceGrpc;
import orion.grpc.file_service.UploadRequest;
import orion.grpc.file_service.UploadResponse;
import java.io.IOException;
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

    @Override
    public StreamObserver<UploadRequest> uploadFile(StreamObserver<UploadResponse> responseObserver) {
        return new StreamObserver<UploadRequest>() {
            private OutputStream writer;
            private String fileId;
            private String fileName;
            private Path targetFile;

            @Override
            public void onNext(UploadRequest request) {
                try {
                    if (request.hasMetadata()) {

                        log.info("start file id generation and create target file operation......");

                        fileId = UUID.randomUUID().toString();
                        fileName = request.getMetadata().getFilename();
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

            }

            private void cleanup() {
                try{
                    if (writer != null) writer.close();;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }



}
