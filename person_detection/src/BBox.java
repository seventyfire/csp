import org.opencv.core.Rect;

public class BBox {

    public BBox() {
    }

    public BBox(Rect rect, float confidence, String className) {
        this.rect = rect;
        this.confidence = confidence;
        this.className = className;
    }

    private Rect rect;

    private float confidence;

    private String className;

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}