package flim.backendcartoon.services;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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


}
