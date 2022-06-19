package AFMs;

public enum AssetType {
    ARTIFACT("http://www.ontoeng.com/factory#Artifact"),
    PALLET("http://www.ontoeng.com/factory#Pallet"),
    RESTPOSITION("http://www.ontoeng.com/factory#BufferRestingPosition"),
    WORKPOSITION("http://www.ontoeng.com/factory#BufferWorkingPosition"),
    BUFFER("http://www.ontoeng.com/factory#BufferElement"),
    CONVEYOR("http://www.ontoeng.com/factory#Conveyor"),
    MACHINETOOL("http://www.ontoeng.com/factory#MachineTool");

    public final String type;

    AssetType(String type) {
        this.type = type;
    }
}
