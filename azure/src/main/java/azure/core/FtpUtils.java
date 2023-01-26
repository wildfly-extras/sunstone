package azure.core;


import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;

public class FtpUtils {
    /**
     * Clean a directory by delete all its sub files and
     * sub directories recursively.
     */
    public static void cleanDirectory(FTPClient ftpClient, String dir) throws IOException {
        removeDirectory(ftpClient, dir, "", true);
    }
    /**
     * Removes a non-empty directory by delete all its sub files and
     * sub directories recursively. And finally remove the directory if requested.
     */
    public static void removeDirectory(FTPClient ftpClient, String parentDir,
                                       String currentDir, boolean removeOnlyContent) throws IOException {
        String dirToList = parentDir;
        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir;
        }

        FTPFile[] subFiles = ftpClient.listFiles(dirToList);

        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile aFile : subFiles) {
                String currentFileName = aFile.getName();
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    // skip parent directory and the directory itself
                    continue;
                }
                String filePath = parentDir + "/" + currentDir + "/"
                        + currentFileName;
                if (currentDir.equals("")) {
                    filePath = parentDir + "/" + currentFileName;
                }

                if (aFile.isDirectory()) {
                    // remove the sub directory
                    removeDirectory(ftpClient, dirToList, currentFileName, false);
                } else {
                    // delete the file
                    boolean deleted = ftpClient.deleteFile(filePath);
                    if (deleted) {
                        System.out.println("DELETED the file: " + filePath);
                    } else {
                        System.out.println("CANNOT delete the file: "
                                + filePath);
                    }
                }
            }

            // finally, remove the directory itself
            if (!removeOnlyContent) {
                boolean removed = ftpClient.removeDirectory(dirToList);
                if (removed) {
                    System.out.println("REMOVED the directory: " + dirToList);
                } else {
                    System.out.println("CANNOT remove the directory: " + dirToList);
                }
            }
        }
    }
}
