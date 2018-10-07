# errorcode4j
**error code generation library for java**

###### usage :
.1)  add error code generator plugin to your pom file
```
<plugin>
                <groupId>com.cloudimpl</groupId>
                <artifactId>errorcode-generator</artifactId>
                <version>1.0</version>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <id>xxx</id>
                        <goals>
                            <goal>ErrorCodeGenerator</goal>
                        </goals>
                    </execution>
                </executions>

            </plugin>
```
.2) then enable error code generation and define the erroFile name and packages using property tag

```
 <properties>
     <cloudImpl.errorCode.enable>true</cloudImpl.errorCode.enable>
     <cloudImpl.errorCode.package>com.cloudimpl.sample.error</cloudImpl.errorCode.package>
     <cloudImpl.errorCode.errorFileName>Sample</cloudImpl.errorCode.errorFileName>
 </properties>  
```
after define properties , build the maven project , it'll generate the ErrorCode file inside the defined package.
in above example it is  Sample.java inside the  com.cloudimpl.sample.error package.

remove '_' enum and define your own enums for the errors.

sample errorCodes.

```
  USER_NOT_FOUND(0, "user [username] not found"),
  LOGIN_FAILED(1, "user login failed for user [username] and reason [reason]");
```
