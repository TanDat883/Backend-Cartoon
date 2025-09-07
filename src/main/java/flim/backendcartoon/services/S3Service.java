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

    // Upload cả thư mục (m3u8/ts đúng content-type)
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
                    .bucket(bucketName)
                    .key(key)
                    .contentType(ct)
                    .build();

            s3Client.putObject(req, software.amazon.awssdk.core.sync.RequestBody.fromFile(f));
        }
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
