package AFMs;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * One of the three root properties
 * Definition of context setup
 * */

public class Context {

    private double UnitOfMeasureScale; //The unit of measure scale
    private boolean Zup; //Boolean set to true if Zup convention is used, or set to false if Yup is used
    /**
     * Path of the repository where the 3D models can be found.
     * RepoPath can be defined as absolute or as relative to the application path.
     * The file path of 3D models is defined relatively to RepoPath.*/
    private String RepoPath;

    public Double getUnitOfMeasureScale() {
        return UnitOfMeasureScale;
    }

    public void setUnitOfMeasureScale(Double unitOfMeasureScale) {
        this.UnitOfMeasureScale = unitOfMeasureScale;
    }

    public Boolean getZup() {
        return Zup;
    }

    public void setZup(Boolean zup) {
        this.Zup = zup;
    }

    public String getRepoPath() {
        return RepoPath;
    }

    public void setRepoPath(String repoPath) {
        this.RepoPath = repoPath;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("unitOfMeasureScale", UnitOfMeasureScale).append("zup", Zup).append("repoPath", RepoPath).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(UnitOfMeasureScale).append(RepoPath).append(Zup).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Context)) {
            return false;
        }
        Context rhs = ((Context) other);
        return new EqualsBuilder().append(UnitOfMeasureScale, rhs.UnitOfMeasureScale).append(RepoPath, rhs.RepoPath).append(Zup, rhs.Zup).isEquals();
    }

}
