package org.renci.gerese4j.core;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Range;

public interface GeReSe4jBuild {

    public BuildType getBuild();

    public Set<String> getIndices() throws GeReSe4jException;

    public String getHeader(String accession) throws GeReSe4jException;

    public Map<String, ReferenceSequence> getReferenceSequenceCache();

    public ReferenceSequence getReferenceSequence(String accession) throws GeReSe4jException;

    public ReferenceSequence getReferenceSequence(String accession, boolean cache) throws GeReSe4jException;

    public String getBase(String accession, int idx, boolean zeroBased) throws GeReSe4jException;

    public String getBase(String accession, int idx, boolean zeroBased, boolean cache) throws GeReSe4jException;

    public String getRegion(String accession, Range<Integer> range, boolean zeroBased) throws GeReSe4jException;

    public String getRegion(String accession, Range<Integer> range, boolean zeroBased, boolean cache) throws GeReSe4jException;

    public void serialize() throws GeReSe4jException;

    public void init() throws GeReSe4jException;

}
