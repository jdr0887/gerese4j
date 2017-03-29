package org.renci.gerese4j;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.Range;
import org.junit.Test;
import org.renci.gerese4j.build_37_2.GeReSe4jManager;
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
        // NC_000004.11:g.4861958C&gt;A
        assertTrue("C".equals(gereseMgr.getBase("NC_000004.11", 4861958, false)));

        // NC_000008.10:g.67091182G&gt;T
        assertTrue("G".equals(gereseMgr.getBase("NC_000008.10", 67091182, false)));

        // NC_000019.9:g.41859120delG
        assertTrue("G".equals(gereseMgr.getBase("NC_000019.9", 41859120, false)));

        // NC_000019.9:g.41858912_41858920dupAGCAGCAGC
        assertTrue("AGCAGCAGC".equals(gereseMgr.getRegion("NC_000019.9", Range.between(41858912, 41858920), true)));

    }

}
