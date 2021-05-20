import java.io.File;


public class FileFilter extends javax.swing.filechooser.FileFilter {
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        if (f.getName().endsWith(".mp4")) {
            return true;
        } else if (f.getName().endsWith(".avi")) {
            return true;
        }
        return false;
    }
    @Override
    public String getDescription() {
        return ".mp4,.avi";
    }
}
