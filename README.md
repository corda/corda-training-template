![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Corda Training

## Pre-requrisites:

* JDK 1.8 latest version
* IntelliJ latest version (2017.1)( as of writing)
* git

## Instructions:

1. Clone this repo
2. Open the repo in IntelliJ
3. Import gradle project and wait for IntelliJ to index
4. Navigate to the unit tests `src/test/kotlin/net/corda/training`
5. Start with the state tests (You'll need to edit `IOUState.kt`)
6. Move on to the contract tests (You'll need to edit `IOUContract.kt`)
7. Move onto the flow tests (Issue, transfer, then Settle) 

## Troubleshooting:

When running the flow tests, if you get a Quasar instrumention error then add:

    -ea -javaagent:lib/quasar.jar 
    
To the VM args property in the default run configuration for JUnit in IntelliJ.

To run the unit tests use the Green arrow in IntelliJ next to the test definition.

Use the instructions above each unit test to complete the code in the required file (e.g. `IOUState.kt`) to get the unit tests to pass.

Solutions are available [here](https://github.com/roger3cev/corda-training-solutions).
