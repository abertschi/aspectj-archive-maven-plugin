package ch.abertschi.aspectj;

public class Module
{
    private String groupId;

    private String artifactId;

    private String classifier;

    private String type = "jar";

    private String version;

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(groupId).append(":").append(artifactId);
        if (version != null)
        {
            builder.append(":").append(version);
        }
        if (type != null)
        {
            builder.append(":").append(type);
        }
        return builder.toString();
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId(String groupId)
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId(String artifactId)
    {
        this.artifactId = artifactId;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier(String classifier)
    {
        this.classifier = classifier;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }
}
