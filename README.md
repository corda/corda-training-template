![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Corda Training Template

This repo contains all of the instructions and class templates for the practical exercises of the Corda two day 
training course.


This repository is divided into two parts: Java templates, and Kotlin templates. You may complete the training in whichever 
language you prefer. 

# To all external trainers:
This repo will have a major upgrade on March 26th. The primary changes will be 
1. Corda version bump
2. Cordapp structural changes: separate flows and contracts/states
3. Spring server rewrite. 

The upgraded code is done and you can see the preview at branch[ 4.4-update ](https://github.com/corda/corda-training-template/tree/4.4-update)
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
Once you are all set up, you can start expanding on the class templates. This project follows a test-based
development style - the unit tests for each class contain all the information you will need to complete this CorDapp.

All the tests are commented out by default - to complete this training, you will uncomment them one at a time, building up 
the CorDapp until it passes everything.

You will begin by opening `IOUState` and `IOUStateTests` in your language of choice and uncommenting the first test. Then, use the TODO and 
hints to modify `IOUState` to pass the test. 

In order to issue IOU's using Corda - You will first fix `IOUState`, then `IOUContract`, and lastly `IOUIssueFlow`. Then you can move on to
more challenging exercises specified in Transfer and Settle testing files.

### Running the tests
* Kotlin: Select `Kotlin - Unit tests` from the dropdown run configuration menu, and click the green play button.
* Java: Select `Java - Unit tests` from the dropdown run configuration menu, and click the green play button.
* Individual tests can be run by clicking the green arrow in the line number column next to each test.
* When running flow tests you must add the following to your run / debug configuration in the VM options field. This enables us to use
* Quasar - a library that provides high-performance, lightweight threads.
* "-javaagent: /PATH_TO_FILE_FROM_ROOT_DIR/quasar.jar"

# Template Files

### Kotlin
State:

* Template: `kotlin-source/src/test/kotlin/net/corda/training/state/IOUState.kt`
* Tests: `kotlin-source/src/main/kotlin/net/corda/training/state/IOUStateTests.kt`

Contract:

* Template: `kotlin-source/src/main/kotlin/net/corda/training/contract/IOUContract.kt`
* Issue Tests: `kotlin-source/src/test/kotlin/net/corda/training/contract/IOUIssueTests.kt`
* Transfer Tests: `kotlin-source/src/test/kotlin/net/corda/training/contract/IOUTransferTests.kt`
* Settle Tests: `kotlin-source/src/test/kotlin/net/corda/training/contract/IOUSettleTests.kt`

Flow:

* Issue flow template: `kotlin-source/src/main/kotlin/net/corda/training/flow/IOUIssueFlow.kt`
* Issue flow tests: `kotlin-source/src/test/kotlin/net/corda/training/flow/IOUIssueFlowTests.kt`
* Transfer flow template `kotlin-source/src/main/kotlin/net/corda/training/flow/IOUTransferFlow.kt`
* Transfer flow tests: `kotlin-source/src/test/kotlin/net/corda/training/flow/IOUTransferFlowTests.kt`
* Settle flow template `kotlin-source/src/main/kotlin/net/corda/training/flow/IOUSettleFlow.kt`
* Settle flow tests: `kotlin-source/src/test/kotlin/net/corda/training/flow/IOUSettleFlowTests.kt`

The code in the following files was already added for you:

* `kotlin-source/src/main/kotlin/net/corda/training/plugin/IOUPlugin.kt`
* `kotlin-source/src/test/kotlin/net/corda/training/Main.kt`
* `kotling-source/src/main/kotlin/net/corda/training/plugin/IOUPlugin.kt`
* `kotling-source/src/main/java/kotlin/corda/training/flow/SelfIssueCashFlow.kt`


### Java
State:

* Template: `java-source/src/main/java/net/corda/training/state/IOUState.java`
* Tests: `java-source/src/test/java/net/corda/training/state/IOUStateTests.java`

Contract:

* Template: `java-source/src/main/java/net/corda/training/contract/IOUContract.java`
* Issue Tests: `java-source/src/test/java/net/corda/training/contract/IOUIssueTests.java`
* Transfer Tests: `java-source/src/test/java/net/corda/training/contract/IOUIssueTests.java`
* Settle Tests: `java-source/src/test/java/net/corda/training/contract/IOUIssueTests.java`

Flow:

* Issue template: `java-source/src/main/java/net/corda/training/flow/IOUIssueFlow.java`
* Issue tests: `java-source/src/test/java/net/corda/training/flow/IOUIssueFlowTests.java`
* Transfer template: `java-source/src/main/java/net/corda/training/flow/IOUTransferFlow.java`
* Transfer tests: `java-source/src/test/java/net/corda/training/flow/IOUTransferFlowTests.java`
* Settle template: `java-source/src/main/java/net/corda/training/flow/IOUSettleFlow.java`
* Settle tests: `java-source/src/test/java/net/corda/training/flow/IOUSettleFlowTests.java`

The code in the following files was already added for you:

* `java-source/src/main/java/net/corda/training/plugin/IOUPlugin.java`
* `java-source/src/test/java/net/corda/training/NodeDriver.java`
* `java-source/src/main/java/net/corda/training/plugin/IOUPlugin.java`
* `java-source/src/main/java/net/corda/training/flow/SelfIssueCashFlow.java`


# Running the CorDapp
Once your application passes all tests in `IOUStateTests`, `IOUIssueTests`, and `IOUIssueFlowTests`, you can run the application and 
interact with it via a web browser. To run the finished application, you have two choices for each language: from the terminal, and from IntelliJ.

### Kotlin
* Terminal: Navigate to the root project folder and run `./gradlew kotlin-source:deployNodes`, followed by 
`./kotlin-source/build/node/runnodes`
* IntelliJ: With the project open, select `Kotlin - Node driver` from the dropdown run configuration menu, and click 
the green play button.

### Java
* Terminal: Navigate to the root project folder and run `./gradlew java-source:deployNodes`, followed by 
`./java-source/build/node/runnodes`
* IntelliJ: With the project open, select `Java - NodeDriver` from the dropdown run configuration menu, and click 
the green play button.

### Interacting with the CorDapp
Once all the three nodes have started up (look for `Webserver started up in XXX sec` in the terminal or IntelliJ ), you can interact
with the app via a web browser. 
* From a Node Driver configuration, look for `Starting webserver on address localhost:100XX` for the addresses. 

* From the terminal: Node A: `localhost:10009`, Node B: `localhost:10012`, Node C: `localhost:10015`.

To access the front-end gui for each node, navigate to `localhost:XXXX/web/iou/`

## Troubleshooting:
When running the flow tests, if you get a Quasar instrumention error then add:

```-ea -javaagent:lib/quasar.jar```

to the VM args property in the default run configuration for JUnit in IntelliJ.

Solutions are available [here](https://github.com/corda/corda-training-solutions).
