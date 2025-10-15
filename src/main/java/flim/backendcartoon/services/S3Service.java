package flim.backendcartoon.services;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.mediaconvert.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.mediaconvert.model.AudioDefaultSelection;
import software.amazon.awssdk.services.mediaconvert.model.AudioSelectorType;
import software.amazon.awssdk.services.mediaconvert.model.AccelerationMode;
import software.amazon.awssdk.services.mediaconvert.model.AccelerationSettings;



import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final Dotenv dotenv = Dotenv.load();

    private final String bucketName = must("AWS_S3_BUCKET_NAME", dotenv.get("AWS_S3_BUCKET_NAME"));
    private final String region     = must("AWS_REGION",          dotenv.get("AWS_REGION"));
    private final String mcRoleArn  = must("MEDIACONVERT_ROLE_ARN",  dotenv.get("MEDIACONVERT_ROLE_ARN"));
    private final String mcQueueArn = must("MEDIACONVERT_QUEUE_ARN", dotenv.get("MEDIACONVERT_QUEUE_ARN"));
    private final String mcEndpoint = dotenv.get("MEDIACONVERT_ENDPOINT"); // optional (auto-discover nếu null)
    private final String cdnDomain  = dotenv.get("CLOUDFRONT_DOMAIN");      // optional
    private final String waitMode   = dotenv.get("MEDIACONVERT_WAIT_MODE"); // "sync" hoặc "async"/null

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // ====== Helpers chung ======

    private static String must(String name, String value) {
        if (value == null || value.isBlank())
            throw new IllegalStateException("Missing config: " + name);
        return value;
    }

    private static String sanitize(String name) {
        if (name == null) return "file";
        String n = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", ""); // bỏ dấu
        return n.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    private static String encodePath(String key) {
        String[] parts = key.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private String buildPublicUrl(String key) {
        String encoded = encodePath(key);
        if (cdnDomain != null && !cdnDomain.isBlank()) {
            return "https://" + cdnDomain + "/" + encoded;
        }
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + encoded;
    }

    // ====== Upload file "thô" (giữ nguyên để controller khỏi đổi) ======

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String key = folder + "/" + UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucketName).key(key).contentType(file.getContentType()).build();

        s3Client.putObject(put, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return buildPublicUrl(key);
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

    public String uploadTrailerUrl(MultipartFile trailerUrl) throws IOException {
        return uploadFile(trailerUrl, "trailerUrls");
    }

    // ====== HLS qua MediaConvert (giữ nguyên SIGNATURE cũ) ======
// VẪN TRẢ String .m3u8 URL để controller khỏi đổi
    public String convertAndUploadToHLS(MultipartFile videoFile) throws IOException {
        // 1) Upload input vào S3 (inputs/)
        String safe = sanitize(videoFile.getOriginalFilename());
        String inputKey = "inputs/" + UUID.randomUUID() + "_" + safe;

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(inputKey)
                .contentType(videoFile.getContentType())
                .build();
        s3Client.putObject(put, RequestBody.fromInputStream(videoFile.getInputStream(), videoFile.getSize()));

        String inputS3 = "s3://" + bucketName + "/" + inputKey;

        // 2) Tạo MediaConvert client (dùng endpoint env nếu có, nếu không auto-discover)
        Region r = Region.of(region);
        String endpoint = mcEndpoint;
        MediaConvertClient mc;
        if (endpoint == null || endpoint.isBlank()) {
            try (MediaConvertClient probe = MediaConvertClient.builder().region(r).build()) {
                endpoint = probe
                        .describeEndpoints(DescribeEndpointsRequest.builder().maxResults(1).build())
                        .endpoints().get(0).url();
            }
        }
        mc = MediaConvertClient.builder().region(r).endpointOverride(URI.create(endpoint)).build();

        // 3) Cấu hình HLS outputs (dùng System presets)
        String outFolder = UUID.randomUUID().toString();
        String destination = "s3://" + bucketName + "/hls/" + outFolder + "/";

        HlsGroupSettings hls = HlsGroupSettings.builder()
                .destination(destination)
                .segmentLength(6)
                .minSegmentLength(2)
                .segmentControl(HlsSegmentControl.SEGMENTED_FILES)
                .build();

        OutputGroupSettings ogs = OutputGroupSettings.builder()
                .type(OutputGroupType.HLS_GROUP_SETTINGS)
                .hlsGroupSettings(hls)
                .build();

        Output o540 = Output.builder()
                .nameModifier("_540p")
                .preset("System-Avc_16x9_540p_29_97fps_3500kbps")
                .build();
        Output o720 = Output.builder()
                .nameModifier("_720p")
                .preset("System-Avc_16x9_720p_29_97fps_5000kbps")
                .build();
        Output o1080 = Output.builder()
                .nameModifier("_1080p")
                .preset("System-Avc_16x9_1080p_29_97fps_8500kbps")
                .build();

        // Khai báo Audio Selector 1 và ép dùng track #1 (MediaConvert: set trực tiếp)
        java.util.Map<String, AudioSelector> audioSelectors = new java.util.HashMap<>();
        AudioSelector audioSel = AudioSelector.builder()
                .defaultSelection(AudioDefaultSelection.DEFAULT)   // enum đúng của MediaConvert
                .selectorType(AudioSelectorType.TRACK)             // chọn theo track
                .tracks(java.util.Arrays.asList(1))                // track index bắt đầu từ 1
                .build();
        audioSelectors.put("Audio Selector 1", audioSel);

        // Input có Audio Selector 1 đúng tên mà system preset đang tham chiếu
        Input input = Input.builder()
                .fileInput(inputS3)
                .audioSelectors(audioSelectors)
                .build();

        JobSettings settings = JobSettings.builder()
                .inputs(input)
                .outputGroups(OutputGroup.builder()
                        .name("Apple HLS")
                        .outputGroupSettings(ogs)
                        .outputs(o540, o720, o1080)
                        .build())
                .build();

// Đọc env (mặc định PREFERRED để tăng tốc; nếu không hỗ trợ sẽ tự fallback)
        String accEnv = String.valueOf(dotenv.get("MEDIACONVERT_ACCELERATION"));
        AccelerationMode accMode = "DISABLED".equalsIgnoreCase(accEnv)
                ? AccelerationMode.DISABLED
                : AccelerationMode.PREFERRED; // default nhanh nhất

        int priority = 0; // -50..50 (cao hơn = ưu tiên hơn)
        try { priority = Integer.parseInt(String.valueOf(dotenv.get("MEDIACONVERT_PRIORITY"))); } catch (Exception ignore) {}

        CreateJobResponse res = mc.createJob(CreateJobRequest.builder()
                .role(mcRoleArn)
                .queue(mcQueueArn)
                .settings(settings)
                .accelerationSettings(AccelerationSettings.builder().mode(accMode).build())
                .priority(priority)
                .build());

        // 4) LUÔN chờ MediaConvert hoàn tất (blocking)
        String jobId = res.job().id();
        waitUntilComplete(mc, jobId, java.time.Duration.ofMinutes(90));

        // 5) Chờ object hiện trên S3 và chọn manifest tốt nhất (master/index/khác)
        String prefix = "hls/" + outFolder + "/";
        if (!waitForM3u8AndSegments(prefix, 60, 2000)) { // tối đa ~120s
            throw new RuntimeException("HLS conversion completed but outputs not visible yet under: " + prefix);
        }
        String m3u8Key = findBestM3u8Key(prefix);
        if (m3u8Key == null) {
            throw new RuntimeException("MediaConvert completed but no .m3u8 found under: " + prefix);
        }

        // 6) Trả URL manifest hợp lệ (thường là master.m3u8 như ảnh bạn chụp)
        return buildPublicUrl(m3u8Key);
    }

    /** Chờ tới khi có ít nhất một .m3u8 và một .ts dưới prefix */
    private boolean waitForM3u8AndSegments(String prefix, int retryCount, long sleepMs) {
        for (int i = 0; i <= retryCount; i++) {
            boolean hasM3u8 = false, hasTs = false;
            String token = null;

            do {
                ListObjectsV2Request.Builder rb = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(prefix);
                if (token != null && !token.isEmpty()) {
                    rb = rb.continuationToken(token);
                }
                ListObjectsV2Response res = s3Client.listObjectsV2(rb.build());

                if (res.hasContents()) {
                    for (S3Object o : res.contents()) {
                        String k = o.key().toLowerCase();
                        if (k.endsWith(".m3u8")) hasM3u8 = true;
                        else if (k.endsWith(".ts")) hasTs = true;
                        if (hasM3u8 && hasTs) return true;
                    }
                }
                token = res.nextContinuationToken();
            } while (token != null && !token.isEmpty());

            try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    private String findBestM3u8Key(String prefix) {
        java.util.List<String> candidates = new java.util.ArrayList<>();
        String token = null;

        do {
            ListObjectsV2Request.Builder rb = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix);
            if (token != null && !token.isEmpty()) {
                rb = rb.continuationToken(token);
            }
            ListObjectsV2Response res = s3Client.listObjectsV2(rb.build());

            if (res.hasContents()) {
                for (S3Object o : res.contents()) {
                    String k = o.key();
                    if (k.toLowerCase().endsWith(".m3u8")) {
                        candidates.add(k);
                    }
                }
            }
            token = res.nextContinuationToken();
        } while (token != null && !token.isEmpty());

        if (candidates.isEmpty()) return null;

        // Ưu tiên master.m3u8 → index.m3u8 → bất kỳ
        for (String k : candidates) {
            String name = k.substring(k.lastIndexOf('/') + 1).toLowerCase();
            if (name.equals("master.m3u8")) return k;
        }
        for (String k : candidates) {
            String name = k.substring(k.lastIndexOf('/') + 1).toLowerCase();
            if (name.equals("index.m3u8")) return k;
        }
        return candidates.get(0);
    }



    /**
     * Kiểm tra sự tồn tại của master m3u8 và ít nhất một file .ts (hoặc child .m3u8).
     * retryCount & sleepMs giúp chờ thêm vài giây sau khi job COMPLETE.
     */
    private boolean hlsFilesExist(String masterM3u8Key, int retryCount, long sleepMs) {
        String prefix = masterM3u8Key.substring(0, masterM3u8Key.lastIndexOf('/') + 1);

        for (int i = 0; i <= retryCount; i++) {
            boolean masterOk = false;
            try {
                s3Client.headObject(b -> b.bucket(bucketName).key(masterM3u8Key));
                masterOk = true;
            } catch (Exception ignore) { /* 404 chưa có */ }

            boolean hasChildrenOrSegments = false;
            try {
                ListObjectsV2Response list = s3Client.listObjectsV2(b -> b.bucket(bucketName).prefix(prefix));
                if (list.hasContents()) {
                    for (S3Object o : list.contents()) {
                        String k = o.key().toLowerCase();
                        if (k.endsWith(".ts") || (k.endsWith(".m3u8") && !k.equalsIgnoreCase(masterM3u8Key))) {
                            hasChildrenOrSegments = true;
                            break;
                        }
                    }
                }
            } catch (Exception ignore) {}

            if (masterOk && hasChildrenOrSegments) return true;

            try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) {}
        }
        return false;
    }


    /** Poll trạng thái job tới COMPLETE/ERROR hoặc hết time */
    private void waitUntilComplete(MediaConvertClient mc, String jobId, Duration timeout) {
        long start = System.currentTimeMillis();
        long max = timeout.toMillis();
        long intervalMs = 5_000; // 5s

        while (true) {
            GetJobResponse g = mc.getJob(GetJobRequest.builder().id(jobId).build());
            JobStatus st = g.job().status();

            if (st == JobStatus.COMPLETE) return;
            if (st == JobStatus.ERROR) {
                String msg = g.job().errorMessage();
                throw new RuntimeException("MediaConvert job failed: " + (msg == null ? "unknown error" : msg));
            }

            if (System.currentTimeMillis() - start > max) {
                throw new RuntimeException("MediaConvert job timeout after " + timeout.toMinutes() + " minutes");
            }

            try { Thread.sleep(intervalMs); } catch (InterruptedException ignored) {}
        }
    }

    // ====== Xoá object / xoá cả thư mục HLS (giữ nguyên để controller không đổi) ======

    public void safeDeleteByUrl(String url) {
        if (url == null || url.isBlank()) return;
        urlToKey(url).ifPresent(key -> {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName).key(key).build());
            } catch (Exception ignore) {}
        });
    }

    public void safeDeleteHlsFolderByM3u8(String m3u8Url) {
        if (m3u8Url == null || m3u8Url.isBlank()) return;
        urlToKey(m3u8Url).ifPresent(masterKey -> {
            String prefix = masterKey.contains("/")
                    ? masterKey.substring(0, masterKey.lastIndexOf('/') + 1)
                    : masterKey;  // "hls/<uuid>/"

            try {
                ListObjectsV2Request req = ListObjectsV2Request.builder()
                        .bucket(bucketName).prefix(prefix).build();
                ListObjectsV2Response res;
                do {
                    res = s3Client.listObjectsV2(req);
                    if (res.hasContents()) {
                        for (S3Object o : res.contents()) {
                            try {
                                s3Client.deleteObject(DeleteObjectRequest.builder()
                                        .bucket(bucketName).key(o.key()).build());
                            } catch (Exception ignore) {}
                        }
                    }
                    req = req.toBuilder().continuationToken(res.nextContinuationToken()).build();
                } while (res.isTruncated());
            } catch (Exception ignore) {}
        });
    }

    public void deleteByMediaUrl(String url) {
        if (url == null || url.isBlank()) return;
        if (url.contains("/hls/") && url.endsWith(".m3u8")) {
            safeDeleteHlsFolderByM3u8(url);
        } else {
            safeDeleteByUrl(url);
        }
    }

    private Optional<String> urlToKey(String url) {
        try {
            URI u = URI.create(url);
            String path = u.getPath();
            if (path == null || path.isBlank()) return Optional.empty();
            return Optional.of(path.startsWith("/") ? path.substring(1) : path);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // ====== (giữ lại – hiện không dùng, nhưng không hại controller cũ) ======
    private void uploadDirRecursively(File root, String s3Prefix) throws IOException {
        for (File f : root.listFiles()) {
            if (f.isDirectory()) {
                uploadDirRecursively(f, s3Prefix + "/" + f.getName());
                continue;
            }
            String key = s3Prefix + "/" + f.getName();
            String ct = "application/octet-stream";
            String name = f.getName().toLowerCase();
            if (name.endsWith(".m3u8")) ct = "application/vnd.apple.mpegurl";
            else if (name.endsWith(".ts")) ct = "video/mp2t";

            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucketName).key(key).contentType(ct).build();
            s3Client.putObject(req, RequestBody.fromFile(f));
        }
    }

    // upload phụ đề phim
    public String uploadSubtitle(MultipartFile file) throws IOException {
        String key = "subtitles/" + UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        String ct = name.endsWith(".vtt") ? "text/vtt"
                : name.endsWith(".srt") ? "application/x-subrip"
                : "text/vtt";
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucketName).key(key).contentType(ct).build();
        s3Client.putObject(put, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return buildPublicUrl(key);
    }
}
