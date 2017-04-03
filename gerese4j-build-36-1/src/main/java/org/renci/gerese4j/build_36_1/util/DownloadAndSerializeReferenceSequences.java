package org.renci.gerese4j.build_36_1.util;

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
import java.util.concurrent.Executors;
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

    private static final BuildType buildType = BuildType.BUILD_36_1;

    public DownloadAndSerializeReferenceSequences() {
        super();
    }

    @Override
    public void run() {
        logger.debug("ENTERING run()");

        File serializationDir = new File("src/main/resources/org/renci/gerese4j/build_36_1");
        if (!serializationDir.exists()) {
            serializationDir.mkdirs();
        }

        File serIndexFile = new File(serializationDir, "refseq_index.ser");

        Set<String> referenceSequenceIndexSet = new HashSet<>();
        Map<String, ReferenceSequence> fastaSequenceMap = new HashMap<>();

        long start = System.currentTimeMillis();

        try {

            File tmpDir = new File(System.getProperty("java.io.tmpdir"), buildType.getVersion());
            if (!tmpDir.exists()) {
                tmpDir.mkdirs();
            }

            File readme = FTPFactory.ncbiDownload(tmpDir, String.format("/genomes/H_sapiens/ARCHIVE/BUILD.%s", buildType.getVersion()),
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

            if (!shortName.equals(buildType.getVersion())) {
                logger.error("buildType & shortName don't match up");
                return;
            }

            List<File> pulledFiles = FTPFactory.ncbiDownloadFiles(tmpDir,
                    String.format("/genomes/H_sapiens/ARCHIVE/BUILD.%s/Assembled_chromosomes", buildType.getVersion()), "hs_ref_",
                    ".fa.gz");
            pulledFiles.addAll(FTPFactory.ncbiDownloadFiles(tmpDir,
                    String.format("/genomes/H_sapiens/ARCHIVE/BUILD.%s/CHR_Un", buildType.getVersion()), "hs_ref_", ".fa.gz"));
            pulledFiles.addAll(FTPFactory.ncbiDownloadFiles(tmpDir,
                    String.format("/genomes/H_sapiens/ARCHIVE/BUILD.%s/CHR_MT", buildType.getVersion()), "hs_ref_", ".fa.gz"));

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
                            referenceSequenceIndexSet.add(genomeRefAccession);
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

            try (FileOutputStream fos = new FileOutputStream(serIndexFile);
                    GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                    ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
                oos.writeObject(referenceSequenceIndexSet);
                logger.info("serialized index file to: {}", serIndexFile.getAbsolutePath());
            }

            for (String key : fastaSequenceMap.keySet()) {
                File serFile = new File(serializationDir, String.format("%s.ser", key));
                try (FileOutputStream fos = new FileOutputStream(serFile);
                        GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                        ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
                    oos.writeObject(fastaSequenceMap.get(key));
                    logger.info("serialized ReferenceSequence to: {}", serFile.getAbsolutePath());
                }
            }

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