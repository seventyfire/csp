package com.culiu.core;

import com.culiu.service.VideoHandleService;
import com.culiu.util.FileFilter;
import com.culiu.util.ImageUtil;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

public class CheckPerson {

    private JFrame jFrame;

    private JLabel imageLabel;

    private String filePath;
    //    图形页面初始化
    public void initGUI(){
//        页面框
        jFrame = new JFrame("图形检测页面！");
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setSize(1000,800);
        jFrame.setLocationRelativeTo(null);
        jFrame.setLayout(null);
//        展示框
        imageLabel = new JLabel();
//        检测按钮
        JButton jButton = new JButton("开始检测");
//        文件选择按钮
        JButton fileButton = new JButton("文件选择");
//        添加文件选择框事件
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc=new JFileChooser();
                jfc.setFileFilter(new FileFilter());
                jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES );
                jfc.showDialog(new JLabel(), "选择");
                File file=jfc.getSelectedFile();
                filePath = file.getAbsolutePath();
            }
        });
//        添加检测事件
        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                未选择视频弹出提示
                if (filePath==null){
                    JOptionPane.showMessageDialog(new Panel(),"请先选择文件","提示",JOptionPane.WARNING_MESSAGE);
                }else{
//                    当执行消耗时间较大的任务，需要另开一个线程
                    new Thread(new Runnable() {
                        public void run() {
                            check();
                        }
                    }).start();

                }
            }
        });
//        设置位置
        imageLabel.setBounds(100,100,750,600);
//        设置边框
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//        设置位置
        fileButton.setBounds(550,30,100,30);
//        设置边框
        jButton.setBounds(350,30,100,30);
        jFrame.add(imageLabel);
        jFrame.add(jButton);
        jFrame.add(fileButton);
        jFrame.setVisible(true);

    }
    //    检测方法
    private void check(){
//        加载opencv库
        String path = "D:/play_code_app/opencv/build/java/x64/opencv_java452.dll";
//        String path = "/face/opencv/opencv-4.5.2/build/lib/libopencv_java452.so";
        System.load(path);
//        加载视频处理库
        System.load("D:/mode/opencv_videoio_ffmpeg452_64.dll");
        VideoHandleService videoHandleService = new VideoHandleService();
//      Mat mat = imread("C:/Users/XBin/Desktop/img/moto.jpg");
        VideoCapture capture = new VideoCapture();
        long start1 = System.currentTimeMillis();
//        String videoname = "C:\\Users\\XBin\\Desktop\\EIR\\video\\ll.mp4";
        String videoname = filePath;
//        String videoname = "/home/pi/dll/dianche.mp4";
        capture.open(videoname);
        long end1 = System.currentTimeMillis();
        System.out.println("视频读取时间："+(end1-start1));
//        Size size = new Size(capture.get(Videoio.CAP_PROP_FRAME_WIDTH), capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
//        VideoWriter videoWriter = new VideoWriter("C:/Users/XBin/Desktop/EIR/video/result.avi",VideoWriter.fourcc('D', 'I','V','X'),15,size,true);
        Mat video = new Mat();
        int index = 0;
        int i=0;
//        获取视频帧数
        double v = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        System.out.println(v);
        while (capture.isOpened()){

            capture.read(video);
//            如果视频帧数完了 进入结束方法
            if (video.cols()==0||video.rows()==0||index==v-1){
                filePath=null;
                index++;
                capture.release();
//                videoWriter.release();
                continue;
            }
            long start = System.currentTimeMillis();
//          Swing显示图片
            Mat result = videoHandleService.handleImage(video);
//            把mat转换为BufferedImage
            BufferedImage tempImage  = ImageUtil.conver2Image(result);
            ImageIcon imageIcon = new ImageIcon(tempImage,"检测结果");
            imageIcon.setImage(imageIcon.getImage().getScaledInstance(750,600, Image.SCALE_DEFAULT));
//            显示
            imageLabel.setIcon(imageIcon);
//            jFrame.pack();
            long end = System.currentTimeMillis();
            System.out.println("图片检测到第"+i+"帧  消耗时间："+(end-start)+"毫秒");
            i++;
//            HighGui.imshow("人脸识别", result);//3 显示图像
//            index = HighGui.waitKey(100);//4 获取键盘输入
//            if (index == 27) {//5 如果是 Esc 则退出
//                capture.release();
//                videoWriter.release();
//                return;
//            }
//            videoWriter.write(result);
//            if (index==2000){
//                capture.release();
//                videoWriter.release();
//            }
//            System.out.println(index);
            index++;
        }
        capture.release();
//        videoWriter.release();
    }
}
