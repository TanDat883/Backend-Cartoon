package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;

@DynamoDbBean
public class SubtitleTrack {
    private String lang;        // vi | en | ja | zh-Hans ...
    private String label;       // "Tiếng Việt" | "English"
    private String kind;        // "subtitles" | "captions"
    private Boolean isDefault;  // 1 track default/episode
    private String url;         // .vtt (ưu tiên); chấp nhận .srt nếu bạn convert
    private Instant createdAt;

    @DynamoDbAttribute("lang")        public String getLang(){return lang;}
    public void setLang(String v){this.lang=v;}

    @DynamoDbAttribute("label")       public String getLabel(){return label;}
    public void setLabel(String v){this.label=v;}

    @DynamoDbAttribute("kind")        public String getKind(){return kind;}
    public void setKind(String v){this.kind=v;}

    @DynamoDbAttribute("isDefault")   public Boolean getIsDefault(){return isDefault;}
    public void setIsDefault(Boolean v){this.isDefault=v;}

    @DynamoDbAttribute("url")         public String getUrl(){return url;}
    public void setUrl(String v){this.url=v;}

    @DynamoDbAttribute("createdAt")   public Instant getCreatedAt(){return createdAt;}
    public void setCreatedAt(Instant t){this.createdAt=t;}
}