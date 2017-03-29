package org.renci.gerese4j.build_36_1;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Range;
import org.renci.gerese4j.core.BuildType;
import org.renci.gerese4j.core.GeReSe4jException;
import org.renci.gerese4j.core.ReferenceSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeReSe4jManager {

    private static final Logger logger = LoggerFactory.getLogger(GeReSe4jManager.class);

    private Map<String, ReferenceSequence> referenceSequenceMap = null;

    private static GeReSe4jManager instance;

    public static GeReSe4jManager getInstance() {
        if (instance == null) {
            instance = new GeReSe4jManager();
            instance.init();
        }
        return instance;
    }

    private GeReSe4jManager() {
        super();
    }

    public BuildType getBuild() {
        return BuildType.BUILD_36_1;
    }

    private void init() {
        long start = System.currentTimeMillis();
        try (InputStream is = getClass().getResourceAsStream(String.format("reference_sequences_%s.ser", getBuild().getVersion()));
                GZIPInputStream gzipis = new GZIPInputStream(is, Double.valueOf(Math.pow(2, 16)).intValue());
                ObjectInputStream ois = new ObjectInputStream(gzipis)) {
            referenceSequenceMap = (Map<String, ReferenceSequence>) ois.readObject();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        long end = System.currentTimeMillis();
        logger.info("duration: {} seconds", (end - start) / 1000);
    }

    public Map<String, ReferenceSequence> getReferenceSequenceMap() {
        return referenceSequenceMap;
    }

    public void setReferenceSequenceMap(Map<String, ReferenceSequence> referenceSequenceMap) {
        this.referenceSequenceMap = referenceSequenceMap;
    }

    public String getBase(String accession, int idx, boolean zeroBased) throws GeReSe4jException {

        if (MapUtils.isEmpty(referenceSequenceMap)) {
            throw new GeReSe4jException("ReferenceSequence is empty");
        }

        if (!this.referenceSequenceMap.containsKey(accession)) {
            throw new GeReSe4jException(String.format("ReferenceSequence not found using: %s", accession));
        }

        ReferenceSequence referenceSequence = this.referenceSequenceMap.get(accession);

        if (zeroBased) {
            return referenceSequence.getSequence().substring(idx, idx + 1);
        } else {
            return referenceSequence.getSequence().substring(idx + 1, idx + 2);
        }
    }

    public String getRegion(String accession, Range<Integer> range, boolean zeroBased) throws GeReSe4jException {

        if (MapUtils.isEmpty(referenceSequenceMap)) {
            throw new GeReSe4jException("ReferenceSequence is empty");
        }

        if (!this.referenceSequenceMap.containsKey(accession)) {
            throw new GeReSe4jException(String.format("ReferenceSequence not found using: %s", accession));
        }

        ReferenceSequence referenceSequence = this.referenceSequenceMap.get(accession);

        if (zeroBased) {
            return referenceSequence.getSequence().substring(range.getMinimum() - 1, range.getMaximum());
        } else {
            return referenceSequence.getSequence().substring(range.getMinimum(), range.getMaximum() + 1);
        }
    }

}
