package org.dromara.project.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.entity.UploadResult;
import org.dromara.common.oss.factory.OssFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class MaterialStorageService {

    public String uploadMaterial(Long tenantId, Long projectId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        String original = StringUtils.blankToDefault(file.getOriginalFilename(), "material.bin");
        String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = tenantId + "/" + projectId + "/materials/" + UUID.randomUUID() + "-" + safeName;
        try {
            byte[] data = file.getBytes();
            OssClient storage = OssFactory.instance();
            UploadResult result = storage.upload(
                new ByteArrayInputStream(data),
                key,
                (long) data.length,
                StringUtils.blankToDefault(file.getContentType(), "application/octet-stream")
            );
            return result.getUrl();
        } catch (IOException e) {
            throw new ServiceException("读取上传文件失败: " + e.getMessage());
        } catch (Exception e) {
            throw new ServiceException("素材上传 MinIO 失败: " + e.getMessage());
        }
    }
}
