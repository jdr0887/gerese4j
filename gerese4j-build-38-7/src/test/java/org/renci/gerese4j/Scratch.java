package org.renci.gerese4j;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.Range;
import org.junit.Test;
import org.renci.gerese4j.build_38_7.GeReSe4jBuild_38_7_Manager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scratch {

    private static final Logger logger = LoggerFactory.getLogger(Scratch.class);

    @Test
    public void testPosition() throws Exception {
        logger.info("start initializing");
        GeReSe4jBuild_38_7_Manager gereseMgr = GeReSe4jBuild_38_7_Manager.getInstance();
        logger.info("finish initializing");

        // grabbed these hgvs expressions from clinvar
        assertTrue("C".equals(gereseMgr.getBase("NC_000004.12", 4860231, true)));
        logger.info("finished search for NC_000004.12:g.4860231C>A");

        assertTrue("G".equals(gereseMgr.getBase("NC_000008.11", 66178947, true)));
        logger.info("finished search for NC_000008.11:g.66178947G>T");

        assertTrue("G".equals(gereseMgr.getBase("NC_000019.10", 41353215, true)));
        logger.info("finished search for NC_000019.10:g.41353215delG");

        assertTrue("A".equals(gereseMgr.getBase("NC_000001.11", 11022530, true)));
        logger.info("finished search for NC_000001.11:g.11022530dupA");

        // this should be wicked fast since NC_000019.10 is now cached
        assertTrue("AGCAGCAGC".equals(gereseMgr.getRegion("NC_000019.10", Range.between(41353007, 41353015), true)));
        logger.info("finished search for NC_000019.10:g.41353007_41353015dupAGCAGCAGC");
    }

}
