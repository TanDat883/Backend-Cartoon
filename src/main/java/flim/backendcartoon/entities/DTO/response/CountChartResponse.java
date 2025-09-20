package flim.backendcartoon.entities.DTO.response;


import java.util.List;

public class CountChartResponse {
    private List<String> labels;
    private List<Long> data;

    public CountChartResponse() {}
    public CountChartResponse(List<String> labels, List<Long> data) {
        this.labels = labels;
        this.data = data;
    }
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
    public List<Long> getData() { return data; }
    public void setData(List<Long> data) { this.data = data; }
}
