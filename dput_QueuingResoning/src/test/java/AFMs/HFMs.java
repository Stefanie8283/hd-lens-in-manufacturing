package AFMs;

import Utils.Pair;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static Utils.Predicate.filterList;
import static Utils.Predicate.filterSingle;

public class HFMs {
    private Context context;
    private List<String> scene = new ArrayList<String>();
    private List<Asset> assets = new ArrayList<Asset>();

    public Context getContext() {
        return context;
    }

    public List<String> getScene() {
        return scene;
    }

    public void setScene(List<String> scene) {
        this.scene = scene;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    /** Get a list of Asset by its type*/
    public List<Asset> getAssetsByType(List<Asset> asset, String type){
        return filterList(asset, (Asset ast) -> ast.getType().equals(type));
    }

    /** Get a list of Asset by its id */
    public Asset getAssetsByID(String id){
        return filterSingle(assets, (Asset ast) -> ast.getId().equals(id));
    }

    Comparator<Asset> compareById = Comparator.comparing(Asset::getId);

    /** Get the parentObject from the known Asset */

    public Asset getAssignmentToAsset(Asset as){
        Asset match = new Asset();
        if(!as.getAssignmentTo().isEmpty())
        {
            String id = as.getAssignmentTo().iterator().next();
            match = getAssetsByID(id);
        }
        return match;
    }

    public Asset getParentObjectAsset(Asset as){
        Asset match = new Asset();
        if(as.getParentObject() !=null)
        {
            String id = as.getParentObject();
            match = getAssetsByID(id);
        }
        return match;
    }

    public Asset getSuccessorsAsset(Asset as){
        Asset match = new Asset();
        if(as.getSuccessors() !=null)
        {
            String id = as.getSuccessors().iterator().next();
            match = getAssetsByID(id);
        }
        return match;
    }

    public Double getAssignmentToTaskTime(Asset asset){
        Double taskTime = 0.0;
        if(!asset.getAssignmentTo().isEmpty())
        {
            Asset as = getAssetsByID(asset.getAssignmentTo().get(1));
            taskTime = as.getTaskTime();
        }
        return taskTime;
    }

    public List<Asset> getSceneAssets() {
        List<Asset> sceneAssets = new ArrayList<>();
        scene.forEach(a -> sceneAssets.add(this.getAssetsByID(a)));
        return sceneAssets;
    }

    public Asset getConectedAsset(Asset as){
        Asset match = new Asset();
        if(!as.getConnectedTo().isEmpty())
        {
            String id = as.getConnectedTo().iterator().next();
            match = getAssetsByID(id);
        }
        return match;
    }

    public List<Asset> getAllSubLevels(Asset as){
        // get the level of as, if it is >0, find its sub-levels until it is 0
        List<Asset> output = new ArrayList<>();
        //int asLevel = as.getLevel();
        for(int i=as.getLevel()-1; i>=0;i--){
            //get its parentAsset
            String parent = as.getParentObject();
            Asset parAsset = getAssetsByID(parent);
            output.add(parAsset);
        }
        return output;
    }

    /** Find a list of sub-level assests which share the same parent object */
    public List<Asset> getSubLevelAssets(Asset as){
        List<Asset> output = new ArrayList<>();
        List<Asset> allAssets = getSceneAssets();
        for(Asset asset:allAssets){
            String parent = asset.getParentObject();
            if(as.getId().equals(parent))
                output.add(asset);
        }
        return output;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    /** Read Json files */
    public static HFMs readJson(String filePath){
        HFMs hfm = new HFMs();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(org.codehaus.jackson.map.DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try{
            hfm = mapper.readValue(new File(filePath), HFMs.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hfm;
    }

    /** Get buffer Cap and TaskTime */
    public static Pair<Integer, Double> getCapAndTime (List<String> bufList, HFMs hfm){
        int bufferCap = 0;
        double minTaskTime = 0.0;

        for (String str : bufList) {
            Asset asset = hfm.getAssetsByID(str);
            Double k = hfm.getAssignmentToTaskTime(asset);
            minTaskTime = minTaskTime + k;
            bufferCap = bufferCap + asset.getBufferCap();
        }
        return new Pair<>(bufferCap,minTaskTime);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("context", context).append("scene", scene).append("assets", assets).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(context).append(assets).append(scene).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof HFMs)) {
            return false;
        }
        HFMs rhs = ((HFMs) other);
        return new EqualsBuilder().append(context, rhs.context).append(assets, rhs.assets).append(scene, rhs.scene).isEquals();
    }


}
