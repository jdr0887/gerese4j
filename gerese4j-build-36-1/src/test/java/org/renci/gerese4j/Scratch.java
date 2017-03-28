package org.renci.gerese4j;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.Test;
import org.renci.gerese4j.build_36_1.GeReSe4jManager;
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

}
