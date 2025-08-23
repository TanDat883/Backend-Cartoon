package flim.backendcartoon.services;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.io.IOException;
import java.util.UUID;


@Service
public class S3Service {

    private final S3Client s3Client;

    private final Dotenv dotenv = Dotenv.load();

    private final String bucketName = dotenv.get("AWS_S3_BUCKET_NAME");


    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String key = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                file.getInputStream(), file.getSize()
        ));

        return "https://" + bucketName + ".s3.amazonaws.com/" + key;
    }

    public String uploadVideo(MultipartFile video) throws IOException {
        return uploadFile(video, "videos");
    }

    public String uploadThumbnail(MultipartFile thumbnail) throws IOException {
        return uploadFile(thumbnail, "thumbnails");
    }
    public String uploadAvatarUrl(MultipartFile avatarUrl) throws IOException {
        return uploadFile(avatarUrl, "avatarUrls");
    }
    public String uploadBannerUrl(MultipartFile bannerUrl) throws IOException {
        return uploadFile(bannerUrl, "bannerUrls");
    }
    //trailer
    public String uploadTrailerUrl(MultipartFile trailerUrl) throws IOException {
        return uploadFile(trailerUrl, "trailerUrls");
    }

    //áp dụng stream cho video phim - tín 18/7/2025
    public String convertAndUploadToHLS(MultipartFile videoFile) throws IOException, InterruptedException {
        // Lưu video tạm thời
        String inputFileName = UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
        File tempInput = new File(System.getProperty("java.io.tmpdir"), inputFileName);
        videoFile.transferTo(tempInput);

        // Tạo thư mục tạm chứa output HLS
        File outputDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        outputDir.mkdirs();

        // Lệnh convert sang HLS
        ProcessBuilder pb = new ProcessBuilder(
                "C:\\ffmpeg-2025-07-17-git-bc8d06d541-essentials_build\\bin\\ffmpeg.exe",
                "-i", tempInput.getAbsolutePath(),
                "-profile:v", "baseline",
                "-level", "3.0",
                "-start_number", "0",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-f", "hls",
                outputDir.getAbsolutePath() + "/output.m3u8"
        );
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg conversion failed");
        }

        // Upload tất cả file .ts và .m3u8 lên S3
        String hlsFolderKey = "hls/" + UUID.randomUUID();
        for (File file : outputDir.listFiles()) {
            String key = hlsFolderKey + "/" + file.getName();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/octet-stream")
                    .build();

            s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromFile(file));
        }

        // Trả về link tới file .m3u8
        return "https://" + bucketName + ".s3.amazonaws.com/" + hlsFolderKey + "/output.m3u8";
    }



    /** Xoá 1 object đơn (thumbnail/banner/mp4...) dựa trên URL */
    public void safeDeleteByUrl(String url) {
        if (url == null || url.isBlank()) return;
        urlToKey(url).ifPresent(key -> {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
            } catch (Exception ignore) {}
        });
    }

    /** Xoá toàn bộ thư mục HLS dựa trên URL .m3u8 (playlist + các .ts) */
    public void safeDeleteHlsFolderByM3u8(String m3u8Url) {
        if (m3u8Url == null || m3u8Url.isBlank()) return;
        urlToKey(m3u8Url).ifPresent(masterKey -> {
            // VD: masterKey = "hls/416eee43-bd6f-4f88-a4b4-8da1b1fe80d4/output.m3u8"
            String prefix = masterKey.contains("/") ?
                    masterKey.substring(0, masterKey.lastIndexOf('/') + 1) :
                    masterKey;  // "hls/<uuid>/"

            try {
                ListObjectsV2Request req = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .build();
                ListObjectsV2Response res;
                do {
                    res = s3Client.listObjectsV2(req);
                    if (res.hasContents()) {
                        for (S3Object o : res.contents()) {
                            try {
                                s3Client.deleteObject(DeleteObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(o.key())
                                        .build());
                            } catch (Exception ignore) {}
                        }
                    }
                    req = req.toBuilder().continuationToken(res.nextContinuationToken()).build();
                } while (res.isTruncated());
            } catch (Exception ignore) {}
        });
    }

    /** Nhận URL media, tự chọn xoá file đơn hay cả folder HLS */
    public void deleteByMediaUrl(String url) {
        if (url == null || url.isBlank()) return;
        // Dạng bạn đưa: hls/<uuid>/output.m3u8
        if (url.contains("/hls/") && url.endsWith(".m3u8")) {
            safeDeleteHlsFolderByM3u8(url);
        } else {
            safeDeleteByUrl(url);
        }
    }

    /** Helper: chuyển URL S3/CloudFront → object key trong bucket */
    private java.util.Optional<String> urlToKey(String url) {
        try {
            java.net.URI u = java.net.URI.create(url);
            String path = u.getPath(); // "/hls/416e.../output.m3u8"
            if (path == null || path.isBlank()) return java.util.Optional.empty();
            return java.util.Optional.of(path.startsWith("/") ? path.substring(1) : path);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

}
