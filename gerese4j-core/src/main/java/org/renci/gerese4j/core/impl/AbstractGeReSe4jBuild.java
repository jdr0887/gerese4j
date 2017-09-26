package org.renci.gerese4j.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.renci.gerese4j.core.GeReSe4jBuild;
import org.renci.gerese4j.core.GeReSe4jException;
import org.renci.gerese4j.core.ReferenceSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGeReSe4jBuild implements GeReSe4jBuild {

    private static final Logger logger = LoggerFactory.getLogger(AbstractGeReSe4jBuild.class);

    private ConcurrentHashMap<String, ReferenceSequence> referenceSequenceCache;

    private Set<String> indices;

    private Map<String, String> headers;

    protected File gerese4jHome;

    public AbstractGeReSe4jBuild() {
        super();
        this.referenceSequenceCache = new ConcurrentHashMap<>();
        this.indices = new HashSet<>();
        this.headers = new HashMap<>();
    }

    public AbstractGeReSe4jBuild(File gerese4jHome) {
        super();
        this.gerese4jHome = gerese4jHome;
        this.referenceSequenceCache = new ConcurrentHashMap<>();
        this.indices = new HashSet<>();
        this.headers = new HashMap<>();
    }

    @Override
    public void init() throws GeReSe4jException {
        logger.debug("ENTERING init()");

        String gerese4jHomeFromEnv = System.getenv("GERESE4j_HOME");

        if (StringUtils.isEmpty(gerese4jHomeFromEnv) && gerese4jHome == null) {
            throw new GeReSe4jException("GERESE4j_HOME not set in env and gerese4jHome is null");
        }

        if (StringUtils.isNotEmpty(gerese4jHomeFromEnv) && gerese4jHome == null) {
            this.gerese4jHome = new File(gerese4jHomeFromEnv);
        }

        logger.debug("gerese4jHome.getAbsolutePath(): {}", gerese4jHome.getAbsolutePath());
        if (gerese4jHome != null && gerese4jHome.exists()) {
            File buildDir = new File(gerese4jHome, getBuild().toString());
            if (indices.isEmpty()) {
                logger.info("loading indices");
                File indicesFile = new File(buildDir, "indices.ser");
                if (indicesFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(indicesFile);
                            GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue());
                            ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                        indices.addAll((Set<String>) ois.readObject());
                        logger.debug("indices.size(): {}", indices.size());
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }

            if (headers.isEmpty()) {
                logger.info("loading headers");
                File headersFile = new File(buildDir, "headers.ser");
                if (headersFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(headersFile);
                            GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue());
                            ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                        headers.putAll((Map<String, String>) ois.readObject());
                        logger.debug("headers.size(): {}", headers.size());
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }

        }

    }

    @Override
    public Set<String> getIndices() throws GeReSe4jException {
        init();
        return indices;
    }

    @Override
    public String getHeader(String accession) throws GeReSe4jException {
        init();
        if (!headers.containsKey(accession)) {
            throw new GeReSe4jException("No accession found");
        }
        return headers.get(accession);
    }

    @Override
    public Map<String, ReferenceSequence> getReferenceSequenceCache() {
        return referenceSequenceCache;
    }

    @Override
    public ReferenceSequence getReferenceSequence(String accession, boolean cache) throws GeReSe4jException {
        logger.debug("ENTERING getReferenceSequence(String, boolean)");
        init();

        if (!indices.contains(accession)) {
            throw new GeReSe4jException("No accession found");
        }

        File buildDir = new File(gerese4jHome, getBuild().toString());
        File serializationFile = new File(buildDir, String.format("%s.ser", accession));

        ReferenceSequence referenceSequence = null;

        if (cache) {

            if (!this.referenceSequenceCache.containsKey(accession)) {
                try (FileInputStream fis = new FileInputStream(serializationFile);
                        GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue());
                        ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                    this.referenceSequenceCache.put(accession, (ReferenceSequence) ois.readObject());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            referenceSequence = this.referenceSequenceCache.get(accession);
        } else {

            try (FileInputStream fis = new FileInputStream(serializationFile);
                    GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue());
                    ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                referenceSequence = (ReferenceSequence) ois.readObject();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        }

        return referenceSequence;
    }

    @Override
    public ReferenceSequence getReferenceSequence(String accession) throws GeReSe4jException {
        logger.debug("ENTERING getReferenceSequence(String)");
        return getReferenceSequence(accession, true);
    }

    @Override
    public String getBase(String accession, int idx, boolean zeroBased, boolean cache) throws GeReSe4jException {
        logger.debug("ENTERING getBase()");
        init();

        if (!indices.contains(accession)) {
            throw new GeReSe4jException("No accession found");
        }

        File buildDir = new File(gerese4jHome, getBuild().toString());
        File serializationFile = new File(buildDir, String.format("%s.ser", accession));

        ReferenceSequence referenceSequence = null;

        if (cache) {

            if (!this.referenceSequenceCache.containsKey(accession)) {
                try (FileInputStream fis = new FileInputStream(serializationFile);
                        GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue());
                        ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                    this.referenceSequenceCache.put(accession, (ReferenceSequence) ois.readObject());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            referenceSequence = this.referenceSequenceCache.get(accession);

        } else {

            try (FileInputStream fis = new FileInputStream(serializationFile);
                    GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue());
                    ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                referenceSequence = (ReferenceSequence) ois.readObject();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        }

        if (zeroBased) {
            return referenceSequence.getSequence().substring(idx - 1, idx);
        } else {
            return referenceSequence.getSequence().substring(idx, idx + 1);
        }
    }

    @Override
    public String getBase(String accession, int idx, boolean zeroBased) throws GeReSe4jException {
        logger.debug("ENTERING getBase(String, int, boolean)");
        return getBase(accession, idx, zeroBased, true);
    }

    @Override
    public String getRegion(String accession, Range<Integer> range, boolean zeroBased, boolean cache) throws GeReSe4jException {
        logger.debug("ENTERING getRegion(String, Range<Integer>, boolean, boolean)");
        init();

        if (!indices.contains(accession)) {
            throw new GeReSe4jException("No accession found");
        }

        File buildDir = new File(gerese4jHome, getBuild().toString());
        File serializationFile = new File(buildDir, String.format("%s.ser", accession));

        ReferenceSequence referenceSequence = null;

        if (cache) {

            if (!this.referenceSequenceCache.containsKey(accession)) {
                try (FileInputStream fis = new FileInputStream(serializationFile);
                        GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue());
                        ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                    this.referenceSequenceCache.put(accession, (ReferenceSequence) ois.readObject());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            referenceSequence = this.referenceSequenceCache.get(accession);

        } else {

            try (FileInputStream fis = new FileInputStream(serializationFile);
                    GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue());
                    ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                referenceSequence = (ReferenceSequence) ois.readObject();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        }

        if (zeroBased) {
            return referenceSequence.getSequence().substring(range.getMinimum() - 1, range.getMaximum() - 1);
        } else {
            return referenceSequence.getSequence().substring(range.getMinimum(), range.getMaximum());
        }
    }

    @Override
    public String getRegion(String accession, Range<Integer> range, boolean zeroBased) throws GeReSe4jException {
        logger.debug("ENTERING getRegion(String, Range<Integer>, boolean)");
        return getRegion(accession, range, zeroBased, true);
    }

}
