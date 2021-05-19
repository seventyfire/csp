package com.culiu.util;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.rectangle;

public class ImageUtil {
//  带文字的框框
    public static void drawPred_(String className, float confidence, Mat im, Rect rect) {
        String text;
        Point br = rect.br(); //右下角
        Point tl = rect.tl();// 左上角

        double x = tl.x; // p1 的 x 坐标
        double y = tl.y; // p1 的 y 坐标

        // 下面加if语句只是为了区分人和其他类别的不同颜色，改成随机获取颜色也可以
        if ("person".equals(className)) {
            rectangle(im, tl, br, new Scalar(0, 0, 255), 1);
            text = className + ":" + confidence;
            Imgproc.putText(im, text, new Point(x, y - 5), Imgproc.FONT_HERSHEY_SIMPLEX, 0.3, new Scalar(0, 255, 0), 1);

        } else {
            rectangle(im, tl, br, new Scalar(0, 255, 0), 1); // 画框
            text = String.format("%s %f", className, confidence); // 标签内容
            // 把标签添加到矩形框左上
            Imgproc.putText(im, text, new Point(x, y - 5), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 255), 1);
        }


    }
}
