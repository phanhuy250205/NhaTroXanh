package nhatroxanh.com.Nhatroxanh.Model.Dto;



public class PdfOptions {
    private String pageSize = "A4";
    private String orientation = "portrait";
    private float marginTop = 20f;
    private float marginBottom = 20f;
    private float marginLeft = 20f;
    private float marginRight = 20f;
    private boolean enableJavaScript = false;

    // Constructors
    public PdfOptions() {}

    public PdfOptions(String pageSize, String orientation) {
        this.pageSize = pageSize;
        this.orientation = orientation;
    }

    // Getters and Setters
    public String getPageSize() { return pageSize; }
    public void setPageSize(String pageSize) { this.pageSize = pageSize; }

    public String getOrientation() { return orientation; }
    public void setOrientation(String orientation) { this.orientation = orientation; }

    public float getMarginTop() { return marginTop; }
    public void setMarginTop(float marginTop) { this.marginTop = marginTop; }

    public float getMarginBottom() { return marginBottom; }
    public void setMarginBottom(float marginBottom) { this.marginBottom = marginBottom; }

    public float getMarginLeft() { return marginLeft; }
    public void setMarginLeft(float marginLeft) { this.marginLeft = marginLeft; }

    public float getMarginRight() { return marginRight; }
    public void setMarginRight(float marginRight) { this.marginRight = marginRight; }

    public boolean isEnableJavaScript() { return enableJavaScript; }
    public void setEnableJavaScript(boolean enableJavaScript) { this.enableJavaScript = enableJavaScript; }
}
