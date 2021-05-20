import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;

import static org.opencv.imgproc.Imgproc.rectangle;

public class ImageUtil {

    // draw a detected rectangular area
    public static void drawRectangle(String className, float confidence, Mat im, Rect rect) {
        String text;
        Point br = rect.br(); // right-bottom
        Point tl = rect.tl(); // left-top

        double x = tl.x;
        double y = tl.y;


        if ("person".equals(className)) {
            rectangle(im, tl, br, new Scalar(0, 0, 255), 1);
            text = className + ":" + confidence;
            Imgproc.putText(im, text, new Point(x, y - 5), Imgproc.FONT_HERSHEY_SIMPLEX, 0.3, new Scalar(0, 255, 0), 1);

        } else {
            rectangle(im, tl, br, new Scalar(0, 255, 0), 1);
            text = String.format("%s %f", className, confidence);
            Imgproc.putText(im, text, new Point(x, y - 5), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 255), 1);
        }
    }

    // convert mat to BufferedImage
    public static BufferedImage conver2Image(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int dims = mat.channels();
        int[] pixels = new int[width * height];
        byte[] rgbData = new byte[width * height * dims];
        mat.get(0, 0, rgbData);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int index = 0;
        int r = 0, g = 0, b = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (dims == 3) {
                    index = row * width * dims + col * dims;
                    b = rgbData[index] & 0xff;
                    g = rgbData[index + 1] & 0xff;
                    r = rgbData[index + 2] & 0xff;
                    pixels[row * width + col] = (0xff << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | b & 0xff;
                }
                if (dims == 1) {
                    index = row * width + col;
                    b = rgbData[index] & 0xff;
                    pixels[row * width + col] = (0xff << 24) | ((b & 0xff) << 16) | ((b & 0xff) << 8) | b & 0xff;
                }
            }
        }
        setRGB(image, 0, 0, width, height, pixels);
        return image;
    }

    // set ARGB
    public static void setRGB(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
        int type = image.getType();
        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB)
            image.getRaster().setDataElements(x, y, width, height, pixels);
        else
            image.setRGB(x, y, width, height, pixels, 0, width);
    }
}
