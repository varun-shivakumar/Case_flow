package com.caseflow.workflow.util;
import java.util.List;

public class StageTemplate {
    public static List<StageDefinition> getCivilStage() {
        return List.of(
                new StageDefinition(1,"LITIGANT",7,"Case Filing"),
                new StageDefinition(2,"CLERK",5,"Document Verification"),
                new StageDefinition(3,"CLERK",5,"Hearing Scheduling"),
                new StageDefinition(4,"JUDGE",14,"Judgment"));
    }
    public static List<StageDefinition> getCriminalStage() {
        return List.of(
                new StageDefinition(1,"LITIGANT",1,"Case Filing"),
                new StageDefinition(2,"CLERK",5,"Document Verification"),
                new StageDefinition(3,"JUDGE",15,"Trial"),
                new StageDefinition(4,"CLERK",5,"Hearing Scheduling"),
                new StageDefinition(5,"JUDGE",3,"Verdict"));
    }
    public static List<StageDefinition> getCorporateStage() {
        return List.of(
                new StageDefinition(1,"LITIGANT",2,"Case Filing"),
                new StageDefinition(2,"CLERK",5,"Document Verification"),
                new StageDefinition(3,"ADMIN",6,"Compliance Review"),
                new StageDefinition(4,"CLERK",5,"Hearing Scheduling"),
                new StageDefinition(5,"LAWYER",10,"Arbitration"),
                new StageDefinition(6,"JUDGE",7,"Decision"));
    }
}
