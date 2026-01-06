package com.orion.pos_file_service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.server.service.GrpcService;
import orion.grpc.file_service.FileServiceGrpc;
import orion.grpc.file_service.UploadRequest;
import orion.grpc.file_service.UploadResponse;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@GrpcService
public class GrpcFileService extends FileServiceGrpc.FileServiceImplBase {

    @Value("${storage.path}")
    private Path storagePath;

    @Override
    public StreamObserver<UploadRequest> uploadFile(StreamObserver<UploadResponse> responseObserver) {
        return new StreamObserver<UploadRequest>() {
            private OutputStream writer;
            private String fileId;
            private String fileName;

            @Override
            public void onNext(UploadRequest request) {
                try{
                    if (request.hasMetadata()) {
                        fileId = UUID.randomUUID().toString();
                        fileName = request.getMetadata().getFilename();
                        writer = Files.newOutputStream(storagePath.resolve(fileId + "_" + fileName));
                    } else if (request.hasChunk()) {
                        request.getChunk().writeTo(writer);
                    } else {
                        writer.write(request.getChunk().toByteArray());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    responseObserver.onError(
                            Status.INTERNAL.withDescription("file write filed : " + e.getMessage())
                                    .withCause(e)
                                    .asRuntimeException()
                    );
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println(throwable.fillInStackTrace());
            }

            @Override
            public void onCompleted() {
                System.err.println("Completed");
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
