package org.renci.gerese4j.core.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.renci.gerese4j.core.BuildType;
import org.renci.gerese4j.core.FTPFactory;
import org.renci.gerese4j.core.GeReSe4jException;
import org.renci.gerese4j.core.ReferenceSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeReSe4jBuild_37_3 extends AbstractGeReSe4jBuild {

    private static final Logger logger = LoggerFactory.getLogger(GeReSe4jBuild_37_3.class);

    private static GeReSe4jBuild_37_3 instance;

    public static GeReSe4jBuild_37_3 getInstance() {
        if (instance == null) {
            instance = new GeReSe4jBuild_37_3();
        }
        return instance;
    }

    public static GeReSe4jBuild_37_3 getInstance(File gerese4jHome) {
        if (instance == null) {
            instance = new GeReSe4jBuild_37_3(gerese4jHome);
        }
        return instance;
    }

    private GeReSe4jBuild_37_3() {
        super();
    }

    private GeReSe4jBuild_37_3(File gerese4jHome) {
        super(gerese4jHome);
    }

    @Override
    public BuildType getBuild() {
        return BuildType.BUILD_37_3;
    }

    @Override
    public void serialize() throws GeReSe4jException {
        init();

        File serializationDir = new File(gerese4jHome, getBuild().toString());
        if (!serializationDir.exists()) {
            serializationDir.mkdirs();
        }

        File indicesFile = new File(serializationDir, "indices.ser");
        File headersFile = new File(serializationDir, "headers.ser");

        Set<String> indices = new HashSet<>();
        Map<String, String> headers = new HashMap<>();
        Map<String, ReferenceSequence> fastaSequenceMap = new HashMap<>();

        long start = System.currentTimeMillis();

        try {

            File tmpDir = new File(serializationDir, "tmp");
            if (!tmpDir.exists()) {
                tmpDir.mkdirs();
            }
            File readme = FTPFactory.ncbiDownload(tmpDir, String.format("/genomes/H_sapiens/ARCHIVE/BUILD.%s", getBuild().getVersion()),
                    "README_CURRENT_BUILD");
            logger.info("Downloaded readme to: {}", readme.getAbsolutePath());

            List<String> lines = FileUtils.readLines(readme, "UTF-8");

            String build = null;
            String patch = null;

            Optional<String> buildNumberLine = lines.stream().filter(a -> a.startsWith("NCBI Build Number")).findAny();
            if (buildNumberLine.isPresent()) {
                build = buildNumberLine.get().replace("NCBI Build Number:", "").trim();
            }

            Optional<String> patchLine = lines.stream().filter(a -> a.startsWith("Version")).findAny();
            if (patchLine.isPresent()) {
                patch = patchLine.get().replace("Version:", "").trim();
            }

            if (StringUtils.isEmpty(build) || StringUtils.isEmpty(patch)) {
                logger.error("build or patch was empty");
                return;
            }

            String shortName = String.format("%s.%s", build, patch);

            if (!shortName.equals(getBuild().getVersion())) {
                logger.error("buildType & shortName don't match up");
                return;
            }

            List<File> pulledFiles = FTPFactory.ncbiDownloadFiles(tmpDir,
                    String.format("/genomes/H_sapiens/ARCHIVE/BUILD.%s/Assembled_chromosomes/seq", getBuild().getVersion()), "hs_ref_",
                    ".fa.gz");
            // pulledFiles.addAll(FTPFactory.ncbiDownloadFiles(tmpDir,
            // String.format("/genomes/H_sapiens/ARCHIVE/BUILD.%s/CHR_Un", getBuild().getVersion()), "hs_ref_", ".fa.gz"));
            pulledFiles.addAll(FTPFactory.ncbiDownloadFiles(tmpDir,
                    String.format("/genomes/H_sapiens/ARCHIVE/BUILD.%s/CHR_MT", getBuild().getVersion()), "hs_ref_", ".fa.gz"));

            for (File f : pulledFiles) {
                logger.info(f.getName());

                try (FileInputStream fis = new FileInputStream(f);
                        GZIPInputStream gzipis = new GZIPInputStream(fis);
                        InputStreamReader decoder = new InputStreamReader(gzipis);
                        BufferedReader br = new BufferedReader(decoder)) {

                    String genomeRefAccession = null;
                    ReferenceSequence fastaSequence = null;
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith(">")) {
                            if (fastaSequence != null) {
                                fastaSequenceMap.put(genomeRefAccession, fastaSequence);
                            }
                            String[] idParts = line.split("\\|");
                            genomeRefAccession = idParts[3];
                            logger.info(genomeRefAccession);
                            indices.add(genomeRefAccession);
                            headers.put(genomeRefAccession, line.replaceAll(">", "").trim());
                            fastaSequence = new ReferenceSequence(line.replaceAll(">", "").trim());
                        } else {
                            fastaSequence.getSequence().append(line);
                        }
                    }

                    if (fastaSequence != null) {
                        fastaSequenceMap.put(genomeRefAccession, fastaSequence);
                    }

                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

            }

            // pulledFiles.forEach(a -> a.delete());

            try (FileOutputStream fos = new FileOutputStream(indicesFile);
                    GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                    ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
                oos.writeObject(indices);
                logger.info("serialized indices file to: {}", indicesFile.getAbsolutePath());
            }

            try (FileOutputStream fos = new FileOutputStream(headersFile);
                    GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                    ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
                oos.writeObject(headers);
                logger.info("serialized headers file to: {}", headersFile.getAbsolutePath());
            }

            ExecutorService es = Executors.newFixedThreadPool(2);
            for (String key : fastaSequenceMap.keySet()) {
                es.submit(() -> {
                    File serFile = new File(serializationDir, String.format("%s.ser", key));
                    try (FileOutputStream fos = new FileOutputStream(serFile);
                            GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                            ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
                        oos.writeObject(fastaSequenceMap.get(key));
                        logger.info("serialized ReferenceSequence to: {}", serFile.getAbsolutePath());
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                });
            }
            es.shutdown();
            es.awaitTermination(20L, TimeUnit.MINUTES);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        long end = System.currentTimeMillis();
        logger.info("duration = {}", String.format("%d seconds", (end - start) / 1000));

    }

}
