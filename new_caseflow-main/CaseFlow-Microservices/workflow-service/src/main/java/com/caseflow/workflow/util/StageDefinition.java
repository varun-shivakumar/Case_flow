package com.caseflow.workflow.util;

public class StageDefinition {
    private int seqNum; private String role; private int slaDays; private String stageName;
    public StageDefinition(int seqNum, String role, int slaDays, String stageName) {
        this.seqNum = seqNum; this.role = role; this.slaDays = slaDays; this.stageName = stageName;
    }
    public int getSeqNum() { return seqNum; }
    public String getRole() { return role; }
    public int getSlaDays() { return slaDays; }
    public String getStageName() { return stageName; }
}
