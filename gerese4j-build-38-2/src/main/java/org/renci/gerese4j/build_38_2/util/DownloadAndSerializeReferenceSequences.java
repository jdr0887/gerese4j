package org.renci.gerese4j.build_38_2.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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

public class DownloadAndSerializeReferenceSequences implements Callable<File> {

    private static final Logger logger = LoggerFactory.getLogger(DownloadAndSerializeReferenceSequences.class);

    public DownloadAndSerializeReferenceSequences() {
        super();
    }

    @Override
    public File call() throws Exception {
        logger.debug("ENTERING execute()");

        File serializationDir = new File("src/main/resources/org/renci/gerese4j");
        if (!serializationDir.exists()) {
            serializationDir.mkdirs();
        }

        File serFile = new File(serializationDir, String.format("reference_sequences_%s.ser", BuildType.BUILD_38_2.getVersion()));

        Map<String, ReferenceSequence> fastaSequenceMap = new HashMap<>();

        long start = System.currentTimeMillis();

        try {

            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
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
                return null;
            }

            String shortName = String.format("%s.%s", build, patch);

            File fastaOutputDir = new File(tmpDir, shortName);
            if (!fastaOutputDir.exists()) {
                fastaOutputDir.mkdirs();
            }

            List<File> pulledFiles = FTPFactory.ncbiDownloadFiles(fastaOutputDir, "/genomes/H_sapiens/Assembled_chromosomes/seq", "hs_ref_",
                    ".fa.gz");
            pulledFiles.addAll(FTPFactory.ncbiDownloadFiles(fastaOutputDir, "/genomes/H_sapiens/CHR_Un", "hs_ref_", ".fa.gz"));
            pulledFiles.addAll(FTPFactory.ncbiDownloadFiles(fastaOutputDir, "/genomes/H_sapiens/CHR_MT", "hs_ref_", ".fa.gz"));

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

            try (FileOutputStream fos = new FileOutputStream(serFile);
                    GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                    ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
                oos.writeObject(fastaSequenceMap);
                logger.info("serialized Map<String, ReferenceSequence> to: {}", serFile.getAbsolutePath());
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        long end = System.currentTimeMillis();
        logger.info("duration = {}", String.format("%d seconds", (end - start) / 1000));

        return serFile;
    }

    public static void main(String[] args) {
        try {
            DownloadAndSerializeReferenceSequences callable = new DownloadAndSerializeReferenceSequences();
            File serFile = Executors.newSingleThreadExecutor().submit(callable).get();
            logger.info("serialized to: {}", serFile.getAbsolutePath());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}