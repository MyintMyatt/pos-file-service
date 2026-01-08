package com.orion.pos_file_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tbl_file_meta_data")
public class FileMetadata {

    @Id
    private String fileId;
    private String filename;
    private String filetype;
    private long size;
    private String storagePath;
    private String ownerService;
    private String ownerId;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isDeleted;

}
