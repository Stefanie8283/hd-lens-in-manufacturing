package AssemblySt;

import org.jetbrains.annotations.NotNull;

/**
 * This represents an assembly status (pallet, status, buffer) on a specific time stamp
 */

public class AssemblySt implements Comparable<AssemblySt> {
    private final String artifact;
    private final String status;
    private final String scene;

    public AssemblySt(String artifact, String status, String buffer){
        this.artifact = artifact;
        this.status = status;
        this.scene = buffer;
    }

    public String getArtifact(){
        return artifact;
    }

    public AssemblySt setArtifact(String artifact){
        return new AssemblySt(artifact, status,scene);
    }

    public String getStatus(){
        return status;
    }

    public AssemblySt setStatus(String enterOR){
        return new AssemblySt(artifact, enterOR,scene);
    }

    public String getScene(){
        return scene;
    }

    public AssemblySt setScene(String scene){
        return new AssemblySt(artifact, status,scene);
    }

    @Override
    public String toString() {
        return artifact +
                " " + status +
                " " + scene;
    }

    @Override
    public int compareTo(@NotNull AssemblySt o) {
        return 0;
    }
}
