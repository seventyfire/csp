import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.util.*;

public class VideoHandleService {

    private final static float CONF_THRESHOLD = 0.5f; //the threshold of confidence

    private final static float NMS_THRESHOLD = 0.1f;  // the threshold of Intersection-over-Union/IoU

    //  set the CNN model path
    static String cfg = "path/to/yolov3.cfg";
    static String model = "path/to/yolov3.weights";
    static Net net = Dnn.readNetFromDarknet(cfg, model);


    //  image size
    private static Size size = new Size(416, 416);

    private final String[] classCanBeDetected = new String[]{
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
            "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
            "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
            "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch", "potted plant", "bed",
            "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", "oven",
            "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
    };  // object categories that can be detected by the program

    // a Mat object represents a frame in the video of the detection result,
    // which is essentially a image
    public void processDetectedObjects(Mat outputMat, Mat originalMat, List<BBox> bboxList) {
        int infoIdx = 0;

        int row = outputMat.rows(); // row: the number of detected objects
        int col = outputMat.cols(); // col: the number of all the objects that can be detected
        for (int i = 0; i < row; i++) {
            int c_size = col * outputMat.channels();
            float[] data = new float[c_size];
            outputMat.get(i, 0, data); // data is an array: [x,y,h,w,c,class1 confidence,class2 confidence....]
            int firstClassIndex = 5; // the index of class1 confidence in data

            ArrayList<Float> confidenceList = new ArrayList<>();

            for (int j = firstClassIndex; j < outputMat.cols(); j++) {
                // confidence represents the degree of confidence that something is recognized as a certain class
                float confidence = data[j];
                // there ara so many classes can be detected by the program,
                // such as ["person", "bicycle", "car", "motorcycle", "airplane"...],
                // I put them into the classCanBeDetected array
                confidenceList.add(confidence);
            }

            int maxConfidenceIndex = findMaxConfidence(confidenceList, 0.5f);

            if (maxConfidenceIndex != -1) {
                float x = data[0];
                float y = data[1];

                float width = data[2];
                float height = data[3];

                float xLeftBottom = (x - width / 2) * originalMat.cols();
                float yLeftBottom = (y - height / 2) * originalMat.rows();
                float xRightTop = (x + width / 2) * originalMat.cols();
                float yRightTop = (y + height / 2) * originalMat.rows();

                BBox bBox = new BBox(); // a BBox object represents a green rectangular detected area
                bBox.setRect(new Rect(new Point(xLeftBottom, yLeftBottom), new Point(xRightTop, yRightTop)));
                bBox.setConfidence(confidenceList.get(maxConfidenceIndex));
                bBox.setClassName(classCanBeDetected[maxConfidenceIndex]);
                bboxList.add(bBox);
            }

        }
    }

    public int findMaxConfidence(ArrayList<Float> confidenceList, float threshold) {
        int maxIdx = 0;
        float maxConfidence = confidenceList.get(0);
        for (int i = 0; i < confidenceList.size(); i++) {
            if (confidenceList.get(i) > maxConfidence) {
                maxConfidence = confidenceList.get(i);
                maxIdx = i;
            }
        }

        if (maxConfidence > threshold && maxIdx == 0) {
            // 0 is the index of the "person" class in the classCanBeDetected array
            return maxIdx;
        } else { // do not detect any "person"
            return -1;
        }
    }


    public Mat handleImage(Mat image) {
        // resize image
        Imgproc.resize(image, new Mat(), size);

        // image normalization
        Mat inputBlog = Dnn.blobFromImage(image, 1.0F / 255.0F, size, new Scalar(0), false, false);

        // input image into CNN
        net.setInput(inputBlog);

        // get the name of three output layers
        List<String> ln = net.getLayerNames();
        //  get the number of three output layers
        List<Integer> outNumber = net.getUnconnectedOutLayers().toList();
        List<String> outString = new ArrayList<>();

        //  set the name
        for (int i : outNumber) {
            outString.add(ln.get(i - 1));
        }

        // the output mat list include three different mat,
        // corresponding to small objects, large objects, medium-sized objects
        List<Mat> matList = new ArrayList<>();

        // forward propagation
        net.forward(matList, outString);

        List<BBox> bboxList = new LinkedList<>();

        for (Mat output : matList) {
            processDetectedObjects(output, image, bboxList);
        }

        // carry out the maximum value suppression processing
        List<BBox> result = NMSBoxes(bboxList, NMS_THRESHOLD);

        // draw green rectangular area for the detected objects
        for (BBox bBox : result) {
            ImageUtil.drawRectangle(bBox.getClassName(), bBox.getConfidence(), image, bBox.getRect());
        }

        return image; // return the detected image
    }


    // carry out the maximum value suppression processing
    public List<BBox> NMSBoxes(List<BBox> bBoxes, float nmsThreshold) {

        List<BBox> result = new ArrayList<>();

        if (Objects.isNull(bBoxes) || bBoxes.size() < 1) {
            return result;
        }

        bBoxes.sort(new Comparator<BBox>() {
            @Override
            public int compare(BBox o1, BBox o2) {
                return Float.compare(o2.getConfidence(), o1.getConfidence());
            }
        });


        int updated_size = bBoxes.size();

        int i = 0;
        result.add(bBoxes.get(i));
        for (int j = i + 1; j < updated_size; j++) {
            float iou = getIouValue(bBoxes.get(i).getRect(), bBoxes.get(j).getRect());
            if (iou <= nmsThreshold) {
                result.add(bBoxes.get(j));
            }
        }
        return result;
    }

    // calculate the Intersection-over-Union(IoU) of two rectangles
    public float getIouValue(Rect rect1, Rect rect2) {
        int xx1, yy1, xx2, yy2;
        xx1 = Math.max(rect1.x, rect2.x);
        yy1 = Math.max(rect1.y, rect2.y);
        xx2 = Math.min(rect1.x + rect1.width - 1, rect2.x + rect2.width - 1);
        yy2 = Math.min(rect1.y + rect1.height - 1, rect2.y + rect2.height - 1);

        int intersectionWidth, intersectionHeight;

        intersectionWidth = Math.max(0, xx2 - xx1 + 1);
        intersectionHeight = Math.max(0, yy2 - yy1 + 1);

        float intersectionArea, union_area, iou;
        intersectionArea = intersectionWidth * intersectionHeight;
        union_area = rect1.width * rect1.height + rect2.width * rect2.height - intersectionArea;
        iou = intersectionArea / union_area;
        return iou;
    }

}
