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

    // GUI initiate
    public void initGUI() {
        jFrame = new JFrame("Person Detecting System");
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setSize(1000, 800);
        jFrame.setLocationRelativeTo(null);
        jFrame.setLayout(null);

        imageLabel = new JLabel();

        JButton jButton = new JButton("Start");

        JButton fileButton = new JButton("Select File");


        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                jfc.setFileFilter(new FileFilter());
                jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                jfc.showDialog(new JLabel(), "select");
                File file = jfc.getSelectedFile();
                filePath = file.getAbsolutePath();
            }
        });

        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (filePath == null) {
                    JOptionPane.showMessageDialog(new Panel(), "please select a file", "message", JOptionPane.WARNING_MESSAGE);
                } else {
                    new Thread(new Runnable() {
                        public void run() {
                            check();
                        }
                    }).start();

                }
            }
        });

        imageLabel.setBounds(100, 100, 750, 600);

        imageLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        fileButton.setBounds(550, 30, 100, 30);

        jButton.setBounds(350, 30, 100, 30);
        jFrame.add(imageLabel);
        jFrame.add(jButton);
        jFrame.add(fileButton);
        jFrame.setVisible(true);

    }

    // check method
    private void check() {
        // load library
        System.load("path/to/opencv_java452.dll");
        System.load("path/to/opencv_videoio_ffmpeg452_64.dll");

        VideoHandleService videoHandleService = new VideoHandleService();
        VideoCapture capture = new VideoCapture();
        String videoName = filePath;

        capture.open(videoName);

        Mat video = new Mat();
        int index = 0;
        int i = 0;

        // get the number of frame in the video
        double v = capture.get(Videoio.CAP_PROP_FRAME_COUNT);

        while (capture.isOpened()) {
            capture.read(video);

            // if the video process is done
            if (video.cols() == 0 || video.rows() == 0 || index == v - 1) {
                filePath = null;
                index++;
                capture.release();
                continue;
            }

            Mat result = videoHandleService.handleImage(video);

            BufferedImage tempImage = ImageUtil.conver2Image(result);
            ImageIcon imageIcon = new ImageIcon(tempImage, "result");
            imageIcon.setImage(imageIcon.getImage().getScaledInstance(750, 600, Image.SCALE_DEFAULT));

            imageLabel.setIcon(imageIcon);

            i++;
            index++;
        }

        capture.release();
    }
}
