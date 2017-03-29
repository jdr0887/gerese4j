package org.renci.gerese4j;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.Range;
import org.junit.Test;
import org.renci.gerese4j.build_38_7.GeReSe4jManager;
import org.renci.gerese4j.core.ReferenceSequence;

public class Scratch {

    @Test
    public void scratch() {
        Map<String, ReferenceSequence> referenceSequenceMap = null;
        long start = System.currentTimeMillis();
        try (InputStream is = GeReSe4jManager.class.getClassLoader().getResourceAsStream("org/renci/gerese4j/reference_sequences_38.7.ser");
                GZIPInputStream gzipis = new GZIPInputStream(is, Double.valueOf(Math.pow(2, 16)).intValue());
                ObjectInputStream ois = new ObjectInputStream(gzipis)) {
            referenceSequenceMap = (Map<String, ReferenceSequence>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        System.out.println(String.format("duration: %d seconds", (end - start) / 1000));
        System.out.println(referenceSequenceMap.keySet().size());

    }

    @Test
    public void testPosition() throws Exception {
        GeReSe4jManager gereseMgr = GeReSe4jManager.getInstance();

        // grabbed these hgvs expressions from clinvar
        // NC_000004.12:g.4860231C>A
        assertTrue("C".equals(gereseMgr.getBase("NC_000004.12", 4860231, false)));

        // NC_000008.11:g.66178947G&gt;T
        assertTrue("G".equals(gereseMgr.getBase("NC_000008.11", 66178947, false)));

        // NC_000019.10:g.41353215delG
        assertTrue("G".equals(gereseMgr.getBase("NC_000019.10", 41353215, false)));

        // NC_000019.10:g.41353007_41353015dupAGCAGCAGC
        assertTrue("AGCAGCAGC".equals(gereseMgr.getRegion("NC_000019.10", Range.between(41353007, 41353015), true)));

    }

}
