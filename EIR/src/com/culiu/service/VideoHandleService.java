package com.culiu.service;

import com.culiu.pojo.BBox;
import com.culiu.util.ImageUtil;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Xbin
 * @date 2021-05-09 13:35
 * @email liuhongbindeemail@gmail.com
 */

public class VideoHandleService {

    private final static float CONF_THRESHOLD = 0.5f; //置信度

    private final static float NMS_THRESHOLD = 0.1f; //重合度  iou阙值

    //       static String cfg = "/home/pi/dll/yolo-fastest-1.1.cfg";
//       static String model = "/home/pi/dll/yolo-fastest-1.1.weights";
    //      获取yolo的模型
    static String cfg = "yolov3.cfg";
    static String model = "yolov3.weights";
    //   String modelNNX =  "D:/mode/yolov5s.onnx";
    static Net net = Dnn.readNetFromDarknet(cfg, model);

//    private static Net net = Dnn.readNetFromDarknet(cfg, model);

    //      图片大小
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
    };  // Classes that the algorithm can detect

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

            int maxConfidenceIndex = findMaxConfidence(confidenceList,  0.5f);

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

    public int findMaxConfidence(ArrayList<Float> confidenceList, float threshold){
        int maxIdx = 0;
        float maxConfidence = confidenceList.get(0);
        for (int i = 0; i < confidenceList.size(); i++){
            if (confidenceList.get(i) > maxConfidence){
                maxConfidence = confidenceList.get(i);
                maxIdx = i;
            }
        }

        if (maxConfidence > threshold && maxIdx == 0) {
            // 0 is the index of the "person" class in the classCanBeDetected array
            return maxIdx;
        } else{ // do not detect any "person"
            return -1;
        }
    }

    /**
     * 图片识别处理
     *
     * @param image
     * @return 返回处理好的图片
     */
    public Mat handleImage(Mat image) {
//      统一图片大小
        Imgproc.resize(image, new Mat(), size);
//      图片预处理  图片归一化
        Mat inputBlog = Dnn.blobFromImage(image, 1.0F / 255.0F, size, new Scalar(0), false, false);
//      图片输入模型进行识别
        net.setInput(inputBlog);
//      获取输出层
//      获取所有的层
        List<String> ln = net.getLayerNames();
//      获取三个输出层的序号。
        List<Integer> outNumber = net.getUnconnectedOutLayers().toList();
//      临时存储
        List<String> outString = new ArrayList<>();

//      根据序号获取名称
        for (int i : outNumber) {
            outString.add(ln.get(i - 1));
        }

        // the output mat list include three different mat,
        // corresponding to small objects, large objects, medium-sized objects
        List<Mat> matList = new ArrayList<>();

        net.forward(matList, outString);


        List<BBox> bboxList = new LinkedList<>();

        for (Mat output : matList) {
            processDetectedObjects(output, image, bboxList);
        }

        List<BBox> result = NMSBoxes(bboxList, CONF_THRESHOLD, NMS_THRESHOLD);

        for (BBox bBox : result) {
            ImageUtil.drawPred_(bBox.getClassName(), bBox.getConfidence(), image, bBox.getRect());
        }

        return image;
    }


    /**
     * 极大值抑制
     *
     * @param bBoxes
     * @param confThreshold
     * @param nmsThreshold
     * @return
     */
    public List<BBox> NMSBoxes(List<BBox> bBoxes, float confThreshold, float nmsThreshold) {

        List<BBox> result = new ArrayList<>();

        if (Objects.isNull(bBoxes) || bBoxes.size() < 1) {
            return result;
        }
        // 新建一个List 存放BBox的对象

        // 循环向bBoxes里添加值：Rect(Point,Point) float confidence, int index
//        for(int i=0; i<boxes.size(); i++){
//            BBox bbox = new BBox();
//            bbox.setBox( boxes.get(i));
//            bbox.setConfidence( confidences.get(i));
//            bbox.setIndex(i);
//            bBoxes.add(bbox);
//        }
//      快速排序  根据confidence的属性值 从大到小
        bBoxes.sort(new Comparator<BBox>() {
            @Override
            public int compare(BBox o1, BBox o2) {
                if (o1.getConfidence() > o2.getConfidence()) {
                    return -1;
                } else if (o1.getConfidence() < o2.getConfidence()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
//        找出可信度最高的框
        // 排序算法 根据confidence的属性值，降序排列List bBoxes
//        for (int i = 0; i < bBoxes.size(); i++) {
//            for (int j = 0; j < bBoxes.size() - i - 1; j++) {
//                if (bBoxes.get(j).getConfidence() < bBoxes.get(j + 1).getConfidence()) {
//
//                    BBox temp = bBoxes.get(j);
//                    bBoxes.set(j, bBoxes.get(j + 1));
//                    bBoxes.set(j+1, temp);
//                }
//            }
//        }

//        for(int i=0; i<bBoxes.size(); i++){
//            System.out.println("可信度："+bBoxes.get(i).getConfidence());
//        }

        int updated_size = bBoxes.size();

//        System.out.println("bBoxes.size(): " +bBoxes.size());


        // 循环删除
        // 比较第一个值（ 最大值）与后面每一个值的交并比
        // 如果iou>阈值，删除这个框,更改bBoxes的长度
        int i = 0;
        result.add(bBoxes.get(i));
        for (int j = i + 1; j < updated_size; j++) {
            float iou = getIouValue(bBoxes.get(i).getRect(), bBoxes.get(j).getRect());
//                System.out.println("iou="+iou);
            if (iou <= nmsThreshold) {
                result.add(bBoxes.get(j));
            }
        }
//        System.out.println("result"+result.size());
        return result;
    }

    /**
     * 获取两个图片的重合度的值
     *
     * @param rect1 图片一的面积的范围
     * @param rect2 图片二的面积范围
     * @return
     */
    public float getIouValue(Rect rect1, Rect rect2) {
        int xx1, yy1, xx2, yy2;
        xx1 = Math.max(rect1.x, rect2.x);
        yy1 = Math.max(rect1.y, rect2.y);
        xx2 = Math.min(rect1.x + rect1.width - 1, rect2.x + rect2.width - 1);
        yy2 = Math.min(rect1.y + rect1.height - 1, rect2.y + rect2.height - 1);

        int insection_width, insection_height;

        insection_width = Math.max(0, xx2 - xx1 + 1);
        insection_height = Math.max(0, yy2 - yy1 + 1);

        float insection_area, union_area, iou;
//        重叠面积
        insection_area = insection_width * insection_height;
        union_area = rect1.width * rect1.height + rect2.width * rect2.height - insection_area;
        iou = insection_area / union_area;
        return iou;
    }

}
