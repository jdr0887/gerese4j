package org.renci.gerese4j.core;

public enum BuildType {

    BUILD_36_1("36.1"),

    BUILD_37_1("37.1"),

    BUILD_37_2("37.2"),

    BUILD_38_2("38.2"),

    BUILD_38_7("38.7");

    private String version;

    private BuildType(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
