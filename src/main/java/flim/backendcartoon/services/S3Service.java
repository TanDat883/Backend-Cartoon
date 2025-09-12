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
// S3Service.java
    public String convertAndUploadToHLS(MultipartFile videoFile) throws IOException, InterruptedException {
        // 1) Lưu file tạm
        String inputFileName = UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
        File tempInput = new File(System.getProperty("java.io.tmpdir"), inputFileName);
        videoFile.transferTo(tempInput);

        // 2) Thư mục output tạm
        File outputDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        if (!outputDir.mkdirs()) throw new IOException("Cannot create temp HLS dir");

        // 3) Cấu hình các mức
        class V {
            String name; int w, h; int vKbps; int aKbps; // bitrate xấp xỉ
            V(String n, int w, int h, int v, int a){ this.name=n; this.w=w; this.h=h; this.vKbps=v; this.aKbps=a; }
        }
        V[] ladder = new V[] {
                new V("480p",  854,  480,   800,  96),
                new V("720p", 1280,  720,  2500, 128),
                new V("1080p",1920, 1080,  5000, 192),
        };

        // 4) Chạy ffmpeg cho từng mức
        for (V v : ladder) {
            File varDir = new File(outputDir, v.name);
            if (!varDir.mkdirs()) throw new IOException("Cannot create dir for " + v.name);

            // scale giữ tỉ lệ, không phóng to quá mức (decrease)
            String ffmpeg = System.getenv().getOrDefault("FFMPEG_PATH",
                    "C:\\ffmpeg-2025-07-17-git-bc8d06d541-essentials_build\\bin\\ffmpeg.exe");

// bộ filter bảo đảm even size + letterbox
            String vf = switch (v.name) {
                case "480p"  -> "scale='if(gt(a,854/480),854,-2)':'if(gt(a,854/480),-2,480)',pad=854:480:(854-iw)/2:(480-ih)/2,format=yuv420p";
                case "720p"  -> "scale='if(gt(a,1280/720),1280,-2)':'if(gt(a,1280/720),-2,720)',pad=1280:720:(1280-iw)/2:(720-ih)/2,format=yuv420p";
                default      -> "scale='if(gt(a,1920/1080),1920,-2)':'if(gt(a,1920/1080),-2,1080)',pad=1920:1080:(1920-iw)/2:(1080-ih)/2,format=yuv420p";
            };

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpeg,
                    "-y",
                    "-hide_banner","-loglevel","error",         // log gọn dễ bắt lỗi
                    "-i", tempInput.getAbsolutePath(),

                    // đảm bảo luôn có stream video, audio (audio có thể vắng)
                    "-map","0:v:0","-map","0:a:0?",

                    "-c:v","libx264",
                    "-profile:v","main",
                    "-preset","veryfast",
                    "-pix_fmt","yuv420p",
                    "-b:v", v.vKbps + "k",
                    "-maxrate", (int)Math.round(v.vKbps*1.07) + "k",
                    "-bufsize", (v.vKbps*2) + "k",
                    "-g","48","-sc_threshold","0",              // GOP ổn định cho HLS

                    "-vf", vf,

                    "-c:a","aac",
                    "-b:a", v.aKbps + "k",
                    "-ac","2",
                    "-ar","48000",

                    "-hls_time","6",
                    "-hls_list_size","0",
                    "-hls_flags","independent_segments",        // segment tự lập, an toàn cho seek
                    "-hls_segment_filename", new File(varDir, "seg_%03d.ts").getAbsolutePath(),
                    new File(varDir, "index.m3u8").getAbsolutePath()
            );

            pb.inheritIO();
            Process p = pb.start();
            if (p.waitFor() != 0) throw new RuntimeException("FFmpeg failed at " + v.name);
        }

        // 5) Tạo master.m3u8
        File master = new File(outputDir, "master.m3u8");
        String masterTxt =
                "#EXTM3U\n" +
                        "#EXT-X-VERSION:3\n" +
                        // BANDWIDTH tính theo bps (video+audio, cộng thêm overhead nhẹ)
                        "#EXT-X-STREAM-INF:BANDWIDTH=" + ((800+96+80)*1000) + ",RESOLUTION=854x480\n" + "480p/index.m3u8\n" +
                        "#EXT-X-STREAM-INF:BANDWIDTH=" + ((2500+128+150)*1000) + ",RESOLUTION=1280x720\n" + "720p/index.m3u8\n" +
                        "#EXT-X-STREAM-INF:BANDWIDTH=" + ((5000+192+300)*1000) + ",RESOLUTION=1920x1080\n" + "1080p/index.m3u8\n";
        java.nio.file.Files.writeString(master.toPath(), masterTxt, java.nio.charset.StandardCharsets.UTF_8);

        // 6) Upload toàn bộ folder lên S3 (đặt content-type đúng)
        String hlsFolderKey = "hls/" + UUID.randomUUID();
        uploadDirRecursively(outputDir, hlsFolderKey);

        // 7) Trả về URL master
        return "https://" + bucketName + ".s3.amazonaws.com/" + hlsFolderKey + "/master.m3u8";
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
}
