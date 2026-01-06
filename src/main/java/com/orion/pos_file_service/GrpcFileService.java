package com.orion.pos_file_service;

import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.server.service.GrpcService;
import orion.grpc.file_service.FileServiceGrpc;
import orion.grpc.file_service.UploadRequest;
import orion.grpc.file_service.UploadResponse;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@GrpcService
public class GrpcFileService extends FileServiceGrpc.FileServiceImplBase {

    @Value("${storage.path}")
    private String storagePath;

    @Override
    public StreamObserver<UploadRequest> uploadFile(StreamObserver<UploadResponse> responseObserver) {
        return new StreamObserver<UploadRequest>() {

            private FileOutputStream outputStream;
            private String fileId = UUID.randomUUID().toString();

            @Override
            public void onNext(UploadRequest uploadRequest) {
                try {
                    if (uploadRequest.hasFilename()) {
                        outputStream = new FileOutputStream(storagePath + "/" + fileId + "_" + uploadRequest.getFilename());
                    } else {
                        outputStream.write(uploadRequest.getChunk().toByteArray());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println(throwable.fillInStackTrace());
            }

            @Override
            public void onCompleted() {
                System.err.println("Completed");
            }
        };
    }
}
