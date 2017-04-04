package org.renci.gerese4j.build_38_7;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.renci.gerese4j.core.BuildType;
import org.renci.gerese4j.core.GeReSe4jException;
import org.renci.gerese4j.core.ReferenceSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeReSe4jBuild_38_7_Manager {

    private static final Logger logger = LoggerFactory.getLogger(GeReSe4jBuild_38_7_Manager.class);

    private Set<String> indexSet = null;

    private final Map<String, ReferenceSequence> referenceSequenceCache = new HashMap<>();

    private static GeReSe4jBuild_38_7_Manager instance;

    public static GeReSe4jBuild_38_7_Manager getInstance() {
        if (instance == null) {
            instance = new GeReSe4jBuild_38_7_Manager();
            instance.init();
        }
        return instance;
    }

    private GeReSe4jBuild_38_7_Manager() {
        super();
    }

    public BuildType getBuild() {
        return BuildType.BUILD_38_7;
    }

    private void init() {
        logger.debug("ENTERING init()");
        try (InputStream is = getClass().getResourceAsStream("refseq_index.ser");
                GZIPInputStream gzipis = new GZIPInputStream(is, Double.valueOf(Math.pow(2, 16)).intValue());
                ObjectInputStream ois = new ObjectInputStream(gzipis)) {
            indexSet = (Set<String>) ois.readObject();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public Set<String> getIndexSet() {
        return indexSet;
    }

    public Map<String, ReferenceSequence> getReferenceSequenceCache() {
        return referenceSequenceCache;
    }

    public String getBase(String accession, int idx, boolean zeroBased) throws GeReSe4jException {
        logger.debug("ENTERING getBase()");

        if (CollectionUtils.isEmpty(indexSet)) {
            throw new GeReSe4jException("No indices found");
        }

        if (!indexSet.contains(accession)) {
            throw new GeReSe4jException("No accession found in indexList");
        }

        if (!this.referenceSequenceCache.containsKey(accession)) {
            try (InputStream is = getClass().getResourceAsStream(String.format("%s.ser", accession));
                    GZIPInputStream gzipis = new GZIPInputStream(is, Double.valueOf(Math.pow(2, 16)).intValue());
                    ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                this.referenceSequenceCache.put(accession, (ReferenceSequence) ois.readObject());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        ReferenceSequence referenceSequence = this.referenceSequenceCache.get(accession);

        if (zeroBased) {
            return referenceSequence.getSequence().substring(idx - 1, idx);
        } else {
            return referenceSequence.getSequence().substring(idx, idx + 1);
        }
    }

    public String getRegion(String accession, Range<Integer> range, boolean zeroBased) throws GeReSe4jException {
        logger.debug("ENTERING getRegion()");

        if (CollectionUtils.isEmpty(indexSet)) {
            throw new GeReSe4jException("No indices found");
        }

        if (!indexSet.contains(accession)) {
            throw new GeReSe4jException("No accession found in indexList");
        }

        if (!this.referenceSequenceCache.containsKey(accession)) {
            try (InputStream is = getClass().getResourceAsStream(String.format("%s.ser", accession));
                    GZIPInputStream gzipis = new GZIPInputStream(is, Double.valueOf(Math.pow(2, 16)).intValue());
                    ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                this.referenceSequenceCache.put(accession, (ReferenceSequence) ois.readObject());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        ReferenceSequence referenceSequence = this.referenceSequenceCache.get(accession);

        if (zeroBased) {
            return referenceSequence.getSequence().substring(range.getMinimum() - 1, range.getMaximum());
        } else {
            return referenceSequence.getSequence().substring(range.getMinimum(), range.getMaximum() + 1);
        }
    }

}
