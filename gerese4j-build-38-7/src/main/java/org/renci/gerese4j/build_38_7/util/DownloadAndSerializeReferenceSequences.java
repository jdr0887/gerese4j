package org.renci.gerese4j.build_38_7.util;

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
import org.renci.gerese4j.core.ReferenceSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadAndSerializeReferenceSequences implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DownloadAndSerializeReferenceSequences.class);

    private static final BuildType buildType = BuildType.BUILD_38_7;

    public DownloadAndSerializeReferenceSequences() {
        super();
    }

    @Override
    public void run() {
        logger.debug("ENTERING run()");

        File serializationDir = new File("src/main/resources/org/renci/gerese4j/build_38_7");
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

            File tmpDir = new File(System.getProperty("java.io.tmpdir"), buildType.getVersion());
            if (!tmpDir.exists()) {
                tmpDir.mkdirs();
            }
            File readme = FTPFactory.ncbiDownload(tmpDir, "/genomes/H_sapiens", "README_CURRENT_RELEASE");
            logger.info("Downloaded readme to: {}", readme.getAbsolutePath());

            List<String> lines = FileUtils.readLines(readme, "UTF-8");

            String build = null;
            String patch = null;

            Optional<String> assemblyNameLine = lines.stream().filter(a -> a.startsWith("ASSEMBLY NAME")).findAny();
            if (assemblyNameLine.isPresent()) {
                String[] lineSplit = assemblyNameLine.get().replace("ASSEMBLY NAME:", "").replace("GRCh", "").trim().split("\\.");
                build = lineSplit[0];
                patch = lineSplit[1].contains("p") ? lineSplit[1].replaceAll("p", "") : lineSplit[1];
                if (StringUtils.isEmpty(patch)) {
                    patch = "1";
                }
            }

            if (StringUtils.isEmpty(build) || StringUtils.isEmpty(patch)) {
                logger.error("build or patch was empty");
                return;
            }

            String shortName = String.format("%s.%s", build, patch);

            if (!shortName.equals(buildType.getVersion())) {
                logger.error("buildType & shortName don't match up");
                return;
            }

            List<File> pulledFiles = FTPFactory.ncbiDownloadFiles(tmpDir, "/genomes/H_sapiens/Assembled_chromosomes/seq", "hs_ref_",
                    ".fa.gz");
            pulledFiles.addAll(FTPFactory.ncbiDownloadFiles(tmpDir, "/genomes/H_sapiens/CHR_Un", "hs_ref_", ".fa.gz"));
            pulledFiles.addAll(FTPFactory.ncbiDownloadFiles(tmpDir, "/genomes/H_sapiens/CHR_MT", "hs_ref_", ".fa.gz"));

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
                            String[] idParts = line.split("\\|");
                            genomeRefAccession = idParts[3];
                            logger.info(genomeRefAccession);
                            indices.add(genomeRefAccession);
                            headers.put(genomeRefAccession, line.replaceAll(">", "").trim());
                            if (fastaSequence != null) {
                                fastaSequenceMap.put(genomeRefAccession, fastaSequence);
                            }
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

            pulledFiles.forEach(a -> a.delete());

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

    public static void main(String[] args) {
        Executors.newSingleThreadExecutor().execute(new DownloadAndSerializeReferenceSequences());
    }
}