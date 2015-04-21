# aspectj-archive-maven-plugin

>  Compile-time weave (CTW) your Java EARs and WARs

This is a simple wrapper around the `aspecj-maven-plugin` by `org.codehaus.mojo`
that is capable of compile-time-weaving (CTW) complete EARs or WARs.

## Usage
Add the artifact below to your plugin section.

```xml
<plugin>
    <groupId>ch.abertschi</groupId>
    <artifactId>aspectj-archive-maven-plugin</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</plugin>

```

## Goal archive-weave
The goal `archive-weave` imports an existing { EAR | WAR }
and applies compile-time-weaving on every specified `weaveDependency`
using the given `aspectLibraries`. The output is a new { EAR | WAR }
containing the specified weaveDependencies replaced by their new compile-time-weaved equivalents.

```xml
<plugin>
    <groupId>ch.abertschi</groupId>
    <artifactId>aspectj-archive-maven-plugin</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>archive-weave</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <archiveImport>../other-maven-module/my-production-artifact.ear</archiveImport>
        <aspectLibraries>
            <aspectLibrary>
                <groupId>com.my.company</groupId>
                <artifactId>my-apsects</artifactId>
            </aspectLibrary>
        </aspectLibraries>
        <weaveDependencies>
            <weaveDependency>
                <groupId>com.my.compay</groupId>
                <artifactId>lib-to-weave-with-aspects</artifactId>
            </weaveDependency>
        </weaveDependencies>
    </configuration>
</plugin>

```

### Configuration

Configurations that are not listed here are delegates 
For those configurations that are not listed here shall the plugin documentation of
the `aspecj-maven-plugin` by `org.codehaus.mojo` be considered.

#### archiveImport

Source artifact to import, type EAR or WAR.

#### archiveExport

Export artifact containing the weaved dependencies.

#### mavenConf

Location of the maven settings.xml

## License
This project is released under the MIT License (MIT). See LICENSE file.

## Contributing
Help is always welcome. Contribute to the project by forking and submitting a pull request.
