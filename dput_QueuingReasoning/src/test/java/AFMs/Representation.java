package AFMs;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Array of 2D/3D representations of the asset.
 * */

public class Representation {

    private String file; // File path of the 2D/3D representation, as relative to the RepoPath [required]
    private double unit; //Unit of measure to interpret a 3D representation

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Double getUnit() {
        return unit;
    }

    public void setUnit(Double unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("file", file).append("unit", unit).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(unit).append(file).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Representation)) {
            return false;
        }
        Representation rhs = ((Representation) other);
        return new EqualsBuilder().append(unit, rhs.unit).append(file, rhs.file).isEquals();
    }
}
