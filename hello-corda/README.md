![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Corda Training Template V2

This repo contains the starting code for a one day Corda training course. The instructional powerpoint that goes along with this content can be found here: https://github.com/corda/corda-training-materials/blob/master/Cordapp%20Development%20Module.pdf

This repository is currently only available in Java.

Solutions are available here: https://github.com/corda/corda-training-solutions


# Setup

### Tools 
* JDK 1.8 latest version
* IntelliJ latest version (2017.1 or newer)
* git

After installing the required tools, clone or download a zip of this repository, and place it in your desired 
location.

### IntelliJ setup
* From the main menu, click `open` (not `import`!) then navigate to where you placed this repository.
* Click `File->Project Structure`, and set the `Project SDK` to be the JDK you downloaded (by clicking `new` and 
nagivating to where the JDK was installed). Click `Okay`.
* Next, click `import` on the `Import Gradle Project` popup, leaving all options as they are. 
* If you do not see the popup: Navigate back to `Project Structure->Modules`, clicking the `+ -> Import` button,
navigate to and select the repository folder, select `Gradle` from the next menu, and finally click `Okay`, 
again leaving all options as they are.

# Instructions
Once you are all set up, you can start expanding on the class templates.
In order to send custom messages using this CorDapp - You will first fix `MessageState`, then `MessageFlow`, and lastly `MessageContract`.
There is a comment above each class that explains the change that needs to be done.
For more details follow the intructional powerpoint that can be found here: https://github.com/corda/corda-training-materials/blob/master/Cordapp%20Development%20Module.pdf. 

### Running the tests
* Select `Baseline Test - Java` from the dropdown run configuration menu, and click the green play button.
* Individual tests can be run by clicking the green arrow in the line number column next to each test.
* When running flow tests you must add the following to your run / debug configuration in the VM options field. This enables us to use
* Quasar - a library that provides high-performance, lightweight threads.
* "-javaagent: /PATH_TO_FILE_FROM_ROOT_DIR/quasar.jar"


# Template Files

### Java

State:
* Template: `java-source/src/main/java/net/corda/hello/MessageState.java`
* Tests: `java-source/src/test/java/net/corda/hello/state/MessageStateTests.java`

Contract:
* Template: `java-source/src/main/java/net/corda/hello/MessageContract.java`
* Tests: `java-source/src/test/java/net/corda/hello/contract/SendMessageTests.java`

Flow:
* SendMessage template: `java-source/src/main/java/net/corda/hello/MessageFlow.java`
* SendMessage tests: `java-source/src/test/java/net/corda/hello/flow/SendMessageFlowTests.java`

Integration Test:
* HelloTests Test: `java-source/src/test/java/net/corda/hello/HelloTests.java`


# Running the CorDapp
While the functionality will differ, this application can be run before and after making the changes described in the tutorial.

### Java
* Terminal: Navigate to the root project folder and run `./gradlew clean java-source:deployNodes`, followed by 
`./java-source/build/node/runnodes`

## Troubleshooting:
When running the flow tests, if you get a Quasar instrumention error then add:

```-ea -javaagent:lib/quasar.jar```

to the VM args property in the default run configuration for JUnit in IntelliJ.
