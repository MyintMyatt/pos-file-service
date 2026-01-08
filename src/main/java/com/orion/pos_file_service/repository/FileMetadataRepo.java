package com.orion.pos_file_service.repository;

import com.orion.pos_file_service.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileMetadataRepo extends JpaRepository<FileMetadata,String> {
}
