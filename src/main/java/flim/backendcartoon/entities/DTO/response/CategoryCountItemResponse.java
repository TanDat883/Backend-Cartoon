package flim.backendcartoon.entities.DTO.response;

public class CategoryCountItemResponse {
    private String key;   // genre/country/status/type/year...
    private long count;

    public CategoryCountItemResponse() {}
    public CategoryCountItemResponse(String key, long count) {
        this.key = key;
        this.count = count;
    }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
