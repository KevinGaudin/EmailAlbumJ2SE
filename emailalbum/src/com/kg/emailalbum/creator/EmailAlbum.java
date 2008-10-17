/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kg.emailalbum.creator;

import com.kg.emailalbum.common.FileImage;
import com.kg.emailalbum.creator.ui.ImagePreview;
import com.kg.util.ImageUtil;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.ProgressMonitor;

/**
 *
 * @author gaudin
 */
public class EmailAlbum {

    private static final Dimension IMAGE_SIZE_LANDSCAPE = new Dimension(1024, 768);
    private static final Dimension IMAGE_SIZE_PORTRAIT = new Dimension(768, 1024);
    private static final String PICTURES_ARCHIVE_PATH = "com/kg/emailalbum/viewer/pictures/";
    private static final String[] VIEWER_FILES = {"com/kg/emailalbum/viewer/ui/EmailAlbum.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$1.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$2.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$3.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$4.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$5.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$6.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$7.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$8.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum.properties",
        "com/kg/emailalbum/viewer/ui/EmailAlbum_en.properties",
        "com/kg/util/ImageUtil.class",
        "com/kg/util/ImageUtil$1.class"
    };
    private static final String CONTENT_FILE = "com/kg/emailalbum/viewer/pictures/content";
    private JarOutputStream archive = null;
    private StringBuffer contentFile = new StringBuffer();
    private String archiveName = null;

    public EmailAlbum(File[] files) {
        ProgressMonitor monitor = new ProgressMonitor(null, java.util.ResourceBundle.getBundle(this.getClass().getName()).getString("monitor.title"), java.util.ResourceBundle.getBundle(this.getClass().getName()).getString("monitor.firstNote"), 0, files.length - 1);
        for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
            monitor.setProgress(i);
            File file = files[i];


            if (file.exists() && file.isFile()) {
                FileImage picture = new FileImage();
                picture.setFileName(file.getName());
                monitor.setNote(file.getName());
                try {
                    BufferedImage pic = ImageIO.read(file);
                    if (pic != null) {
                        Dimension bounds = pic.getHeight() > pic.getWidth() ? IMAGE_SIZE_PORTRAIT : IMAGE_SIZE_LANDSCAPE;
                        if (pic.getWidth() > bounds.getWidth() || pic.getHeight() > bounds.getHeight()) {
                            pic = ImageUtil.resize(pic, bounds);
                        }

                        picture.setImage(pic);
                        addToArchive(picture);
                        contentFile.append(picture.getFileName()).append("\n");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (!monitor.isCanceled()) {
            if (archive != null) {
                try {
                    monitor.setNote(java.util.ResourceBundle.getBundle(this.getClass().getName()).getString("monitor.lastNote"));
                    writeContentFile();
                    archive.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            try {
                // Operation cancelled by user
                archive.close();
                File archiveFile = new File(archiveName);
                archiveFile.delete();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
        monitor.close();
        System.exit(0);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (archive != null) {
            archive.close();
        }
    }

    private void addToArchive(FileImage picture) throws IOException {
        getArchive().putNextEntry(new ZipEntry(PICTURES_ARCHIVE_PATH + picture.getFileName()));
        ImageIO.write(picture.getImage(), "jpeg", getArchive());
        getArchive().closeEntry();
    }

    private JarOutputStream getArchive() throws IOException {
        if (archive == null) {
            Calendar today = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HHmm");
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.1");
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, com.kg.emailalbum.viewer.ui.EmailAlbum.class.getName());

            JFileChooser fileSelector = null;
            fileSelector = new JFileChooser();
            
            int usrInput = fileSelector.showSaveDialog(null);

            if(usrInput == JFileChooser.APPROVE_OPTION) {
                archiveName = fileSelector.getSelectedFile().getAbsolutePath();
                if(!archiveName.endsWith(".jar")) {
                    archiveName += ".jar";
                }
            } else {
                // User cancelled operation
                System.exit(0);
            }

            archive = new JarOutputStream(new FileOutputStream(archiveName), manifest);
            for (int i = 0; i < VIEWER_FILES.length; i++) {
                ZipEntry entry = new ZipEntry(VIEWER_FILES[i]);
                archive.putNextEntry(entry);
                InputStream viewerIS = getClass().getResourceAsStream("/" + VIEWER_FILES[i]);
                byte[] viewer = new byte[viewerIS.available()];
                int bytesread = 0;
                while (viewerIS.available() > 0) {
                    bytesread = viewerIS.read(viewer, bytesread, viewerIS.available());
                }
                archive.write(viewer);
                archive.closeEntry();
            }

        }
        return archive;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JFileChooser fileSelector = null;
        if (args.length > 0) {
            System.out.println("Using base directory : " + args[0]);
            fileSelector = new JFileChooser(args[0]);
        } else {
            fileSelector = new JFileChooser();
        }
        fileSelector.setMultiSelectionEnabled(true);
        fileSelector.addChoosableFileFilter(ImageUtil.getJpegFilter());

        fileSelector.setAccessory(new ImagePreview(fileSelector));

        int returnVal = fileSelector.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            new EmailAlbum(fileSelector.getSelectedFiles());
        }
    }

    private void writeContentFile() throws IOException {
        getArchive().putNextEntry(new ZipEntry(CONTENT_FILE));
        getArchive().write(contentFile.toString().getBytes());
        getArchive().closeEntry();
    }
}
