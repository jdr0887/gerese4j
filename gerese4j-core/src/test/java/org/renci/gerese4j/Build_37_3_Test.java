package org.renci.gerese4j;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.lang3.Range;
import org.junit.Test;
import org.renci.gerese4j.core.GeReSe4jBuild;
import org.renci.gerese4j.core.GeReSe4jException;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_37_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Build_37_3_Test {

    private static final Logger logger = LoggerFactory.getLogger(Build_37_3_Test.class);

    @Test
    public void testPosition() throws Exception {
        logger.info("start initializing");
        GeReSe4jBuild gereseMgr = GeReSe4jBuild_37_3.getInstance(new File("/home/jdr0887/gerese4j"));
        logger.info("finish initializing");

        // grabbed these hgvs expressions from clinvar
        assertTrue("C".equals(gereseMgr.getBase("NC_000004.11", 4861958, true)));
        logger.info("finished search for NC_000004.11:g.4861958C>A");

        assertTrue("G".equals(gereseMgr.getBase("NC_000008.10", 67091182, true)));
        logger.info("finished search for NC_000008.10:g.67091182G>T");

        assertTrue("G".equals(gereseMgr.getBase("NC_000019.9", 41859120, true)));
        logger.info("finished search for NC_000019.9:g.41859120delG");

        assertTrue("G".equals(gereseMgr.getBase("NC_000014.8", 23871743, true)));
        logger.info("finished search for NC_000019.9:g.41859120delG");

        assertTrue("AGCAGCAGC".equals(gereseMgr.getRegion("NC_000019.9", Range.between(41858912, 41858920), true)));
        logger.info("finished search for NC_000019.9:g.41858912_41858920dupAGCAGCAGC");

        assertTrue("CC".equals(gereseMgr.getRegion("NC_000003.11", Range.between(184075220, 184075221), true)));
        logger.info("finished search for NC_000003.11:g.184075220dupC");

    }

    @Test
    public void testSerialize() {

        try {
            GeReSe4jBuild gerese4jMgr = GeReSe4jBuild_37_3.getInstance(new File(
                    "/home/jdr0887/workspace/renci/canvas/primer/primer/primer-server/target/primer-server-0.0.3-SNAPSHOT/data/GenomeReference"));
            gerese4jMgr.serialize();
        } catch (GeReSe4jException e) {
            e.printStackTrace();
        }
    }

}
