package flim.backendcartoon.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationResponse;
import software.amazon.awssdk.services.cloudfront.model.Distribution;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.cloudfront.model.Paths;

import java.net.URI;
import java.util.List;
import java.util.UUID;


@Service
public class CloudFrontService {

    private final CloudFrontClient cloudFrontClient;


    @Value("${aws.cloudfront.distribution-id:EUVCNTE9CJQKF}")
    private String distributionId;

    @Value("${aws.cloudfront.domain:d621tiwb61jda.cloudfront.net}")
    private String cloudFrontDomain;

    @Value("${aws.cloudfront.enabled:true}")
    private boolean enabled;

    @Value("${aws.cloudfront.auto-convert-urls:true}")
    private boolean autoConvertUrls;

    public CloudFrontService(CloudFrontClient cloudFrontClient) {
        this.cloudFrontClient = cloudFrontClient;
    }

    /** Convert S3 URL sang CloudFront URL (không đụng DB) */
    public String convertToCloudFrontUrl(String s3Url) {
        if (!enabled || !autoConvertUrls || s3Url == null || s3Url.isBlank()) {
            return s3Url;
        }
        if (cloudFrontDomain == null || cloudFrontDomain.isBlank()) {
            return s3Url;
        }
        // Đã là CF URL thì giữ nguyên
        if (s3Url.contains(cloudFrontDomain)) {
            return s3Url;
        }
        String key = extractObjectKeyFromS3Url(s3Url);
        if (key == null) {
            return s3Url;
        }
        return "https://" + cloudFrontDomain + "/" + stripLeadingSlash(key);
    }

    /** Lấy object key từ nhiều dạng S3 URL khác nhau */
    private String extractObjectKeyFromS3Url(String s3Url) {
        try {
            // Dạng phổ biến: https://bucket.s3.region.amazonaws.com/key...
            int idx = s3Url.indexOf(".amazonaws.com/");
            if (idx > 0) {
                return s3Url.substring(idx + ".amazonaws.com/".length());
            }

            // Dạng path-style: https://s3.region.amazonaws.com/bucket/key...
            URI uri = URI.create(s3Url);
            String host = uri.getHost();   // ví dụ: s3.ap-southeast-1.amazonaws.com
            String path = uri.getPath();   // ví dụ: /bucket/key/...
            if (host != null && host.startsWith("s3.") && path != null && path.length() > 1) {
                // Bỏ segment bucket đầu tiên
                return path.replaceFirst("^/[^/]+/", "");
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String stripLeadingSlash(String s) {
        return s == null ? null : s.replaceFirst("^/+", "");
    }

    /** Tạo invalidation cho danh sách đường dẫn (ví dụ: ["/hls/*"]) */
    public String invalidateCache(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            throw new IllegalArgumentException("paths must not be empty");
        }
        Paths p = Paths.builder()
                .quantity(paths.size())
                .items(paths)
                .build();

        InvalidationBatch batch = InvalidationBatch.builder()
                .paths(p)
                .callerReference("inv-" + UUID.randomUUID())
                .build();

        CreateInvalidationRequest req = CreateInvalidationRequest.builder()
                .distributionId(distributionId)
                .invalidationBatch(batch)
                .build();

        CreateInvalidationResponse resp = cloudFrontClient.createInvalidation(req);
        return resp.invalidation().id();
    }

    /** Lấy thông tin distribution */
    public CloudFrontDistributionInfo getDistributionInfo() {
        GetDistributionRequest req = GetDistributionRequest.builder()
                .id(distributionId)
                .build();
        GetDistributionResponse resp = cloudFrontClient.getDistribution(req);
        Distribution d = resp.distribution();

        CloudFrontDistributionInfo info = new CloudFrontDistributionInfo();
        info.setId(d.id());
        info.setDomainName(d.domainName());
        info.setStatus(d.status());
        info.setEnabled(d.distributionConfig().enabled());
        return info;
    }

    /** Health = enabled & status == Deployed */
    public boolean isDistributionHealthy() {
        try {
            CloudFrontDistributionInfo info = getDistributionInfo();
            return info.isEnabled() && "Deployed".equalsIgnoreCase(info.getStatus());
        } catch (Exception e) {
            return false;
        }
    }

    /** DTO đơn giản cho /api/cloudfront/info */
    public static class CloudFrontDistributionInfo {
        private String id;
        private String domainName;
        private String status;
        private boolean enabled;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDomainName() { return domainName; }
        public void setDomainName(String domainName) { this.domainName = domainName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
