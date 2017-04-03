package org.renci.gerese4j;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.Range;
import org.junit.Test;
import org.renci.gerese4j.build_37_2.GeReSe4jManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scratch {

    private static final Logger logger = LoggerFactory.getLogger(Scratch.class);

    @Test
    public void testPosition() throws Exception {
        logger.info("start initializing");
        GeReSe4jManager gereseMgr = GeReSe4jManager.getInstance();
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

    }

}
