package AFMs;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Asset {
    private String id;
    private String type; // The type of the asset (i.e. class it belongs to) [required]
    private String descr; //textual static description of the asset [optional]
    private String model;  //The model of the asset (if existing). In turn, the model can have a model.
    private String placementRelTo;
    private List<Double> position = new ArrayList<>();
    private String parentObject;
    private List<String> connectedTo = new ArrayList<>();
    private List<Integer> rotation = null;
    private List<String> assignmentTo = null;
    private Integer level;
    private Integer duration;
    private Integer quantity;
    private Integer bufferCap;
    private List<String> successors = new ArrayList<>();
    private Double taskTime;

    public List<Integer> getRotation() {
        return rotation;
    }
    public void setRotation(List<Integer> rotation) {
        this.rotation = rotation;
    }

    public List<String> getAssignmentTo() {
        return assignmentTo;
    }
    public void setAssignmentTo(List<String> assignmentTo) {
        this.assignmentTo = assignmentTo;
    }

    public Integer getDuration() {
        return duration;
    }
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getLevel() {
        return level;
    }
    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getQuantity() {
        return quantity;
    }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getBufferCap() {
        return bufferCap;
    }
    public void setBufferCap(Integer bufferCap) {
        this.bufferCap = bufferCap;
    }

    public boolean equals(AFMs.Asset asset) {
        return this.getId().equals(asset.getId()); }

    public String getId() {
        return id;
    }
    public void setId(String id){this.id = id;}

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getDescr() {
        return descr;
    }
    public void setDescr(String descr) {
        this.descr = descr;
    }

    public String getModel() {
        return model;
    }
    public void setModel(String model) {
        this.model = model;
    }

    public String getPlacementRelTo() {
        return placementRelTo;
    }
    public void setPlacementRelTo(String placementRelTo) {
        this.placementRelTo = placementRelTo;
    }

    public List<Double> getPosition() {
        return position;
    }
    public void setPosition(List<Double> position) {
        this.position = position;
    }

    public String getParentObject() {
        return parentObject;
    }
    public void setParentObject(String parentObject) {
        this.parentObject = parentObject;
    }

    public List<String> getConnectedTo() {
        return connectedTo;
    }
    public void setConnectedTo(List<String> connectedTo) {
        this.connectedTo = connectedTo;
    }

    public List<String> getSuccessors() {
        return successors;
    }
    public void setSuccessors(List<String> successors) {
        this.successors = successors;
    }

    public Double getTaskTime() {
        return taskTime;
    }
    public void setTaskTime(Double taskTime) {
        this.taskTime = taskTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("type", type)
                .append("descr", descr)
                .append("model", model)
                .append("successors", successors)
                .append("taskTime", taskTime)
                .append("placementRelTo", placementRelTo)
                .append("position", position)
                .append("parentObject", parentObject)
                .append("connectedTo", connectedTo)
                .append("rotation", rotation)
                .append("assignmentTo", assignmentTo)
                .append("duration", duration)
                .append("quantity", quantity)
                .append("bufferCap", bufferCap)
                .append("duration", duration)
                .toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(descr).append(parentObject).append(placementRelTo).append(model).append(id).append(position).append(rotation).append(assignmentTo).append(duration).append(quantity).append(bufferCap).append(type).append(connectedTo).toHashCode();
    }

    public static Comparator<Asset> compareByType() {
        return Comparator.comparing(Asset::getType);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Asset)) {
            return false;
        }
        Asset rhs = ((Asset) other);
        return new EqualsBuilder().append(quantity, rhs.quantity).append(parentObject, rhs.parentObject).append(rotation, rhs.rotation).append(type, rhs.type).append(assignmentTo, rhs.assignmentTo).append(connectedTo, rhs.connectedTo).append(successors, rhs.successors).append(taskTime, rhs.taskTime).append(duration, rhs.duration).append(descr, rhs.descr).append(bufferCap, rhs.bufferCap).append(placementRelTo, rhs.placementRelTo).append(model, rhs.model).append(id, rhs.id).append(position, rhs.position).isEquals();
    }

}
