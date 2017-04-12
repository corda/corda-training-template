![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Corda Training Solutions

This is a temporary repo for Roger to build out the latest iteration of the Corda training.

Work done:

* Rebased to M10
* Added SignTransactionFlow
* Added extensive helper comments in all unit test files
* Added much improved unit tests

## Todo

General:
* Finish `SignTransactionFlow`: Add progress tracking steps, allow it to take a partially signed Tx
* Update the web app to add UI components for dealing with transferring and settling IOUs
* Port the CollectSignatureFlow into the main Corda repo (requires changes!) - figure out how to refactor some of the
  existing flows

Training specific:
* Create the template branch to be used for training. Break out the solutions into a separate repo and stub out the
  template with comments to help the delegates complete the tasks.
* Add a proper set of instructions in this README.md for the exercises.
* Update the training slides.

Extra:
* Add support for Bilateral netting of IOUs
* Add support for defaulted IOUs - will require adding an ENUM to the IOUState - will need to change lots of the tests!

