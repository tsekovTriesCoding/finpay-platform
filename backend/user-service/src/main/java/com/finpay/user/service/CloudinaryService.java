package com.finpay.user.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads a profile image to Cloudinary.
     *
     * @param file   the image file to upload
     * @param userId the user's ID (used as part of the public_id)
     * @return the secure URL of the uploaded image
     */
    @SuppressWarnings("unchecked")
    public String uploadProfileImage(MultipartFile file, UUID userId) {
        try {
            log.info("Uploading profile image for user: {}", userId);

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "finpay/profiles",
                    "public_id", userId.toString(),
                    "overwrite", true,
                    "resource_type", "image",
                    "transformation", "c_fill,w_400,h_400,g_face,q_auto,f_auto"
            ));

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Profile image uploaded successfully for user: {}. URL: {}", userId, secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Failed to upload profile image for user: {}", userId, e);
            throw new RuntimeException("Failed to upload profile image: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a profile image from Cloudinary.
     *
     * @param userId the user's ID
     */
    public void deleteProfileImage(UUID userId) {
        try {
            log.info("Deleting profile image for user: {}", userId);
            cloudinary.uploader().destroy("finpay/profiles/" + userId, ObjectUtils.emptyMap());
            log.info("Profile image deleted successfully for user: {}", userId);
        } catch (IOException e) {
            log.error("Failed to delete profile image for user: {}", userId, e);
        }
    }
}
