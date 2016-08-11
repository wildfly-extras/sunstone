package org.wildfly.extras.sunstone.api.impl;

import org.jboss.shrinkwrap.impl.base.io.tar.TarEntry;
import org.jboss.shrinkwrap.impl.base.io.tar.TarInputStream;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.ExecResult;
import org.wildfly.extras.sunstone.api.OperationNotSupportedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Class which contains useful constants, enumerations and methods for working with ssh.
 */
public class SshUtils {
    private static final Logger LOGGER = SunstoneCoreLogger.SSH;

    /**
     * Order is important.
     */
    public enum FileType {
        NA, FILE, DIRECTORY;

        public static String getShellTestStr(String filePath) {
            return "TF=\"" + filePath + "\";  test -f \"$TF\" && exit 1; test -d \"$TF\" && exit 2; exit 0";
        }

        public static FileType fromExitCode(int exitCode) throws ArrayIndexOutOfBoundsException {
            return FileType.values()[exitCode];
        }

        public static FileType fromPath(Path path) {
            if (Files.isDirectory(path)) {
                return FileType.DIRECTORY;
            } else if (Files.isRegularFile(path)) {
                return FileType.FILE;
            } else {
                return FileType.NA;
            }
        }
    }

    public static ExecResult exec(SshClient sshClient, String... command) throws OperationNotSupportedException {
        // note that this is much better than ComputeService.runScriptOnNode, which uses a monstrous script
        // that embeds the 'command' and messes up with the exit code

        StringBuilder stringBuilder = new StringBuilder();
        for (String string : command) {
            stringBuilder.append('\'').append(string).append('\'').append(" ");
        }
        ExecResponse execResponse = sshClient.exec(stringBuilder.toString());
        sshClient.disconnect();
        return new DefaultExecResult(execResponse.getOutput(), execResponse.getError(), execResponse.getExitStatus());
    }

    /**
     * Un-tars folder content from given input stream. The localTarget denotes a target folder. If strToStrip is used (not-null)
     * then this string is cropped from the tar-entries names (the ones which starts with given prefix).
     *
     * @param tis
     * @param localTarget
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static void untarFolder(TarInputStream tis, Path localTarget, String strToStrip)
            throws IOException {
        File parentFile = localTarget.toFile();
        mkdirsOrFail(parentFile);
        TarEntry tarEntry;
        while (null != (tarEntry = tis.getNextEntry())) {
            String entryName = tarEntry.getName();
            if (strToStrip != null && entryName.startsWith(strToStrip)) {
                entryName = entryName.substring(strToStrip.length());
                if (entryName.startsWith("/")) {
                    entryName = entryName.substring(1);
                }
            }
            if (entryName.isEmpty())
                continue;

            File targetFile = new File(parentFile, entryName);
            if (tarEntry.isDirectory()) {
                LOGGER.debug("Untaring folder {}", targetFile);
                mkdirsOrFail(targetFile);
            } else {
                mkdirsOrFail(targetFile.getParentFile());
                LOGGER.debug("Untaring entry {} into file {}", entryName, targetFile);
                try (FileOutputStream out = new FileOutputStream(targetFile)) {
                    byte[] rdbuf = new byte[32 * 1024];
                    int numRead;
                    while (-1 != (numRead = tis.read(rdbuf))) {
                        out.write(rdbuf, 0, numRead);
                    }
                }
            }
        }
    }

    /**
     * Un-tars single file from the given {@link TarInputStream} to given path. The target path is either localFile itself (in
     * that case the targetIsParent==false) or the parent folder (when targetIsParent==true).
     *
     * @param tis
     * @param target
     * @param targetIsParent
     * @throws IOException
     */
    public static void untarFile(TarInputStream tis, Path target, boolean targetIsParent) throws IOException {
        File targetFile = target.toFile();
        File parentFile = targetIsParent ? targetFile : targetFile.getParentFile();
        mkdirsOrFail(parentFile);
        TarEntry tarEntry;
        while (null != (tarEntry = tis.getNextEntry())) {
            if (!tarEntry.isDirectory()) {
                final String entryName = tarEntry.getName();
                if (targetIsParent) {
                    targetFile = new File(parentFile, entryName);
                }
                LOGGER.debug("Untaring entry {} into file {}", entryName, targetFile);
                try (FileOutputStream out = new FileOutputStream(targetFile)) {
                    byte[] rdbuf = new byte[32 * 1024];
                    int numRead;
                    while (-1 != (numRead = tis.read(rdbuf))) {
                        out.write(rdbuf, 0, numRead);
                    }
                }
            }
        }
    }

    /**
     * Check if given file is directory and try to create it if not. It throws {@link IOException} if the directory doesn't
     * exist and it's not possible to create it.
     *
     * @param dirFile directory to be created (may be {@code null})
     * @throws IOException Creation of directory was not successful
     */
    private static void mkdirsOrFail(File dirFile) throws IOException {
        if (dirFile == null) {
            return;
        }

        if (!dirFile.isDirectory()) {
            if (!dirFile.mkdirs()) {
                throw new IOException("Directory doesn't exist and its creation failed: " + dirFile.getAbsolutePath());
            }
        }
    }
}
